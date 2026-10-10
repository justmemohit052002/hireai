package com.vionsys.hireai.admin.service.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.common.enums.RoleType;
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
    private final AiEngineClient aiEngineClient;

    // In-memory cache for health summary (TTL: 30 seconds)
    private volatile Map<String, Object> cachedHealthSummary;
    private volatile Instant lastHealthCheckTime = Instant.EPOCH;
    private static final long HEALTH_CACHE_TTL_SECONDS = 30;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponse> getUsers(String search, RoleType role, Boolean enabled, Pageable pageable) {
        String querySearch = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;
        
        Page<User> usersPage = userRepository.searchUsers(querySearch, role, enabled, pageable);
        return usersPage.map(this::toAdminUserResponse);
    }

    @Transactional
    @Override
    public AdminUserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Prevent admin from accidentally demoting themselves
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail()) && request.getRole() != RoleType.ROLE_ADMIN) {
            throw new IllegalArgumentException("Admins cannot revoke their own administrator privileges.");
        }

        Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRole()));

        user.setRole(newRole);
        User saved = userRepository.save(user);
        log.info("Admin updated user {} ({}) role to {}", saved.getId(), saved.getEmail(), newRole.getName());
        return toAdminUserResponse(saved);
    }

    @Transactional
    @Override
    public AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Prevent admin from deactivating themselves
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName().equalsIgnoreCase(user.getEmail()) && Boolean.FALSE.equals(request.getEnabled())) {
            throw new IllegalArgumentException("Admins cannot deactivate their own user account.");
        }

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
        log.info("Admin updated user {} ({}) status: enabled={}, nonLocked={}",
                saved.getId(), saved.getEmail(), saved.getEnabled(), saved.getAccountNonLocked());
        return toAdminUserResponse(saved);
    }

    @Override
    public Map<String, Object> getHealthSummary() {
        Instant now = Instant.now();
        if (cachedHealthSummary != null && lastHealthCheckTime.plusSeconds(HEALTH_CACHE_TTL_SECONDS).isAfter(now)) {
            return cachedHealthSummary;
        }

        Map<String, Object> freshHealth = aiEngineClient.getAdminHealthSummary().orElseGet(() -> {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("service", "HireAI AI Engine");
            fallback.put("overallStatus", "OFFLINE");
            fallback.put("checkedAt", Instant.now().toString());
            fallback.put("error", "AI Engine service is unreachable at " + aiEngineClient.getProperties().getBaseUrl());
            fallback.put("modules", List.of(
                    Map.of("id", "jd_generator", "name", "Job Description Generator", "status", "OFFLINE"),
                    Map.of("id", "resume_parser", "name", "Resume Parser", "status", "OFFLINE"),
                    Map.of("id", "match_engine", "name", "Match Engine", "status", "OFFLINE"),
                    Map.of("id", "chatbot", "name", "Pre-Screening Chatbot", "status", "OFFLINE"),
                    Map.of("id", "interview_ai", "name", "Interview AI", "status", "OFFLINE"),
                    Map.of("id", "decision_engine", "name", "Decision Engine", "status", "OFFLINE")
            ));
            return fallback;
        });

        // Add backend platform health info
        Map<String, Object> enriched = new HashMap<>(freshHealth);
        enriched.put("backendStatus", "HEALTHY");
        enriched.put("databaseStatus", "CONNECTED");
        enriched.put("activeUsersCount", userRepository.countActiveUsers());
        enriched.put("totalUsersCount", userRepository.countTotalUsers());

        cachedHealthSummary = enriched;
        lastHealthCheckTime = now;
        return enriched;
    }

    @Override
    public Map<String, Object> getLlmStats() {
        return aiEngineClient.getAdminStats().orElseGet(() -> {
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalCalls", 0);
            emptyStats.put("todayCalls", 0);
            emptyStats.put("weekCalls", 0);
            emptyStats.put("failedCalls", 0);
            emptyStats.put("successRate", 100.0);
            emptyStats.put("avgLatencySeconds", 0.0);
            emptyStats.put("quota", Map.of(
                    "provider", "gemini",
                    "tier", "Free Tier",
                    "dailyLimit", 250,
                    "usedToday", 0,
                    "remainingToday", 250,
                    "percentageUsed", 0.0
            ));
            emptyStats.put("providerDistribution", List.of());
            emptyStats.put("moduleBreakdown", List.of());
            return emptyStats;
        });
    }

    @Override
    public Map<String, Object> getCallLogs(String query, String taskType, String status, String provider, int page, int size) {
        return aiEngineClient.getAdminLogs(query, taskType, status, provider, page, size).orElseGet(() -> {
            Map<String, Object> emptyLogs = new HashMap<>();
            emptyLogs.put("items", List.of());
            emptyLogs.put("totalElements", 0);
            emptyLogs.put("page", page);
            emptyLogs.put("size", size);
            emptyLogs.put("totalPages", 0);
            return emptyLogs;
        });
    }

    @Override
    public Map<String, Object> getProvidersStatus() {
        Map<String, Object> health = getHealthSummary();
        @SuppressWarnings("unchecked")
        Map<String, Object> providers = (Map<String, Object>) health.get("providers");
        if (providers != null) {
            return providers;
        }

        return Map.of(
                "activeProvider", "ollama",
                "activeModel", "llama3.2:3b",
                "ollama", Map.of("name", "Ollama (Local)", "isConfigured", true, "reachable", false),
                "gemini", Map.of("name", "Google Gemini", "isConfigured", false, "keyMasked", "Not Configured"),
                "groq", Map.of("name", "Groq API", "isConfigured", false, "keyMasked", "Not Configured")
        );
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
}
