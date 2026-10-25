package com.vionsys.hireai.security;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.vionsys.hireai.common.enums.PermissionType;
import com.vionsys.hireai.user.entity.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean mustChangePassword;
    private final com.vionsys.hireai.common.enums.RoleType activeRole;
    private final Set<com.vionsys.hireai.common.enums.RoleType> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(
            UUID id,
            String email,
            String password,
            boolean enabled,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, enabled, accountNonLocked, false, null, null, authorities);
    }

    public static CustomUserDetails fromUser(User user) {
        return fromUser(user, null);
    }

    public static CustomUserDetails fromUser(User user, com.vionsys.hireai.common.enums.RoleType activeRoleOverride) {
        com.vionsys.hireai.common.enums.RoleType effectiveActiveRole = activeRoleOverride;
        if (effectiveActiveRole == null && user.getRole() != null) {
            effectiveActiveRole = user.getRole().getName();
        }

        Set<com.vionsys.hireai.common.enums.RoleType> userRoleTypes = new java.util.HashSet<>();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(r -> {
                if (r != null && r.getName() != null) {
                    userRoleTypes.add(r.getName());
                }
            });
        }
        if (user.getRole() != null && user.getRole().getName() != null) {
            userRoleTypes.add(user.getRole().getName());
        }
        if (effectiveActiveRole != null) {
            userRoleTypes.add(effectiveActiveRole);
        }

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        // AUTHORITIES ARE BUILT FROM activeRole ONLY!
        if (effectiveActiveRole != null) {
            // 1. Role authority (e.g. ROLE_RECRUITER)
            grantedAuthorities.add(new SimpleGrantedAuthority(effectiveActiveRole.name()));

            // 2. Granular permission authorities (e.g. job:create, application:read)
            Set<String> permissions = PermissionType.getPermissionsForRole(effectiveActiveRole);
            if (permissions != null) {
                permissions.forEach(perm -> grantedAuthorities.add(new SimpleGrantedAuthority(perm)));
            }
        }

        boolean isNonLocked = Boolean.TRUE.equals(user.getAccountNonLocked())
                && (user.getLockoutUntil() == null || LocalDateTime.now().isAfter(user.getLockoutUntil()));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "",
                Boolean.TRUE.equals(user.getEnabled()) && !Boolean.TRUE.equals(user.getDeleted()),
                isNonLocked,
                Boolean.TRUE.equals(user.getMustChangePassword()),
                effectiveActiveRole,
                userRoleTypes,
                grantedAuthorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}