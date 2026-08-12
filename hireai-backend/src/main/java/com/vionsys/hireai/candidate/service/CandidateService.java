package com.vionsys.hireai.candidate.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vionsys.hireai.candidate.dto.CandidateProfileRequest;
import com.vionsys.hireai.candidate.dto.CandidateRequest;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.filter.CandidateFilter;

public interface CandidateService {

    // =========================================================
    // GENERAL CANDIDATE MANAGEMENT
    // =========================================================

    CandidateResponse createCandidate(
            CandidateRequest request
    );

    CandidateResponse getCandidateById(
            UUID candidateId
    );

    Page<CandidateResponse> getAllCandidates(
            CandidateFilter filter,
            Pageable pageable
    );

    CandidateResponse updateCandidate(
            UUID candidateId,
            CandidateRequest request
    );

    void deleteCandidate(
            UUID candidateId
    );


    // =========================================================
    // AUTHENTICATED CANDIDATE PROFILE
    // =========================================================

    CandidateResponse createMyProfile(
            UUID userId,
            CandidateProfileRequest request
    );

    CandidateResponse getMyProfile(
            UUID userId
    );

    CandidateResponse updateMyProfile(
            UUID userId,
            CandidateProfileRequest request
    );
}