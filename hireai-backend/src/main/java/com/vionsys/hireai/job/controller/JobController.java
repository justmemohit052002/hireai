package com.vionsys.hireai.job.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;
import com.vionsys.hireai.job.service.JobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Postings & Discovery", description = "Endpoints for recruiter job posting lifecycle and candidate job discovery")
public class JobController {

    private final JobService jobService;


    // =========================================================
    // RECRUITER - CREATE JOB
    // =========================================================

    @Operation(summary = "Create Job Posting (Recruiter)", description = "Publish a new job opening with skills, salary range, experience level, currency (INR, USD, EUR, GBP, AED), and deadline")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('job:create')")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request) {

        JobResponse response =
                jobService.createJob(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // RECRUITER - MY JOBS
    // =========================================================

    @Operation(summary = "List My Job Postings (Recruiter)", description = "Fetch all job postings created by the authenticated recruiter")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('job:read')")
    public ResponseEntity<List<JobResponse>> getMyJobs() {

        List<JobResponse> response =
                jobService.getMyJobs();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // CANDIDATE - OPEN JOBS
    // =========================================================

    @Operation(summary = "Browse Open Jobs (Candidate & Recruiter)", description = "List all active, open job postings available for applications")
    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<List<JobResponse>> getOpenJobs() {

        List<JobResponse> response =
                jobService.getOpenJobs();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // RECRUITER - GET OWN JOB
    // =========================================================

    @Operation(summary = "Get Job by ID (Recruiter)", description = "Fetch details of a specific job posting owned by the recruiter")
    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or @jobSecurity.isJobOwner(#jobId, principal.id)")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable UUID jobId) {

        JobResponse response =
                jobService.getJobById(jobId);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // RECRUITER - UPDATE JOB
    // =========================================================

    @Operation(summary = "Update Job Posting (Recruiter)", description = "Update job title, description, skills, salary, location, or deadline")
    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or (hasAuthority('job:update') and @jobSecurity.isJobOwner(#jobId, principal.id))")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID jobId,
            @Valid @RequestBody JobRequest request) {

        JobResponse response =
                jobService.updateJob(
                        jobId,
                        request
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // RECRUITER - CLOSE JOB
    // =========================================================

    @Operation(summary = "Close Job Posting (Recruiter)", description = "Change job status to CLOSED so candidates can no longer apply")
    @PatchMapping("/{jobId}/close")
    @PreAuthorize("hasRole('ADMIN') or (hasAuthority('job:delete') and @jobSecurity.isJobOwner(#jobId, principal.id))")
    public ResponseEntity<Void> closeJob(
            @PathVariable UUID jobId) {

        jobService.closeJob(jobId);

        return ResponseEntity
                .noContent()
                .build();
    }
}