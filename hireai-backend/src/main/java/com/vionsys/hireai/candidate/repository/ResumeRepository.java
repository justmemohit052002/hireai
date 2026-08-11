package com.vionsys.hireai.candidate.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.candidate.entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

	 Optional<Resume> findByCandidate_Id(UUID candidateId);

}
