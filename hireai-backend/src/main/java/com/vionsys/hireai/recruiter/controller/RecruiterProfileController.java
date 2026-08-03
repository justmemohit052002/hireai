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

}