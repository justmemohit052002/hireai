package com.vionsys.hireai.recruiter.service;

import java.util.UUID;

import com.vionsys.hireai.recruiter.dto.RecruiterProfileRequest;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileResponse;

public interface RecruiterProfileService {

    /**
     * Create recruiter profile for the authenticated recruiter.
     */
    RecruiterProfileResponse createRecruiterProfile(
            RecruiterProfileRequest request);

    /**
     * Get logged-in recruiter's profile.
     */
    RecruiterProfileResponse getCurrentRecruiterProfile();

    /**
     * Get recruiter profile by user id.
     */
    RecruiterProfileResponse getRecruiterProfileByUserId(UUID userId);

    /**
     * Update logged-in recruiter's profile.
     */
    RecruiterProfileResponse updateRecruiterProfile(
            RecruiterProfileRequest request);

}