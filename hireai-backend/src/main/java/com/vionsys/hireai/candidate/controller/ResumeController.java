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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.dto.ApiResponse;
import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.service.ResumeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class ResumeController {

	private final ResumeService resumeService;

    @PostMapping("/{candidateId}/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @PathVariable UUID candidateId,
            @RequestParam("file") MultipartFile file) {

        ResumeResponse response =
                resumeService.uploadResume(candidateId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.<ResumeResponse>builder()
                                .success(true)
                                .message("Resume uploaded successfully")
                                .data(response)
                                .build()
                );
    }
    
    @GetMapping("/{candidateId}/resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(
            @PathVariable UUID candidateId) {

        ResumeResponse response =
                resumeService.getResume(candidateId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.<ResumeResponse>builder()
                                .success(true)
                                .message("Resume retrieved successfully")
                                .data(response)
                                .build()
                );
    }
    
    @GetMapping("/{candidateId}/resume/download")
    public ResponseEntity<Resource> downloadResume(
            @PathVariable UUID candidateId) {

        Resource resource = resumeService.downloadResume(candidateId);

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

                String lowerCaseFilename = filename.toLowerCase();

                if (lowerCaseFilename.endsWith(".pdf")) {
                    mediaType = MediaType.APPLICATION_PDF;

                } else if (lowerCaseFilename.endsWith(".docx")) {
                    mediaType = MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    );
                }
            }
        }

        ContentDisposition contentDisposition =
                ContentDisposition.inline()
                        .filename(resource.getFilename())
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(resource);
    }
    
    @DeleteMapping("/{candidateId}/resume")
    public ResponseEntity<Void> deleteResume(
            @PathVariable UUID candidateId) {

        resumeService.deleteResume(candidateId);

        return ResponseEntity.noContent().build();
    }
}
