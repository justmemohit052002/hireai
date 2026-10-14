package com.vionsys.hireai.admin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vionsys.hireai.admin.dto.AdminAuditLogResponse;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.common.enums.RoleType;

public interface AdminService {

    /**
     * Search and list system users with role and status filters
     */
    Page<AdminUserResponse> getUsers(String search, RoleType role, Boolean enabled, Pageable pageable);

    /**
     * Update user role (e.g. promote to ADMIN or RECRUITER)
     */
    AdminUserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request);

    /**
     * Soft activate / deactivate user account or lock / unlock
     */
    AdminUserResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request);

    /**
     * Return AI Engine module health and reachability (cached via background poller)
     */
    AdminHealthResponse getHealthSummary();

    /**
     * Return LLM usage, cost estimates, and quota tracking
     */
    AdminStatsResponse getLlmStats(String range);

    /**
     * Query AI engine call logs / audit trail with range, correlation, and filters
     */
    AdminCallLogsResponse getCallLogs(
            String query, String taskType, String status, String provider,
            String from, String to, String correlationId, String candidateRef,
            int page, int size);

    /**
     * Retrieve single AI call log entry by ID
     */
    AdminCallLogEntry getCallLogById(String logId);

    /**
     * Return read-only provider configuration status (no plain secrets)
     */
    ProvidersConfigDto getProvidersStatus();

    /**
     * Retrieve paginated administrative user action audit log
     */
    Page<AdminAuditLogResponse> getAuditLogs(Pageable pageable);

    /**
     * Provision a new sub-administrator with initial credentials and forced password change flag
     */
    AdminUserResponse createSubAdmin(com.vionsys.hireai.admin.dto.CreateSubAdminRequest request);

    /**
     * Enforce and execute password change for sub-admin first login or self-service update
     */
    void changePassword(String userEmail, com.vionsys.hireai.user.dto.ChangePasswordRequest request);

    /**
     * Return comprehensive platform metrics (overview counts, time-series, AI performance, system health, endpoint metrics)
     */
    com.vionsys.hireai.admin.dto.PlatformMetricsResponse getPlatformMetrics(String range);

    /**
     * Diagnostic: Send a test email to verify outbound SMTP reachability and credentials
     */
    java.util.Map<String, Object> testEmailDelivery(String recipientEmail);
}
