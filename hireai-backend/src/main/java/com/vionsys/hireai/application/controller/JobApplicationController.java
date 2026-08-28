package com.vionsys.hireai.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.application.dto.JobApplicationRequest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.dto.UpdateApplicationStatusRequest;
import com.vionsys.hireai.application.service.JobApplicationService;
import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.annotation.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Job Applications & ATS", description = "Endpoints for candidate applications, ATS match scoring, and recruiter applicant tracking pipeline")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    // =========================================================
    // CANDIDATE: APPLY TO A JOB (WITH OPTIONAL RESUME UPLOAD)
    // =========================================================

    @PostMapping(value = "/jobs/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(
            summary = "Apply to Job with Resume Upload (Candidate)",
            description = "Submit an application with cover note and optional resume file. Uploads & parses resume skills, then calculates ATS match percentage against candidate profile."
    )
    public ResponseEntity<ApiResponse<JobApplicationResponse>> applyToJobWithResume(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID jobId,
            @RequestParam(value = "coverNote", required = false) String coverNote,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        JobApplicationRequest request = JobApplicationRequest.builder()
                .coverNote(coverNote)
                .build();

        JobApplicationResponse response = jobApplicationService.applyToJob(
                currentUser.getId(),
                jobId,
                request,
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Application submitted successfully with ATS match score calculated"));
    }

    @PostMapping(value = "/jobs/{jobId}/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(
            summary = "Apply to Job with JSON (Candidate)",
            description = "Submit an application using candidate profile and already uploaded resume."
    )
    public ResponseEntity<ApiResponse<JobApplicationResponse>> applyToJobJson(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID jobId,
            @Valid @RequestBody(required = false) JobApplicationRequest request) {

        JobApplicationResponse response = jobApplicationService.applyToJob(
                currentUser.getId(),
                jobId,
                request != null ? request : new JobApplicationRequest()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Application submitted successfully with ATS match score calculated"));
    }

    // =========================================================
    // CANDIDATE: VIEW MY APPLICATIONS
    // =========================================================

    @GetMapping("/candidate/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(
            summary = "View My Applications (Candidate)",
            description = "Fetch all applications submitted by the authenticated candidate, with live stage status and ATS scores."
    )
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getMyApplications(
            @CurrentUser CustomUserDetails currentUser) {

        List<JobApplicationResponse> response =
                jobApplicationService.getCandidateApplications(currentUser.getId());

        return ResponseEntity.ok(ApiResponse.success(response, "Retrieved submitted applications"));
    }

    // =========================================================
    // RECRUITER: VIEW APPLICANTS RANKED BY ATS SCORE
    // =========================================================

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(
            summary = "List Job Applicants (Recruiter)",
            description = "Fetch all applicants for a job posting owned by the recruiter, ranked automatically by highest ATS match score."
    )
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> getJobApplications(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID jobId) {

        List<JobApplicationResponse> response =
                jobApplicationService.getJobApplications(currentUser.getId(), jobId);

        return ResponseEntity.ok(ApiResponse.success(response, "Retrieved applicants ranked by ATS match score"));
    }

    // =========================================================
    // RECRUITER: UPDATE CANDIDATE STAGE & FEEDBACK
    // =========================================================

    @PatchMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(
            summary = "Update Application Stage (Recruiter)",
            description = "Move applicant through recruitment stages (SCREENING, SHORTLISTED, INTERVIEW_SCHEDULED, OFFERED, REJECTED) with optional feedback notes."
    )
    public ResponseEntity<ApiResponse<JobApplicationResponse>> updateApplicationStatus(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {

        JobApplicationResponse response = jobApplicationService.updateApplicationStatus(
                currentUser.getId(),
                applicationId,
                request
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Application stage updated successfully"));
    }

    // =========================================================
    // SHARED: GET APPLICATION BY ID
    // =========================================================

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    @Operation(
            summary = "Get Application Details",
            description = "Retrieve details and ATS breakdown for a specific job application."
    )
    public ResponseEntity<ApiResponse<JobApplicationResponse>> getApplicationById(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID applicationId) {

        JobApplicationResponse response = jobApplicationService.getApplicationById(
                currentUser.getId(),
                applicationId
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Application details retrieved"));
    }

    // =========================================================
    // RECRUITER / CANDIDATE: DOWNLOAD APPLICATION RESUME
    // =========================================================

    @GetMapping("/applications/{applicationId}/resume/download")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER', 'ADMIN')")
    @Operation(
            summary = "Download Applicant Resume by Application ID",
            description = "Allows recruiters (or applicant) to download the resume attached to a specific job application."
    )
    public ResponseEntity<org.springframework.core.io.Resource> downloadApplicationResume(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID applicationId) {

        org.springframework.core.io.Resource resource = jobApplicationService.downloadApplicationResume(
                currentUser.getId(),
                applicationId
        );

        return prepareDownloadResponse(resource);
    }

    private ResponseEntity<org.springframework.core.io.Resource> prepareDownloadResponse(org.springframework.core.io.Resource resource) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(resource.getURI());
            String contentType = java.nio.file.Files.probeContentType(path);
            if (contentType != null) {
                mediaType = MediaType.parseMediaType(contentType);
            }
        } catch (Exception ex) {
            String filename = resource.getFilename();
            if (filename != null) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".pdf")) {
                    mediaType = MediaType.APPLICATION_PDF;
                } else if (lower.endsWith(".docx")) {
                    mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                }
            }
        }

        org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition.inline()
                .filename(resource.getFilename() != null ? resource.getFilename() : "resume")
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }
}
