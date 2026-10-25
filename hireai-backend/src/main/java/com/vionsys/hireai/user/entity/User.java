package com.vionsys.hireai.user.entity;

import java.util.UUID;

import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "password_set", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean passwordSet = true;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Builder.Default
    private Boolean enabled = false;

    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(name = "failed_login_attempts", columnDefinition = "integer default 0")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private java.time.LocalDateTime lockoutUntil;

    @Column(name = "auth_provider", length = 30)
    @Builder.Default
    private String authProvider = "LOCAL";

    @Column(name = "provider_id", length = 150)
    private String providerId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "account_status", length = 30)
    @Builder.Default
    private com.vionsys.hireai.user.enums.AccountStatus accountStatus = com.vionsys.hireai.user.enums.AccountStatus.ACTIVE;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @Column(name = "must_change_password", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean mustChangePassword = false;

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }

    public boolean hasPassword() {
        if (Boolean.FALSE.equals(this.passwordSet) || this.password == null || this.password.isBlank()) {
            return false;
        }
        if (Boolean.TRUE.equals(this.passwordSet)) {
            return true;
        }
        return this.providerId == null || "LOCAL".equalsIgnoreCase(this.authProvider);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    public java.util.Set<Role> getRoles() {
        if (roles == null) {
            roles = new java.util.HashSet<>();
        }
        return roles;
    }

    public void setRole(Role role) {
        this.role = role;
        if (role != null) {
            getRoles().add(role);
        }
    }

    public void addRole(Role role) {
        if (role != null) {
            getRoles().add(role);
        }
    }

    public boolean hasRole(RoleType roleType) {
        if (roleType == null) {
            return false;
        }
        if (role != null && role.getName() == roleType) {
            return true;
        }
        if (roles != null) {
            return roles.stream().anyMatch(r -> r.getName() == roleType);
        }
        return false;
    }

    public java.util.List<String> getRoleNames() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (roles != null) {
            roles.forEach(r -> {
                if (r.getName() != null) {
                    set.add(r.getName().name());
                }
            });
        }
        if (role != null && role.getName() != null) {
            set.add(role.getName().name());
        }
        return new java.util.ArrayList<>(set);
    }

    @PrePersist
    @PreUpdate
    public void syncRoles() {
        if (this.role != null) {
            getRoles().add(this.role);
        }
    }
}