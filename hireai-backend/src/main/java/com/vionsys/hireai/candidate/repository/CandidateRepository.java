package com.vionsys.hireai.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.vionsys.hireai.candidate.entity.Candidate;

public interface CandidateRepository
		extends JpaRepository<Candidate, UUID>,
		JpaSpecificationExecutor<Candidate> {

	Optional<Candidate> findByCandidateId(
			String candidateId
	);

	Optional<Candidate> findByEmail(
			String email
	);

	Optional<Candidate> findByPhone(
			String phone
	);

	boolean existsByEmail(
			String email
	);

	boolean existsByPhone(
			String phone
	);

	boolean existsByCandidateId(
			String candidateId
	);

	Optional<Candidate> findTopByOrderByCreatedAtDesc();

	Optional<Candidate> findByUserId(
			UUID userId
	);

	boolean existsByUserId(
			UUID userId
	);

	@org.springframework.data.jpa.repository.Query(value = "SELECT candidate_id FROM candidates WHERE candidate_id LIKE CONCAT('CAN-', :year, '-%') ORDER BY candidate_id DESC LIMIT 1", nativeQuery = true)
	Optional<String> findTopCandidateIdForYear(@org.springframework.data.repository.query.Param("year") int year);

	@org.springframework.data.jpa.repository.Query(value = "SELECT COUNT(*) > 0 FROM candidates WHERE candidate_id = :candidateId", nativeQuery = true)
	boolean existsByCandidateIdNative(@org.springframework.data.repository.query.Param("candidateId") String candidateId);
}