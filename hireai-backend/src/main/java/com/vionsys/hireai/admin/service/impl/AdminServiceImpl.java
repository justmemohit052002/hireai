package com.vionsys.hireai.admin.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.admin.dto.AdminAuditLogResponse;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.ModuleStatDto;
import com.vionsys.hireai.admin.dto.ProviderDetailsDto;
import com.vionsys.hireai.admin.dto.ProviderStatDto;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;
import com.vionsys.hireai.admin.dto.QuotaUsageDto;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.entity.AdminAuditLog;
import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.admin.service.AdminHealthPollerService;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.AdminConflictException;
import com.vionsys.hireai.exception.AiCallLogNotFoundException;
import com.vionsys.hireai.exception.RoleNotFoundException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminHealthPollerService healthPollerService;
    private final AiEngineClient aiEngineClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.vionsys.hireai.email.EmailService emailService;
    private final com.vionsys.hireai.email.EmailProperties emailProperties;

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponse> getUsers(String search, RoleType role, Boolean enabled, Pageable pageable) {
        String querySearch = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
        
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Pageable clampedPageable = PageRequest.of(pageNumber, pageSize, pageable.getSort());

        Page<User> usersPage = userRepository.searchUsers(querySearch, role, enabled, clampedPageable);
        return usersPage.map(this::toAdminUserResponse);
    }

    @Transactional
    @Override
    public AdminUserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "system";
        UUID actorUserId = resolveActorUserId(actorEmail);

        // 1. Prevent admin from demoting themselves
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail()) && request.getRole() != RoleType.ROLE_ADMIN) {
            throw new AdminConflictException("Admins cannot revoke their own administrator privileges.");
        }

        // 2. Protect last active, unlocked admin from demotion
        if (user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN
                && Boolean.TRUE.equals(user.getEnabled())
                && Boolean.TRUE.equals(user.getAccountNonLocked())
                && request.getRole() != RoleType.ROLE_ADMIN) {
            if (userRepository.countActiveUnlockedAdmins() <= 1) {
                throw new AdminConflictException("Cannot modify role of the last active administrator account.");
            }
        }

        Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRole()));

        String oldRoleName = user.getRole() != null ? user.getRole().getName().name() : "NONE";
        String newRoleName = newRole.getName().name();

        user.setRole(newRole);
        User saved = userRepository.save(user);

        // Immediately revoke all active refresh tokens for the user
        refreshTokenRepository.revokeAllUserTokens(saved);

        // Record audit log inside transaction
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action("UPDATE_ROLE")
                .targetUserId(user.getId())
                .oldValue("role=" + oldRoleName)
                .newValue("role=" + newRoleName)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Admin [{}] updated user {} ({}) role from {} to {}", actorEmail, saved.getId(), saved.getEmail(), oldRoleName, newRoleName);
        return toAdminUserResponse(saved);
    }

    @Transactional
    @Override
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "system";
        UUID actorUserId = resolveActorUserId(actorEmail);

        // 1. Prevent admin from deactivating or locking themselves
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail())) {
            if (Boolean.FALSE.equals(request.getEnabled())) {
                throw new AdminConflictException("Admins cannot deactivate their own user account.");
            }
            if (Boolean.FALSE.equals(request.getAccountNonLocked())) {
                throw new AdminConflictException("Admins cannot lock their own user account.");
            }
        }

        // 2. Protect last active, unlocked admin from deactivation or locking
        if (user.getRole() != null && user.getRole().getName() == RoleType.ROLE_ADMIN
                && Boolean.TRUE.equals(user.getEnabled())
                && Boolean.TRUE.equals(user.getAccountNonLocked())) {
            boolean deactivating = Boolean.FALSE.equals(request.getEnabled());
            boolean locking = Boolean.FALSE.equals(request.getAccountNonLocked());
            if (deactivating || locking) {
                if (userRepository.countActiveUnlockedAdmins() <= 1) {
                    throw new AdminConflictException("Cannot deactivate or lock the last active administrator account.");
                }
            }
        }

        String oldStatus = String.format("enabled=%s, accountNonLocked=%s", user.getEnabled(), user.getAccountNonLocked());

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getAccountNonLocked() != null) {
            user.setAccountNonLocked(request.getAccountNonLocked());
            if (Boolean.TRUE.equals(request.getAccountNonLocked())) {
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
            }
        }

        User saved = userRepository.save(user);
        String newStatus = String.format("enabled=%s, accountNonLocked=%s", saved.getEnabled(), saved.getAccountNonLocked());

        // Immediately revoke active refresh tokens if user was deactivated or locked
        if (Boolean.FALSE.equals(saved.getEnabled()) || Boolean.FALSE.equals(saved.getAccountNonLocked())) {
            refreshTokenRepository.revokeAllUserTokens(saved);
        }

        // Record audit log inside transaction
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action("UPDATE_STATUS")
                .targetUserId(user.getId())
                .oldValue(oldStatus)
                .newValue(newStatus)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Admin [{}] updated user {} ({}) status: {}", actorEmail, saved.getId(), saved.getEmail(), newStatus);
        return toAdminUserResponse(saved);
    }

    @Override
    public AdminHealthResponse getHealthSummary() {
        return healthPollerService.getCachedHealthSnapshot();
    }

    @Override
    public AdminStatsResponse getLlmStats(String range) {
        return aiEngineClient.getAdminStats(range)
                .map(stats -> {
                    stats.setAvailable(true);
                    return stats;
                })
                .orElseGet(() -> AdminStatsResponse.builder()
                        .available(false)
                        .message("AI engine unreachable")
                        .totalCalls(0L)
                        .todayCalls(0L)
                        .weekCalls(0L)
                        .failedCalls(0L)
                        .successRate(0.0)
                        .avgLatencySeconds(0.0)
                        .unknownTimeCount(0L)
                        .quota(null)
                        .providerDistribution(List.of())
                        .moduleBreakdown(List.of())
                        .build());
    }

    @Transactional
    @Override
    public AdminCallLogsResponse getCallLogs(
            String query, String taskType, String status, String provider,
            String from, String to, String correlationId, String candidateRef,
            int page, int size) {

        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int clampedPage = Math.max(page, 1);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "system";
        UUID actorUserId = resolveActorUserId(actorEmail);

        String filters = String.format("query=%s, taskType=%s, status=%s, provider=%s, from=%s, to=%s, correlationId=%s, candidateRef=%s, page=%d, size=%d",
                query, taskType, status, provider, from, to, correlationId, candidateRef, clampedPage, clampedSize);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action("VIEW_CALL_LOGS")
                .targetUserId(null)
                .newValue(filters)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return aiEngineClient.getAdminLogs(
                query, taskType, status, provider, from, to, correlationId, candidateRef, clampedPage, clampedSize)
                .map(logs -> {
                    logs.setAvailable(true);
                    return logs;
                })
                .orElseGet(() -> AdminCallLogsResponse.builder()
                        .available(false)
                        .message("AI engine unreachable")
                        .items(List.of())
                        .totalElements(0L)
                        .page(clampedPage)
                        .size(clampedSize)
                        .totalPages(0)
                        .build());
    }

    @Transactional
    @Override
    public AdminCallLogEntry getCallLogById(String logId) {
        AdminCallLogEntry entry = aiEngineClient.getAdminLogById(logId)
                .orElseThrow(() -> new AiCallLogNotFoundException("AI call log not found with id: " + logId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "system";
        UUID actorUserId = resolveActorUserId(actorEmail);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action("VIEW_CALL_LOG")
                .targetUserId(null)
                .newValue("logId=" + logId)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return entry;
    }

    @Override
    public ProvidersConfigDto getProvidersStatus() {
        AdminHealthResponse health = getHealthSummary();
        if (health != null && health.getProviders() != null) {
            return health.getProviders();
        }

        return ProvidersConfigDto.builder()
                .activeProvider("ollama")
                .activeModel("llama3.2:3b")
                .ollama(ProviderDetailsDto.builder().name("Ollama (Local)").isConfigured(true).reachable(false).build())
                .gemini(ProviderDetailsDto.builder().name("Google Gemini").isConfigured(false).keyMasked("Not Configured").build())
                .groq(ProviderDetailsDto.builder().name("Groq API").isConfigured(false).keyMasked("Not Configured").build())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AdminAuditLogResponse> getAuditLogs(Pageable pageable) {
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Pageable clampedPageable = PageRequest.of(pageNumber, pageSize);

        Page<AdminAuditLog> auditPage = auditLogRepository.findAllByOrderByCreatedAtDesc(clampedPageable);
        return auditPage.map(this::toAdminAuditLogResponse);
    }

    private UUID resolveActorUserId(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }

    private AdminAuditLogResponse toAdminAuditLogResponse(AdminAuditLog log) {
        if (log == null) return null;
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .action(log.getAction())
                .targetUserId(log.getTargetUserId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        if (user == null) return null;
        return AdminUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null ? user.getRole().getName().name() : "ROLE_CANDIDATE")
                .enabled(user.getEnabled())
                .accountNonLocked(user.getAccountNonLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public java.util.Map<String, Object> testEmailDelivery(String recipientEmail) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("recipient", com.vionsys.hireai.email.EmailServiceImpl.maskEmail(recipientEmail));
        boolean configured = emailService.isSmtpConfigured();
        result.put("smtpConfigured", configured);
        result.put("senderFrom", emailProperties.getFrom());
        try {
            emailService.sendHtmlEmail(
                    recipientEmail,
                    "HireAI Outbound SMTP Connectivity Diagnostic",
                    """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: sans-serif; padding: 20px;">
                      <h2>HireAI SMTP Connectivity Diagnostic</h2>
                      <p>This automated message confirms that outbound SMTP delivery is fully functional.</p>
                      <p style="color: #64748b; font-size: 12px;">HireAI Platform Administration</p>
                    </body>
                    </html>
                    """
            );
            result.put("status", "SUCCESS");
            result.put("message", configured
                    ? "Diagnostic email successfully dispatched via SMTP."
                    : "Simulated email dispatch executed (SMTP unconfigured or hireai.mail.enabled=false).");
        } catch (Exception ex) {
            result.put("status", "FAILED");
            result.put("error", ex.getMessage());
        }
        return result;
    }
}
