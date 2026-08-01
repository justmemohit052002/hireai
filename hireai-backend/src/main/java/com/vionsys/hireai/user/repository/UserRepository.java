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
        WHERE u.email = :email
    """)
    Optional<User> findByEmail(@Param("email") String email);

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role
        WHERE u.id = :id
    """)
    Optional<User> findById(@Param("id") UUID id);

    boolean existsByEmail(String email);
}