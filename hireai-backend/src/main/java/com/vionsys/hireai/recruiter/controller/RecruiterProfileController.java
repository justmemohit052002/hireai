package com.vionsys.hireai.recruiter.controller;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileRequest;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileResponse;
import com.vionsys.hireai.recruiter.service.RecruiterProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/recruiter/profile")
@RequiredArgsConstructor
@Tag(name = "Recruiter Profile Management", description = "Endpoints for managing recruiter company profile, branding, and profile photo")
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;
    private final ProfilePhotoStorageService photoStorageService;

    @Operation(summary = "Create Recruiter Profile", description = "Create initial employer profile with company details")
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfileResponse> createRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request) {

        RecruiterProfileResponse response =
                recruiterProfileService.createRecruiterProfile(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Current Recruiter Profile", description = "Retrieve company profile for the authenticated recruiter")
    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfileResponse> getCurrentRecruiterProfile() {

        RecruiterProfileResponse response =
                recruiterProfileService.getCurrentRecruiterProfile();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Recruiter Profile by User ID", description = "Retrieve employer profile for a specific user ID")
    @GetMapping("/{userId}")
    public ResponseEntity<RecruiterProfileResponse> getRecruiterProfileByUserId(
            @PathVariable UUID userId) {

        RecruiterProfileResponse response =
                recruiterProfileService.getRecruiterProfileByUserId(userId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Recruiter Profile", description = "Update company description, website, size, address, and industry")
    @PutMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfileResponse> updateRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request) {

        RecruiterProfileResponse response =
                recruiterProfileService.updateRecruiterProfile(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // RECRUITER PROFILE PHOTO ENDPOINTS
    // =========================================================

    @Operation(summary = "Upload Recruiter Profile Photo", description = "Upload a company logo or profile photo (.png, .jpg, .jpeg)")
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfileResponse> uploadProfilePhoto(
            @Parameter(description = "Image file (.png, .jpg, .jpeg)", required = true)
            @RequestParam("file") MultipartFile file) {

        RecruiterProfileResponse response = recruiterProfileService.uploadProfilePhoto(file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Current Recruiter Profile Photo Binary", description = "View/download the authenticated recruiter's profile image")
    @GetMapping("/photo")
    public ResponseEntity<Resource> getCurrentProfilePhoto() {
        Resource resource = recruiterProfileService.getCurrentProfilePhoto();
        String photoPath = recruiterProfileService.getCurrentPhotoPath();
        MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @Operation(summary = "Get Recruiter Profile Photo by User ID", description = "View/download the profile photo of a recruiter by user ID")
    @GetMapping("/{userId}/photo")
    public ResponseEntity<Resource> getProfilePhotoByUserId(
            @PathVariable UUID userId) {
        Resource resource = recruiterProfileService.getProfilePhotoByUserId(userId);
        String photoPath = recruiterProfileService.getPhotoPathByUserId(userId);
        MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @Operation(summary = "Delete Recruiter Profile Photo", description = "Remove the current recruiter profile photo")
    @DeleteMapping("/photo")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<RecruiterProfileResponse> deleteProfilePhoto() {
        RecruiterProfileResponse response = recruiterProfileService.deleteProfilePhoto();
        return ResponseEntity.ok(response);
    }

}