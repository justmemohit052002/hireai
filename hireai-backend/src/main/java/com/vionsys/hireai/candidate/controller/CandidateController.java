package com.vionsys.hireai.candidate.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.vionsys.hireai.candidate.dto.ApiResponse;
import com.vionsys.hireai.candidate.dto.CandidateResponse;
import com.vionsys.hireai.candidate.dto.CreateCandidateRequest;
import com.vionsys.hireai.candidate.dto.UpdateCandidateRequest;
import com.vionsys.hireai.candidate.filter.CandidateFilter;
import com.vionsys.hireai.candidate.service.CandidateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Validated
public class CandidateController {

	 private final CandidateService candidateService;

	    /**
	     * Create a new candidate.
	     */
	    @PostMapping
	    public ResponseEntity<ApiResponse<CandidateResponse>> createCandidate(
	            @Valid @RequestBody CreateCandidateRequest request) {

	        CandidateResponse response = candidateService.createCandidate(request);

	        URI location = ServletUriComponentsBuilder
	                .fromCurrentRequest()
	                .path("/{id}")
	                .buildAndExpand(response.getId())
	                .toUri();

	        ApiResponse<CandidateResponse> apiResponse = ApiResponse.<CandidateResponse>builder()
	                .success(true)
	                .message("Candidate created successfully")
	                .data(response)
	                .build();

	        return ResponseEntity
	                .created(location)
	                .body(apiResponse);
	    }

	    /**
	     * Get candidate by UUID.
	     */
	    @GetMapping("/{id}")
	    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(
	            @PathVariable UUID id) {

	        CandidateResponse response = candidateService.getCandidateById(id);

	        ApiResponse<CandidateResponse> apiResponse = ApiResponse.<CandidateResponse>builder()
	                .success(true)
	                .message("Candidate retrieved successfully")
	                .data(response)
	                .build();

	        return ResponseEntity.ok(apiResponse);
	    }

	    /**
	     * Get candidates with pagination, sorting and dynamic filtering.
	     */
	    @GetMapping
	    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getAllCandidates(

	            @RequestParam(required = false) String candidateId,

	            @RequestParam(required = false) String firstName,

	            @RequestParam(required = false) String lastName,

	            @RequestParam(required = false) String email,

	            @RequestParam(required = false) String phone,

	            @RequestParam(required = false) String location,

	            @RequestParam(required = false) String candidateStatus,

	            @RequestParam(required = false) java.math.BigDecimal experience,

	            @RequestParam(required = false) String skill,

	            @RequestParam(defaultValue = "0") int page,

	            @RequestParam(defaultValue = "10") int size,

	            @RequestParam(defaultValue = "createdAt") String sortBy,

	            @RequestParam(defaultValue = "desc") String sortDir) {

	        CandidateFilter filter = new CandidateFilter();

	        filter.setCandidateId(candidateId);
	        filter.setFirstName(firstName);
	        filter.setLastName(lastName);
	        filter.setEmail(email);
	        filter.setPhone(phone);
	        filter.setLocation(location);
	        filter.setExperience(experience);
	        filter.setSkill(skill);

	        if (candidateStatus != null && !candidateStatus.isBlank()) {
	            filter.setCandidateStatus(
	                    com.vionsys.hireai.candidate.enums.CandidateStatus
	                            .valueOf(candidateStatus.toUpperCase()));
	        }

	        Page<CandidateResponse> candidates =
	                candidateService.getAllCandidates(
	                        filter,
	                        page,
	                        size,
	                        sortBy,
	                        sortDir);

	        ApiResponse<Page<CandidateResponse>> apiResponse =
	                ApiResponse.<Page<CandidateResponse>>builder()
	                        .success(true)
	                        .message("Candidates retrieved successfully")
	                        .data(candidates)
	                        .build();

	        return ResponseEntity.ok(apiResponse);
	    }

	    /**
	     * Update an existing candidate.
	     */
	    @PutMapping("/{id}")
	    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidate(
	            @PathVariable UUID id,
	            @Valid @RequestBody UpdateCandidateRequest request) {

	        CandidateResponse response =
	                candidateService.updateCandidate(id, request);

	        ApiResponse<CandidateResponse> apiResponse =
	                ApiResponse.<CandidateResponse>builder()
	                        .success(true)
	                        .message("Candidate updated successfully")
	                        .data(response)
	                        .build();

	        return ResponseEntity.ok(apiResponse);
	    }

	    /**
	     * Soft delete a candidate.
	     */
	    @DeleteMapping("/{id}")
	    public ResponseEntity<Void> deleteCandidate(
	            @PathVariable UUID id) {

	        candidateService.deleteCandidate(id);

	        return ResponseEntity.noContent().build();
	    }
}
