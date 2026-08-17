package com.vionsys.hireai.candidate.service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vionsys.hireai.candidate.entity.Candidate;

public interface SkillService {
  
	void saveCadidate(UUID Candidate);
	
	ResponseEntity<Candidate> getCandidate();
}
