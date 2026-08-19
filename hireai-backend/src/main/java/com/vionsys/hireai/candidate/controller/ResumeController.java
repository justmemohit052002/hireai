package com.vionsys.hireai.candidate.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.dto.ApiResponse;
import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Resume Management", description = "Endpoints for uploading, parsing, downloading, and managing candidate resumes")
public class ResumeController {

    private final ResumeService resumeService;

    // =========================================================================
    // CANDIDATE SELF-SERVICE RESUME ENDPOINTS
    // =========================================================================

    @Operation(summary = "Upload candidate's own resume and trigger AI parsing")
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping("/candidate/resume/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadMyResume(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        UUID userId = getUserId(authentication);
        ResumeResponse response = resumeService.uploadMyResume(userId, file);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.<ResumeResponse>builder()
                        .success(true)
                        .message("Resume uploaded successfully. AI parsing has been initiated.")
                        .data(response)
                        .build());
    }

    @Operation(summary = "Get candidate's own resume and AI parsed metadata")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/candidate/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> getMyResume(Authentication authentication) {
        UUID userId = getUserId(authentication);
        ResumeResponse response = resumeService.getMyResume(userId);

        return ResponseEntity.ok(ApiResponse.<ResumeResponse>builder()
                .success(true)
                .message("Resume retrieved successfully")
                .data(response)
                .build());
    }

    @Operation(summary = "Download candidate's own resume binary file")
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/candidate/resume/download")
    public ResponseEntity<Resource> downloadMyResume(Authentication authentication) {
        UUID userId = getUserId(authentication);
        Resource resource = resumeService.downloadMyResume(userId);
        return prepareDownloadResponse(resource);
    }

    @Operation(summary = "Delete candidate's own resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    @DeleteMapping("/candidate/resume")
    public ResponseEntity<Void> deleteMyResume(Authentication authentication) {
        UUID userId = getUserId(authentication);
        resumeService.deleteMyResume(userId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // RECRUITER / ADMIN RESUME ENDPOINTS
    // =========================================================================

    @Operation(summary = "Upload resume for a specific candidate (Recruiter/Admin)")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @PostMapping("/candidates/{candidateId}/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResumeForCandidate(
            @PathVariable UUID candidateId,
            @RequestParam("file") MultipartFile file) {

        ResumeResponse response = resumeService.uploadResume(candidateId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<ResumeResponse>builder()
                        .success(true)
                        .message("Resume uploaded and AI parsing started")
                        .data(response)
                        .build());
    }

    @Operation(summary = "Get resume details by candidate ID (Recruiter/Admin)")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/candidates/{candidateId}/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeByCandidateId(
            @PathVariable UUID candidateId) {

        ResumeResponse response = resumeService.getResume(candidateId);

        return ResponseEntity.ok(ApiResponse.<ResumeResponse>builder()
                .success(true)
                .message("Resume retrieved successfully")
                .data(response)
                .build());
    }

    @Operation(summary = "Get resume parsing status by candidate ID (Recruiter/Admin)")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/candidates/{candidateId}/resume/status")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeStatus(
            @PathVariable UUID candidateId) {

        ResumeResponse response = resumeService.getResumeStatus(candidateId);

        return ResponseEntity.ok(ApiResponse.<ResumeResponse>builder()
                .success(true)
                .message("Resume parsing status retrieved")
                .data(response)
                .build());
    }

    @Operation(summary = "Download resume file by candidate ID (Recruiter/Admin)")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @GetMapping("/candidates/{candidateId}/resume/download")
    public ResponseEntity<Resource> downloadResumeByCandidateId(
            @PathVariable UUID candidateId) {

        Resource resource = resumeService.downloadResume(candidateId);
        return prepareDownloadResponse(resource);
    }

    @Operation(summary = "Delete resume by candidate ID (Recruiter/Admin)")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @DeleteMapping("/candidates/{candidateId}/resume")
    public ResponseEntity<Void> deleteResumeByCandidateId(
            @PathVariable UUID candidateId) {

        resumeService.deleteResume(candidateId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> prepareDownloadResponse(Resource resource) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            Path path = Paths.get(resource.getURI());
            String contentType = Files.probeContentType(path);
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

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(resource.getFilename() != null ? resource.getFilename() : "resume")
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
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
