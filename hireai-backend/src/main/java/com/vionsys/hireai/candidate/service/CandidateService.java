package com.vionsys.hireai.candidate.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.dto.CreateCandidateRequest;
import com.vionsys.hireai.candidate.dto.UpdateCandidateRequest;
import com.vionsys.hireai.candidate.filter.CandidateFilter;

public interface CandidateService {

	CandidateResponse createCandidate(CreateCandidateRequest request);

    CandidateResponse getCandidateById(UUID id);

    Page<CandidateResponse> getAllCandidates(
    		CandidateFilter filter,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    CandidateResponse updateCandidate(UUID id,
                                      UpdateCandidateRequest request);

    String deleteCandidate(UUID id);
}
