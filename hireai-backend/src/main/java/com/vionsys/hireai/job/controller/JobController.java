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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;


    // =========================================================
    // RECRUITER - CREATE JOB
    // =========================================================

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
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

    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<JobResponse>> getMyJobs() {

        List<JobResponse> response =
                jobService.getMyJobs();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // CANDIDATE - OPEN JOBS
    // =========================================================

    @GetMapping("/open")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER')")
    public ResponseEntity<List<JobResponse>> getOpenJobs() {

        List<JobResponse> response =
                jobService.getOpenJobs();

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // RECRUITER - GET OWN JOB
    // =========================================================

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable UUID jobId) {

        JobResponse response =
                jobService.getJobById(jobId);

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // RECRUITER - UPDATE JOB
    // =========================================================

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
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

    @PatchMapping("/{jobId}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> closeJob(
            @PathVariable UUID jobId) {

        jobService.closeJob(jobId);

        return ResponseEntity
                .noContent()
                .build();
    }
}