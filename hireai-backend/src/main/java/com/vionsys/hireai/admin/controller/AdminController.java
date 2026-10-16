package com.vionsys.hireai.admin.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.admin.dto.AdminAuditLogResponse;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.CreateSubAdminRequest;
import com.vionsys.hireai.admin.dto.PlatformMetricsResponse;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.annotation.CurrentUser;
import com.vionsys.hireai.user.dto.ChangePasswordRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN')")
@Tag(name = "System Administration & Health", description = "Operations and IT administrative endpoints for AI engine health, LLM metrics, audit logs, and user access control")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Get AI Module Health Summary", description = "Returns cached operational status of all 6 AI engine modules, background poller snapshot, and database liveness")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<AdminHealthResponse>> getHealthSummary() {
        AdminHealthResponse health = adminService.getHealthSummary();
        return ResponseEntity.ok(ApiResponse.success(health, "System health retrieved successfully"));
    }

    @Operation(summary = "Test Outbound Email Notification Delivery", description = "Admin diagnostic endpoint to test SMTP dispatch and verify delivery reachability")
    @PostMapping("/email/test")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> testEmailDelivery(
            @Parameter(description = "Recipient email address for test message", example = "admin@vionsys.com")
            @RequestParam String recipientEmail) {
        java.util.Map<String, Object> diagnostic = adminService.testEmailDelivery(recipientEmail);
        return ResponseEntity.ok(ApiResponse.success(diagnostic, "Email delivery diagnostic completed"));
    }

    @Operation(summary = "Get LLM Usage & Quota Statistics", description = "Fetches call volumes, provider breakdown, average latencies, and quota usage from AI call logs with optional date range filter")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getLlmStats(
            @Parameter(description = "Time range scope (today, week, all)", example = "today")
            @RequestParam(name = "range", defaultValue = "all", required = false) String range) {

        AdminStatsResponse stats = adminService.getLlmStats(range);
        return ResponseEntity.ok(ApiResponse.success(stats, "LLM statistics retrieved successfully"));
    }

    @Operation(summary = "Query AI Call Logs & Audit Trail", description = "Searchable log viewer for AI engine invocations with truncated privacy-safe previews, date filtering, and correlation tracking")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<AdminCallLogsResponse>> getCallLogs(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "taskType", required = false) String taskType,
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to,
            @RequestParam(name = "correlationId", required = false) String correlationId,
            @RequestParam(name = "candidateRef", required = false) String candidateRef,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        // Support module as alias of taskType
        String effectiveTaskType = (taskType != null && !taskType.isBlank()) ? taskType : module;

        AdminCallLogsResponse logs = adminService.getCallLogs(
                query, effectiveTaskType, status, provider, from, to, correlationId, candidateRef, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs, "Call logs retrieved successfully"));
    }

    @Operation(summary = "Get Single AI Call Log", description = "Fetches complete details for a single AI engine execution by ID, including sanitized error message for failures")
    @GetMapping("/logs/{logId}")
    public ResponseEntity<ApiResponse<AdminCallLogEntry>> getCallLogById(
            @Parameter(description = "Unique UUID of the call log entry")
            @PathVariable String logId) {

        AdminCallLogEntry entry = adminService.getCallLogById(logId);
        return ResponseEntity.ok(ApiResponse.success(entry, "Call log entry retrieved successfully"));
    }

    @Operation(summary = "Get Provider & API Key Status", description = "Read-only configuration view showing provider status and masked API key presence")
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<ProvidersConfigDto>> getProvidersStatus() {
        ProvidersConfigDto providers = adminService.getProvidersStatus();
        return ResponseEntity.ok(ApiResponse.success(providers, "Provider configuration retrieved successfully"));
    }

    @Operation(summary = "List & Search System Users", description = "Paginated list of system accounts with role and status filtering (size clamped to 100 max)")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) RoleType role,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AdminUserResponse> users = adminService.getUsers(search, role, enabled, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "User accounts retrieved successfully"));
    }

    @Operation(summary = "Get Comprehensive Platform Metrics", description = "Returns cached aggregate metrics: user growth, daily active users, AI performance, system health, and endpoint stats")
    @GetMapping("/platform-metrics")
    public ResponseEntity<ApiResponse<PlatformMetricsResponse>> getPlatformMetrics(
            @Parameter(description = "Time range scope (today, 7d, 30d)", example = "today")
            @RequestParam(name = "range", defaultValue = "today", required = false) String range) {

        PlatformMetricsResponse metrics = adminService.getPlatformMetrics(range);
        return ResponseEntity.ok(ApiResponse.success(metrics, "Platform metrics retrieved successfully"));
    }

    @Operation(summary = "Update User Role", description = "Assign a new role to an existing user account with self-demotion and last-admin protection")
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        AdminUserResponse updated = adminService.updateUserRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "User role updated successfully"));
    }

    @Operation(summary = "Create Sub-Administrator Account", description = "Provision a new sub-administrator with forced first-login password change")
    @PostMapping("/sub-admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createSubAdmin(
            @Valid @RequestBody CreateSubAdminRequest request) {

        AdminUserResponse created = adminService.createSubAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Sub-admin account created successfully"));
    }

    @Operation(summary = "Change Password", description = "Self-service password update or required password change on first sign-in")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser CustomUserDetails currentUser) {

        String email = currentUser != null ? currentUser.getUsername() : null;
        adminService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "Password changed successfully"));
    }

    @Operation(summary = "Update User Account Status", description = "Soft-deactivate/activate or lock/unlock a user account with self-protection and last-admin protection")
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        AdminUserResponse updated = adminService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "User account status updated successfully"));
    }

    @Operation(summary = "Get Administrative Action Audit Logs", description = "Paginated audit trail recording all user role changes, status updates, and account locks (newest first)")
    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<Page<AdminAuditLogResponse>>> getAuditLogs(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AdminAuditLogResponse> auditLogs = adminService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(auditLogs, "Admin audit trail retrieved successfully"));
    }
}
