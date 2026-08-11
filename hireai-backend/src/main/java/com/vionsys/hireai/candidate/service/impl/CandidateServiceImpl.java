package com.vionsys.hireai.candidate.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.dto.CreateCandidateRequest;
import com.vionsys.hireai.candidate.dto.UpdateCandidateRequest;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.exception.CandidateNotFoundException;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.filter.CandidateFilter;
import com.vionsys.hireai.candidate.mapper.CandidateMapper;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.service.CandidateService;
import com.vionsys.hireai.candidate.specification.CandidateSpecification;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateServiceImpl implements CandidateService{

	private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final CandidateIdGenerator candidateIdGenerator;
    
    
    private void validateCandidate(CreateCandidateRequest request) {
    	if (candidateRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Candidate with email '" + request.getEmail() + "' already exists."
            );
        }

        if (candidateRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException(
                    "Candidate with phone '" + request.getPhone() + "' already exists."
            );
        }
    }
    
    @Override
    public CandidateResponse createCandidate(CreateCandidateRequest request) {
    	
    	 validateCandidate(request);
    	 
        Candidate candidate = candidateMapper.toEntity(request);

        candidate.setCandidateId(candidateIdGenerator.generateCandidateId());

        candidate.setCandidateStatus(CandidateStatus.APPLIED);

        Candidate savedCandidate = candidateRepository.save(candidate);

        return candidateMapper.toResponse(savedCandidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(UUID id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() ->
                        new CandidateNotFoundException(
                                "Candidate not found with id : " + id));

        return candidateMapper.toResponse(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> getAllCandidates(
    		CandidateFilter filter,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Candidate> candidates = candidateRepository.findAll(CandidateSpecification.withFilter(filter),
                pageable);

        return candidates.map(candidateMapper::toResponse);
    }
    
    private void validateEmail(UUID candidateId, String email) {

        candidateRepository.findByEmail(email)
                .filter(candidate -> !candidate.getId().equals(candidateId))
                .ifPresent(candidate -> {
                    throw new DuplicateResourceException(
                            "Candidate with email '" + email + "' already exists."
                    );
                });
    }
    
    private void validatePhone(UUID candidateId, String phone) {

        candidateRepository.findByPhone(phone)
                .filter(candidate -> !candidate.getId().equals(candidateId))
                .ifPresent(candidate -> {
                    throw new DuplicateResourceException(
                            "Candidate with phone '" + phone + "' already exists."
                    );
                });
    }

    @Override
    public CandidateResponse updateCandidate(UUID id,
                                             UpdateCandidateRequest request) {
        // Find existing candidate
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() ->
                        new CandidateNotFoundException(
                                "Candidate not found with id : " + id));

        // Validate email if changed
        if (request.getEmail() != null
                && !request.getEmail().equals(candidate.getEmail())) {

            validateEmail(id, request.getEmail());
        }

        // Validate phone if changed
        if (request.getPhone() != null
                && !request.getPhone().equals(candidate.getPhone())) {

            validatePhone(id, request.getPhone());
        }

        // Update entity using MapStruct
        candidateMapper.updateCandidate(request, candidate);

        // Save updated entity
        Candidate updatedCandidate = candidateRepository.save(candidate);

        // Convert to response DTO
        return candidateMapper.toResponse(updatedCandidate);

    }

    @Override
    public String deleteCandidate(UUID id) {
    	Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() ->
                        new CandidateNotFoundException(
                                "Candidate not found with id : " + id));

        candidateRepository.delete(candidate);

        return "Candidate deleted successfully.";
    }

}
