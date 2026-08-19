package com.vionsys.hireai.ai.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.ai.entity.InterviewAssessment;

public interface InterviewAssessmentRepository extends JpaRepository<InterviewAssessment, UUID> {

    Optional<InterviewAssessment> findByJobApplicationId(UUID jobApplicationId);
}
