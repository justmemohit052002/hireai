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
}