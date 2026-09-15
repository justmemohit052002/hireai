package com.vionsys.hireai.ai.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.ai.entity.InterviewQuestion;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findByAssessmentId(UUID assessmentId);

    Optional<InterviewQuestion> findByAssessmentIdAndQuestionId(UUID assessmentId, String questionId);
}
