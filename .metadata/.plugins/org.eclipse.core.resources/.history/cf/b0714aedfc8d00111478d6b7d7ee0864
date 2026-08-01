package com.vionsys.hireai.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.user.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
        SELECT u
        FROM User u
        JOIN FETCH u.role
        WHERE u.email = :email
    """)
    Optional<User> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);
}