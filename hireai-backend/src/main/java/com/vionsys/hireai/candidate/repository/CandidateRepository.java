package com.vionsys.hireai.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.candidate.entity.Candidate;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID>,JpaSpecificationExecutor<Candidate>{
	
	    boolean existsByEmail(String email);

	    boolean existsByPhone(String phone);

	    boolean existsByCandidateId(String candidateId);

	    Optional<Candidate> findByCandidateId(String candidateId);

	    Optional<Candidate> findByEmail(String email);
	    
	    Optional<Candidate> findByPhone(String phone);
	    
	    Optional<Candidate> findTopByOrderByCreatedAtDesc();
}
