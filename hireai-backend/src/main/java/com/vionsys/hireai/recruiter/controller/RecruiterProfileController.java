package com.vionsys.hireai.recruiter.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vionsys.hireai.recruiter.dto.RecruiterProfileRequest;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileResponse;
import com.vionsys.hireai.recruiter.service.RecruiterProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/recruiter/profile")
@RequiredArgsConstructor
public class RecruiterProfileController {

    private final RecruiterProfileService recruiterProfileService;
    private final com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService photoStorageService;

    @PostMapping
    public ResponseEntity<RecruiterProfileResponse> createRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request) {

        RecruiterProfileResponse response =
                recruiterProfileService.createRecruiterProfile(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<RecruiterProfileResponse> getCurrentRecruiterProfile() {

        RecruiterProfileResponse response =
                recruiterProfileService.getCurrentRecruiterProfile();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<RecruiterProfileResponse> getRecruiterProfileByUserId(
            @PathVariable UUID userId) {

        RecruiterProfileResponse response =
                recruiterProfileService.getRecruiterProfileByUserId(userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<RecruiterProfileResponse> updateRecruiterProfile(
            @Valid @RequestBody RecruiterProfileRequest request) {

        RecruiterProfileResponse response =
                recruiterProfileService.updateRecruiterProfile(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // RECRUITER PROFILE PHOTO ENDPOINTS
    // =========================================================

    @PostMapping(value = "/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruiterProfileResponse> uploadProfilePhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        RecruiterProfileResponse response = recruiterProfileService.uploadProfilePhoto(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photo")
    public ResponseEntity<org.springframework.core.io.Resource> getCurrentProfilePhoto() {
        org.springframework.core.io.Resource resource = recruiterProfileService.getCurrentProfilePhoto();
        String photoPath = recruiterProfileService.getCurrentPhotoPath();
        org.springframework.http.MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/{userId}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> getProfilePhotoByUserId(
            @PathVariable UUID userId) {
        org.springframework.core.io.Resource resource = recruiterProfileService.getProfilePhotoByUserId(userId);
        String photoPath = recruiterProfileService.getPhotoPathByUserId(userId);
        org.springframework.http.MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @DeleteMapping("/photo")
    public ResponseEntity<RecruiterProfileResponse> deleteProfilePhoto() {
        RecruiterProfileResponse response = recruiterProfileService.deleteProfilePhoto();
        return ResponseEntity.ok(response);
    }

}