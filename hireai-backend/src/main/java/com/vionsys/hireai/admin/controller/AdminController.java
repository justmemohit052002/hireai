package com.vionsys.hireai.admin.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.common.enums.RoleType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "System Administration & Health", description = "Operations and IT administrative endpoints for AI engine health, LLM metrics, audit logs, and user access control")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Get AI Module Health Summary", description = "Polls and checks operational status of all 6 AI engine modules with 30s TTL caching")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthSummary() {
        Map<String, Object> health = adminService.getHealthSummary();
        return ResponseEntity.ok(ApiResponse.success(health, "System health retrieved successfully"));
    }

    @Operation(summary = "Get LLM Usage & Quota Statistics", description = "Fetches call volumes, provider breakdown, average latencies, and quota usage from AI call logs")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLlmStats() {
        Map<String, Object> stats = adminService.getLlmStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "LLM statistics retrieved successfully"));
    }

    @Operation(summary = "Query AI Call Logs & Audit Trail", description = "Searchable log viewer for AI engine invocations with truncated privacy-safe previews")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCallLogs(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "taskType", required = false) String taskType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "provider", required = false) String provider,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Map<String, Object> logs = adminService.getCallLogs(query, taskType, status, provider, page, size);
        return ResponseEntity.ok(ApiResponse.success(logs, "Call logs retrieved successfully"));
    }

    @Operation(summary = "Get Provider & API Key Status", description = "Read-only configuration view showing provider status and masked API key presence")
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProvidersStatus() {
        Map<String, Object> providers = adminService.getProvidersStatus();
        return ResponseEntity.ok(ApiResponse.success(providers, "Provider configuration retrieved successfully"));
    }

    @Operation(summary = "List & Search System Users", description = "Paginated list of system accounts with role and status filtering")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) RoleType role,
            @RequestParam(name = "enabled", required = false) Boolean enabled,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminUserResponse> users = adminService.getUsers(search, role, enabled, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "User accounts retrieved successfully"));
    }

    @Operation(summary = "Update User Role", description = "Assign a new role to an existing user account (requires ADMIN role)")
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        AdminUserResponse updated = adminService.updateUserRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "User role updated successfully"));
    }

    @Operation(summary = "Update User Account Status", description = "Soft-deactivate/activate or lock/unlock a user account")
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        AdminUserResponse updated = adminService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "User account status updated successfully"));
    }
}
