package com.vionsys.hireai.application.mapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.entity.JobApplication;

public final class JobApplicationMapper {

    private JobApplicationMapper() {
    }

    public static JobApplicationResponse toResponse(JobApplication app) {
        if (app == null) {
            return null;
        }

        List<String> matchingSkills = app.getMatchingSkills() != null && !app.getMatchingSkills().isBlank()
                ? Arrays.stream(app.getMatchingSkills().split(",")).map(String::trim).toList()
                : Collections.emptyList();

        List<String> missingSkills = app.getMissingSkills() != null && !app.getMissingSkills().isBlank()
                ? Arrays.stream(app.getMissingSkills().split(",")).map(String::trim).toList()
                : Collections.emptyList();

        return JobApplicationResponse.builder()
                .id(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .companyName(app.getJob().getRecruiterProfile() != null ? app.getJob().getRecruiterProfile().getCompanyName() : null)
                .candidateId(app.getCandidate().getId())
                .candidateBusinessId(app.getCandidate().getCandidateId())
                .candidateName(app.getCandidate().getFirstName() + " " + app.getCandidate().getLastName())
                .candidateEmail(app.getCandidate().getEmail())
                .candidatePhone(app.getCandidate().getPhone())
                .status(app.getStatus())
                .atsMatchScore(app.getAtsMatchScore())
                .matchingSkills(matchingSkills)
                .missingSkills(missingSkills)
                .coverNote(app.getCoverNote())
                .recruiterNotes(app.getRecruiterNotes())
                .appliedAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
