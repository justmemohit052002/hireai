package com.vionsys.hireai.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vionsys.hireai.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmail(@Param("email") String email);

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role
        WHERE u.id = :id
    """)
    Optional<User> findById(@Param("id") UUID id);

    @Query(value = """
        SELECT u
        FROM User u
        JOIN FETCH u.role r
        WHERE (:search IS NULL OR LOWER(u.firstName) LIKE :search OR LOWER(u.lastName) LIKE :search OR LOWER(u.email) LIKE :search)
          AND (:role IS NULL OR r.name = :role)
          AND (:enabled IS NULL OR u.enabled = :enabled)
    """, countQuery = """
        SELECT COUNT(u)
        FROM User u
        JOIN u.role r
        WHERE (:search IS NULL OR LOWER(u.firstName) LIKE :search OR LOWER(u.lastName) LIKE :search OR LOWER(u.email) LIKE :search)
          AND (:role IS NULL OR r.name = :role)
          AND (:enabled IS NULL OR u.enabled = :enabled)
    """)
    org.springframework.data.domain.Page<User> searchUsers(
            @Param("search") String search,
            @Param("role") com.vionsys.hireai.common.enums.RoleType role,
            @Param("enabled") Boolean enabled,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    @Query("SELECT COUNT(u) FROM User u JOIN u.role r WHERE r.name = com.vionsys.hireai.common.enums.RoleType.ROLE_ADMIN AND u.enabled = true AND u.accountNonLocked = true")
    long countActiveUnlockedAdmins();

    @Query("SELECT COUNT(u) FROM User u JOIN u.role r WHERE r.name = :role")
    long countByRoleName(@Param("role") com.vionsys.hireai.common.enums.RoleType role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false")
    long countDeactivatedUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countSignupsSince(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since")
    long countActiveUsersSince(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :since")
    java.util.List<User> findUsersCreatedSince(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since")
    java.util.List<User> findUsersActiveSince(@Param("since") java.time.LocalDateTime since);

    boolean existsByEmail(String email);
}