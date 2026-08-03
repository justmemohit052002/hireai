package com.vionsys.hireai.recruiter.mapper;

import com.vionsys.hireai.recruiter.dto.RecruiterProfileRequest;
import com.vionsys.hireai.recruiter.dto.RecruiterProfileResponse;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;

public final class RecruiterProfileMapper {

    private RecruiterProfileMapper() {
    }

    public static RecruiterProfile toEntity(RecruiterProfileRequest request) {

        if (request == null) {
            return null;
        }

        return RecruiterProfile.builder()
                .companyName(request.getCompanyName())
                .designation(request.getDesignation())
                .companyWebsite(request.getCompanyWebsite())
                .companyEmail(request.getCompanyEmail())
                .companyPhone(request.getCompanyPhone())
                .companyLogoUrl(request.getCompanyLogoUrl())
                .companyDescription(request.getCompanyDescription())
                .industry(request.getIndustry())
                .companySize(request.getCompanySize())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .address(request.getAddress())
                .build();
    }

    public static RecruiterProfileResponse toResponse(
            RecruiterProfile recruiterProfile) {

        if (recruiterProfile == null) {
            return null;
        }

        return RecruiterProfileResponse.builder()
                .id(recruiterProfile.getId())
                .userId(recruiterProfile.getUser().getId())
                .companyName(recruiterProfile.getCompanyName())
                .designation(recruiterProfile.getDesignation())
                .companyWebsite(recruiterProfile.getCompanyWebsite())
                .companyEmail(recruiterProfile.getCompanyEmail())
                .companyPhone(recruiterProfile.getCompanyPhone())
                .companyLogoUrl(recruiterProfile.getCompanyLogoUrl())
                .companyDescription(recruiterProfile.getCompanyDescription())
                .industry(recruiterProfile.getIndustry())
                .companySize(recruiterProfile.getCompanySize())
                .country(recruiterProfile.getCountry())
                .state(recruiterProfile.getState())
                .city(recruiterProfile.getCity())
                .address(recruiterProfile.getAddress())
                .verified(recruiterProfile.getVerified())
                .build();
    }

}