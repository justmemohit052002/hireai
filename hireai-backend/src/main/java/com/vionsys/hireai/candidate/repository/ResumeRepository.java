package com.vionsys.hireai.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.candidate.entity.Resume;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

	Optional<Resume> findByCandidateId(UUID candidateId);

	boolean existsByCandidateId(UUID candidateId);
}