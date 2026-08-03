package com.vionsys.hireai.recruiter.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.vionsys.hireai.recruiter.entity.RecruiterProfile;


public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, UUID> {

    Optional<RecruiterProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

}