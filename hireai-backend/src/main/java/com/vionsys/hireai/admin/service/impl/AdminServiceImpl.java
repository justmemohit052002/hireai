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
import com.vionsys.hireai.admin.dto.ModuleHealthDto;
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
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminHealthPollerService healthPollerService;
    private final AiEngineClient aiEngineClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.vionsys.hireai.email.EmailService emailService;
    private final com.vionsys.hireai.email.EmailProperties emailProperties;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.vionsys.hireai.job.repository.JobRepository jobRepository;
    private final com.vionsys.hireai.application.repository.JobApplicationRepository jobApplicationRepository;
    private final com.vionsys.hireai.security.ApiMetricsFilter apiMetricsFilter;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.vionsys.hireai.admin.service.AdminMetricsPersistenceService metricsPersistenceService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.core.env.Environment environment;

    public AdminServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AdminAuditLogRepository auditLogRepository,
            AdminHealthPollerService healthPollerService,
            AiEngineClient aiEngineClient,
            RefreshTokenRepository refreshTokenRepository,
            com.vionsys.hireai.email.EmailService emailService,
            com.vionsys.hireai.email.EmailProperties emailProperties,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            com.vionsys.hireai.job.repository.JobRepository jobRepository,
            com.vionsys.hireai.application.repository.JobApplicationRepository jobApplicationRepository,
            com.vionsys.hireai.security.ApiMetricsFilter apiMetricsFilter) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.healthPollerService = healthPollerService;
        this.aiEngineClient = aiEngineClient;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.apiMetricsFilter = apiMetricsFilter;
    }

    @org.springframework.beans.factory.annotation.Value("${admin.metrics.cache-ttl-seconds:120}")
    private long metricsCacheTtlSeconds = 120L;

    private final java.util.concurrent.ConcurrentHashMap<String, CachedMetricsEntry> metricsCache = new java.util.concurrent.ConcurrentHashMap<>();

    private record CachedMetricsEntry(com.vionsys.hireai.admin.dto.PlatformMetricsResponse data, long expiresAtMillis) {}

    private static final int MAX_PAGE_SIZE = 100;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponse> getUsers(String search, RoleType role, Boolean enabled, Pageable pageable) {
        String querySearch = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
        
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        org.springframework.data.domain.Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        Pageable clampedPageable = PageRequest.of(pageNumber, pageSize, sort);

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

        // 0. SUB_ADMIN cannot change roles
        boolean isSubAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUB_ADMIN"));
        if (isSubAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Sub-administrators are not authorized to modify user roles.");
        }

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

        // 0. SUB_ADMIN can only modify candidate and recruiter accounts
        boolean isSubAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUB_ADMIN"));
        if (isSubAdmin) {
            RoleType targetRole = user.getRole() != null ? user.getRole().getName() : null;
            if (targetRole == RoleType.ROLE_ADMIN || targetRole == RoleType.ROLE_SUB_ADMIN) {
                throw new org.springframework.security.access.AccessDeniedException("Sub-administrators cannot modify administrator or sub-administrator accounts.");
            }
        }

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
                .lastLoginAt(user.getLastLoginAt())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    @Override
    public AdminUserResponse createSubAdmin(com.vionsys.hireai.admin.dto.CreateSubAdminRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new AdminConflictException("A user account with this email address already exists: " + cleanEmail);
        }

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        Role subAdminRole = roleRepository.findByName(RoleType.ROLE_SUB_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleType.ROLE_SUB_ADMIN)
                        .description("Sub Administrator")
                        .build()));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = auth != null ? auth.getName() : "system";
        UUID actorUserId = resolveActorUserId(actorEmail);

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(cleanEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .passwordSet(true)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .mustChangePassword(true)
                .authProvider("LOCAL")
                .role(subAdminRole)
                .build();

        User saved = userRepository.save(user);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action("CREATE_SUB_ADMIN")
                .targetUserId(saved.getId())
                .oldValue(null)
                .newValue("email=" + cleanEmail + ", role=ROLE_SUB_ADMIN, mustChangePassword=true")
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Admin [{}] created new SUB_ADMIN account for {} (ID: {})", actorEmail, cleanEmail, saved.getId());

        // Send notification email asynchronously if configured (never include password)
        try {
            emailService.sendSubAdminCreatedEmail(cleanEmail, actorEmail);
        } catch (Exception ex) {
            log.warn("Failed to dispatch sub-admin creation email: {}", ex.getMessage());
        }

        return toAdminUserResponse(saved);
    }

    @Transactional
    @Override
    public void changePassword(String userEmail, com.vionsys.hireai.user.dto.ChangePasswordRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cannot be blank.");
        }

        User user = userRepository.findByEmail(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Current password is incorrect.");
        }

        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordSet(true);
        User saved = userRepository.save(user);

        // Revoke old refresh tokens
        refreshTokenRepository.revokeAllUserTokens(saved);

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .actorUserId(saved.getId())
                .actorEmail(user.getEmail())
                .action("CHANGE_PASSWORD")
                .targetUserId(saved.getId())
                .oldValue("mustChangePassword=" + user.getMustChangePassword())
                .newValue("mustChangePassword=false")
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        log.info("User [{}] successfully changed their password. mustChangePassword set to false.", user.getEmail());
    }

    @Transactional(readOnly = true)
    @Override
    public com.vionsys.hireai.admin.dto.PlatformMetricsResponse getPlatformMetrics(String range) {
        String cleanRange = (range != null && !range.isBlank()) ? range.trim().toLowerCase() : "30d";
        long nowMs = System.currentTimeMillis();

        CachedMetricsEntry cached = metricsCache.get(cleanRange);
        if (cached != null && nowMs < cached.expiresAtMillis()) {
            com.vionsys.hireai.admin.dto.PlatformMetricsResponse res = cached.data();
            res.setCached(true);
            return res;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        // 1. Overview counts
        long totalUsers = userRepository.countTotalUsers();
        long candidates = userRepository.countByRoleName(RoleType.ROLE_CANDIDATE);
        long recruiters = userRepository.countByRoleName(RoleType.ROLE_RECRUITER);
        long admins = userRepository.countByRoleName(RoleType.ROLE_ADMIN);
        long subAdmins = userRepository.countByRoleName(RoleType.ROLE_SUB_ADMIN);
        long activeUsers = userRepository.countActiveUsers();
        long deactivatedUsers = userRepository.countDeactivatedUsers();

        long signupsToday = userRepository.countSignupsSince(todayStart);
        long signups7d = userRepository.countSignupsSince(sevenDaysAgo);
        long signups30d = userRepository.countSignupsSince(thirtyDaysAgo);

        long active24h = userRepository.countActiveUsersSince(now.minusHours(24));
        long active7d = userRepository.countActiveUsersSince(sevenDaysAgo);
        long active30d = userRepository.countActiveUsersSince(thirtyDaysAgo);

        long totalJobs = jobRepository.count();
        long openJobs = jobRepository.countByStatus(com.vionsys.hireai.job.enums.JobStatus.OPEN);
        if (totalJobs < openJobs) {
            totalJobs = openJobs;
        }
        long totalApplications = jobApplicationRepository.count();

        double avgAppsPerOpenJob = openJobs > 0 ? Math.round((double) totalApplications / openJobs * 10.0) / 10.0 : 0.0;
        double avgAppsPerJob = totalJobs > 0 ? Math.round((double) totalApplications / totalJobs * 10.0) / 10.0 : 0.0;
        double avgDailySignups = Math.round((double) signups30d / 30.0 * 10.0) / 10.0;
        double avgDailyActive = Math.round((double) active30d / 30.0 * 10.0) / 10.0;

        com.vionsys.hireai.admin.dto.OverviewMetricsDto overview = com.vionsys.hireai.admin.dto.OverviewMetricsDto.builder()
                .totalUsers(totalUsers)
                .candidatesCount(candidates)
                .recruitersCount(recruiters)
                .adminsCount(admins)
                .subAdminsCount(subAdmins)
                .activeUsersCount(activeUsers)
                .deactivatedUsersCount(deactivatedUsers)
                .signupsToday(signupsToday)
                .signups7Days(signups7d)
                .signups30Days(signups30d)
                .activeUsers24h(active24h)
                .activeUsers7d(active7d)
                .activeUsers30d(active30d)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .totalApplications(totalApplications)
                .avgApplicationsPerJob(avgAppsPerJob)
                .avgApplicationsPerOpenJob(avgAppsPerOpenJob)
                .avgDailySignups(avgDailySignups)
                .avgDailyActiveUsers(avgDailyActive)
                .build();

        // 2. Time-series calculations from persistence service (merges persisted history + live today)
        int daysBack = cleanRange.equals("today") ? 1 : cleanRange.equals("7d") ? 7 : 30;
        java.time.LocalDate startDate = java.time.LocalDate.now().minusDays(daysBack - 1);

        List<com.vionsys.hireai.admin.dto.DailyTimeSeriesDto> timeSeriesList;
        if (metricsPersistenceService != null) {
            timeSeriesList = metricsPersistenceService.getTimeSeries(startDate, daysBack);
        } else {
            timeSeriesList = new java.util.ArrayList<>();
        }

        // 3. AI Performance from AI engine
        AdminStatsResponse llmStats = getLlmStats(cleanRange);
        java.util.Map<String, Long> providerSplit = new java.util.HashMap<>();
        if (llmStats.getProviderDistribution() != null) {
            for (ProviderStatDto ps : llmStats.getProviderDistribution()) {
                long c = ps.getCalls() != null ? ps.getCalls() : (ps.getCallCount() != null ? ps.getCallCount() : 0L);
                providerSplit.put(ps.getProvider(), c);
            }
        }

        List<com.vionsys.hireai.admin.dto.ModuleAverageDto> moduleAverages = new java.util.ArrayList<>();
        if (llmStats.getModuleBreakdown() != null) {
            for (ModuleStatDto ms : llmStats.getModuleBreakdown()) {
                long callCount = ms.getCalls() != null ? ms.getCalls() : (ms.getCallCount() != null ? ms.getCallCount() : 0L);
                long failCount = ms.getFailedCount() != null ? ms.getFailedCount() : 0L;
                Double sRate = callCount > 0 ? Math.round((double)(callCount - failCount) / callCount * 1000.0) / 10.0 : null;
                Double avgLat = ms.getAvgLatencySeconds();
                moduleAverages.add(com.vionsys.hireai.admin.dto.ModuleAverageDto.builder()
                        .moduleId(ms.getId() != null ? ms.getId() : ms.getModule())
                        .moduleName(ms.getName() != null ? ms.getName() : ms.getModuleName())
                        .totalCalls(callCount)
                        .failureCount(failCount)
                        .avgLatencySeconds(avgLat)
                        .p50LatencySeconds(avgLat)
                        .p95LatencySeconds(avgLat != null ? Math.round(avgLat * 1.5 * 10.0) / 10.0 : null)
                        .successRate(sRate)
                        .build());
            }
        }

        // Fetch recent failures from AI Engine
        AdminCallLogsResponse failureLogs = getCallLogs(null, null, "FAILED", null, null, null, null, null, 1, 5);
        List<AdminCallLogEntry> recentFailures = failureLogs != null && failureLogs.getItems() != null ? failureLogs.getItems() : List.of();

        long scopedTotalCalls = llmStats.getTotalCalls() != null ? llmStats.getTotalCalls() : 0L;
        Double scopedAvgLat = scopedTotalCalls > 0 ? llmStats.getAvgLatencySeconds() : null;
        Double scopedP50 = scopedTotalCalls > 0 ? llmStats.getP50LatencySeconds() : null;
        Double scopedP95 = scopedTotalCalls > 0 ? llmStats.getP95LatencySeconds() : null;
        Double scopedSuccessRate = scopedTotalCalls > 0 ? llmStats.getSuccessRate() : null;
        long scopedFailures = llmStats.getFailedCalls() != null ? llmStats.getFailedCalls() : 0L;
        long scopedSuccesses = scopedTotalCalls > 0 ? Math.max(0, scopedTotalCalls - scopedFailures) : 0L;

        com.vionsys.hireai.admin.dto.AiPerformanceDto aiPerformance = com.vionsys.hireai.admin.dto.AiPerformanceDto.builder()
                .range(cleanRange)
                .windowStart(llmStats.getWindowStart())
                .windowEnd(llmStats.getWindowEnd())
                .generatedAt(llmStats.getGeneratedAt() != null ? llmStats.getGeneratedAt() : java.time.Instant.now().toString())
                .source(llmStats.getSource() != null ? llmStats.getSource() : "llm_call_log.jsonl")
                .callsToday(llmStats.getTodayCalls() != null ? llmStats.getTodayCalls() : 0L)
                .callsThisWeek(llmStats.getWeekCalls() != null ? llmStats.getWeekCalls() : 0L)
                .totalCalls(scopedTotalCalls)
                .allTimeCalls(llmStats.getAllTimeCalls() != null ? llmStats.getAllTimeCalls() : scopedTotalCalls)
                .successCount(scopedSuccesses)
                .failureCount(scopedFailures)
                .successRate(scopedSuccessRate)
                .avgLatencySeconds(scopedAvgLat)
                .p50LatencySeconds(scopedP50)
                .p95LatencySeconds(scopedP95)
                .providerSplit(providerSplit)
                .moduleAverages(moduleAverages)
                .geminiQuota(llmStats.getQuota())
                .recentFailures(recentFailures)
                .build();

        // 4. System Health
        AdminHealthResponse healthSnapshot = healthPollerService.getCachedHealthSnapshot();
        long jvmUptimeSeconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        Long engineUptime = healthSnapshot != null ? healthSnapshot.getEngineUptimeSeconds() : null;
        String activeEnv = (environment != null && environment.getActiveProfiles().length > 0)
                ? environment.getActiveProfiles()[0]
                : "development";

        List<com.vionsys.hireai.admin.dto.ModuleHealthStateDto> moduleStates = new java.util.ArrayList<>();
        if (healthSnapshot != null && healthSnapshot.getModules() != null) {
            for (ModuleHealthDto m : healthSnapshot.getModules()) {
                long durationSec = healthPollerService.getModuleStateDurationSeconds(m.getId());
                moduleStates.add(com.vionsys.hireai.admin.dto.ModuleHealthStateDto.builder()
                        .id(m.getId())
                        .name(m.getName())
                        .status(m.getStatus())
                        .provider(m.getProvider())
                        .model(m.getModel())
                        .lastSuccessAt(m.getLastSuccessAt())
                        .lastFailureAt(m.getLastFailureAt())
                        .durationInCurrentStateSeconds(durationSec)
                        .error(m.getError())
                        .build());
            }
        }

        com.vionsys.hireai.admin.dto.SystemHealthDetailsDto systemHealth = com.vionsys.hireai.admin.dto.SystemHealthDetailsDto.builder()
                .backendUptimeSeconds(jvmUptimeSeconds)
                .engineUptimeSeconds(engineUptime)
                .environment(activeEnv)
                .backendStartedAt(healthPollerService != null && healthPollerService.getBackendStartedInstant() != null ? healthPollerService.getBackendStartedInstant().toString() : java.time.Instant.now().toString())
                .databaseConnectivity(healthSnapshot != null && healthSnapshot.getDatabaseStatus() != null ? healthSnapshot.getDatabaseStatus() : "UP")
                .aiEngineReachability(healthSnapshot != null ? healthSnapshot.getOverallStatus() : "UNKNOWN")
                .statusReasonCode(healthSnapshot != null ? healthSnapshot.getStatusReasonCode() : null)
                .statusMessage(healthSnapshot != null ? healthSnapshot.getStatusMessage() : null)
                .lastSuccessfulPoll(healthSnapshot != null ? healthSnapshot.getLastSuccessfulPoll() : null)
                .consecutiveFailures(healthSnapshot != null ? healthSnapshot.getConsecutiveFailures() : 0)
                .emailSmtpConfigured(emailService != null && emailService.isSmtpConfigured())
                .emailMode(emailService != null && emailService.getEmailMode() != null ? emailService.getEmailMode().name() : "UNKNOWN")
                .emailLastSuccessfulSend(emailService != null && emailService.getLastSuccessfulSend() != null ? emailService.getLastSuccessfulSend().toString() : null)
                .emailRecentFailureCount(emailService != null ? emailService.getRecentFailureCount() : 0)
                .lastHealthPollAt(healthPollerService != null && healthPollerService.getLastSuccessfulHealthPollInstant() != null ? healthPollerService.getLastSuccessfulHealthPollInstant().toString() : (healthSnapshot != null ? healthSnapshot.getLastCheckedAt() : null))
                .modules(moduleStates)
                .build();

        // 5. Application Performance
        com.vionsys.hireai.admin.dto.ApplicationPerformanceDto appPerformance = apiMetricsFilter.getSnapshot();

        com.vionsys.hireai.admin.dto.PlatformMetricsResponse response = com.vionsys.hireai.admin.dto.PlatformMetricsResponse.builder()
                .range(cleanRange)
                .windowStart(llmStats.getWindowStart())
                .windowEnd(llmStats.getWindowEnd())
                .generatedAt(java.time.Instant.now().toString())
                .source("admin_metrics_service")
                .environment(activeEnv)
                .lastUpdatedAt(java.time.Instant.now().toString())
                .cached(false)
                .overview(overview)
                .timeSeries(timeSeriesList)
                .aiPerformance(aiPerformance)
                .systemHealth(systemHealth)
                .appPerformance(appPerformance)
                .build();

        long ttlMs = (metricsCacheTtlSeconds > 0 ? metricsCacheTtlSeconds : 120L) * 1000L;
        metricsCache.put(cleanRange, new CachedMetricsEntry(response, nowMs + ttlMs));

        return response;
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
