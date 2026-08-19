package com.vionsys.hireai.recruiter.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateRequest;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateResponse;
import com.vionsys.hireai.ai.service.AiJdService;
import com.vionsys.hireai.ai.service.DecisionEngineService;
import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.service.AtsMatchScoringService;
import com.vionsys.hireai.candidate.dto.ApiResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.exception.JobNotFoundException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/recruiter/ai")
@RequiredArgsConstructor
@Tag(name = "Recruiter AI Engine", description = "AI services for recruiters: JD generation, Match calculation, Decision Engine")
@PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
public class AiRecruiterController {

    private final AiJdService aiJdService;
    private final AtsMatchScoringService atsMatchScoringService;
    private final DecisionEngineService decisionEngineService;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;

    @Operation(summary = "Generate Job Description, responsibilities, and interview questions using AI")
    @PostMapping("/jd/generate")
    public ResponseEntity<ApiResponse<AiJdGenerateResponse>> generateJobDescription(
            @Valid @RequestBody AiJdGenerateRequest request) {

        AiJdGenerateResponse response = aiJdService.generateJobDescription(request);

        return ResponseEntity.ok(ApiResponse.<AiJdGenerateResponse>builder()
                .success(true)
                .message("Job description generated successfully by AI")
                .data(response)
                .build());
    }

    @Operation(summary = "Compute semantic vector skill match between candidate and job on demand")
    @PostMapping("/match/{jobId}/{candidateId}")
    public ResponseEntity<ApiResponse<AtsMatchResult>> calculateMatchScore(
            @PathVariable UUID jobId,
            @PathVariable UUID candidateId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + jobId));

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with id: " + candidateId));

        AtsMatchResult result = atsMatchScoringService.computeAtsScore(candidate, job);

        return ResponseEntity.ok(ApiResponse.<AtsMatchResult>builder()
                .success(true)
                .message("ATS Match score computed successfully")
                .data(result)
                .build());
    }

    @Operation(summary = "Execute AI Decision Engine calculation and classify job application")
    @PostMapping("/applications/{applicationId}/decision")
    public ResponseEntity<ApiResponse<AiDecisionResponse>> finalizeDecision(
            Authentication authentication,
            @PathVariable UUID applicationId) {

        UUID userId = getUserId(authentication);
        AiDecisionResponse response = decisionEngineService.finalizeApplicationDecisionForRecruiter(userId, applicationId);

        return ResponseEntity.ok(ApiResponse.<AiDecisionResponse>builder()
                .success(true)
                .message("Application decision finalized successfully by AI Decision Engine")
                .data(response)
                .build());
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user.getId();
        }
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            return user.getId();
        }
        throw new IllegalStateException("Authenticated user not found");
    }
}
