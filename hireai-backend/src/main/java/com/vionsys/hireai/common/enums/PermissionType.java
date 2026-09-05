package com.vionsys.hireai.common.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionType {

    // Job permissions
    JOB_READ("job:read"),
    JOB_CREATE("job:create"),
    JOB_UPDATE("job:update"),
    JOB_DELETE("job:delete"),

    // Application permissions
    APPLICATION_CREATE("application:create"),
    APPLICATION_READ("application:read"),
    APPLICATION_UPDATE_STATUS("application:update_status"),
    APPLICATION_DELETE("application:delete"),

    // Candidate profile & resume permissions
    CANDIDATE_PROFILE_READ("candidate:profile_read"),
    CANDIDATE_PROFILE_WRITE("candidate:profile_write"),
    CANDIDATE_SEARCH("candidate:search"),
    RESUME_MANAGE("resume:manage"),
    RESUME_READ("resume:read"),

    // Recruiter permissions
    RECRUITER_PROFILE_READ("recruiter:profile_read"),
    RECRUITER_PROFILE_WRITE("recruiter:profile_write"),

    // Admin & system permissions
    ADMIN_USER_MANAGE("admin:user_manage"),
    ADMIN_SYSTEM_CONFIG("admin:system_config");

    private final String permission;

    public static Set<String> getPermissionsForRole(RoleType roleType) {
        if (roleType == null) {
            return Collections.emptySet();
        }

        return switch (roleType) {
            case ROLE_ADMIN -> EnumSet.allOf(PermissionType.class)
                    .stream()
                    .map(PermissionType::getPermission)
                    .collect(Collectors.toSet());

            case ROLE_RECRUITER -> EnumSet.of(
                    JOB_READ,
                    JOB_CREATE,
                    JOB_UPDATE,
                    JOB_DELETE,
                    APPLICATION_READ,
                    APPLICATION_UPDATE_STATUS,
                    CANDIDATE_SEARCH,
                    RESUME_READ,
                    RECRUITER_PROFILE_READ,
                    RECRUITER_PROFILE_WRITE
            ).stream().map(PermissionType::getPermission).collect(Collectors.toSet());

            case ROLE_CANDIDATE -> EnumSet.of(
                    JOB_READ,
                    APPLICATION_CREATE,
                    APPLICATION_READ,
                    CANDIDATE_PROFILE_READ,
                    CANDIDATE_PROFILE_WRITE,
                    RESUME_MANAGE,
                    RESUME_READ
            ).stream().map(PermissionType::getPermission).collect(Collectors.toSet());
        };
    }
}
