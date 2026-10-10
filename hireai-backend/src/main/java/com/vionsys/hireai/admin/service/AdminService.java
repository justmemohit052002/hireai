package com.vionsys.hireai.admin.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vionsys.hireai.admin.dto.AdminUserResponse;
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
     * Return AI Engine module health and reachability (cached TTL 30s)
     */
    Map<String, Object> getHealthSummary();

    /**
     * Return LLM usage, cost estimates, and quota tracking
     */
    Map<String, Object> getLlmStats();

    /**
     * Query AI engine call logs / audit trail
     */
    Map<String, Object> getCallLogs(String query, String taskType, String status, String provider, int page, int size);

    /**
     * Return read-only provider configuration status (no plain secrets)
     */
    Map<String, Object> getProvidersStatus();
}
