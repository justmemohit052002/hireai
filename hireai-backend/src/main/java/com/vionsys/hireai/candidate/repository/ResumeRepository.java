package com.vionsys.hireai.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vionsys.hireai.candidate.entity.Resume;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findByCandidateId(UUID candidateId);

    Optional<Resume> findByCandidateIdAndDeletedFalse(UUID candidateId);

    boolean existsByCandidateId(UUID candidateId);

    boolean existsByCandidateIdAndDeletedFalse(UUID candidateId);

    @Query("SELECT r FROM Resume r WHERE r.candidate.user.id = :userId AND r.deleted = false")
    Optional<Resume> findByCandidateUserIdAndDeletedFalse(@Param("userId") UUID userId);

    Optional<Resume> findByAiJobId(String aiJobId);
}