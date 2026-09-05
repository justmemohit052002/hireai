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
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserDetails fromUser(User user) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

        if (user.getRole() != null && user.getRole().getName() != null) {
            // 1. Role authority (e.g. ROLE_RECRUITER)
            grantedAuthorities.add(new SimpleGrantedAuthority(user.getRole().getName().name()));

            // 2. Granular permission authorities (e.g. job:create, application:read)
            Set<String> permissions = PermissionType.getPermissionsForRole(user.getRole().getName());
            permissions.forEach(perm -> grantedAuthorities.add(new SimpleGrantedAuthority(perm)));
        }

        boolean isNonLocked = Boolean.TRUE.equals(user.getAccountNonLocked())
                && (user.getLockoutUntil() == null || LocalDateTime.now().isAfter(user.getLockoutUntil()));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getEnabled()),
                isNonLocked,
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