package com.vionsys.hireai.application.mapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.entity.JobApplication;

public final class JobApplicationMapper {

    private JobApplicationMapper() {
    }

    public static JobApplicationResponse toResponse(JobApplication app) {
        if (app == null) {
            return null;
        }

        List<String> matchingSkills = splitSkillsSafely(app.getMatchingSkills());
        List<String> missingSkills = splitSkillsSafely(app.getMissingSkills());

        UUID resumeId = app.getCandidate() != null && app.getCandidate().getResume() != null
                ? app.getCandidate().getResume().getId()
                : null;

        String resumeFileName = app.getCandidate() != null && app.getCandidate().getResume() != null
                ? app.getCandidate().getResume().getOriginalFileName()
                : null;

        String resumeDownloadUrl = resumeId != null
                ? "/applications/" + app.getId() + "/resume/download"
                : null;

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
                .candidateProfilePhotoUrl(app.getCandidate().getProfilePhotoUrl())
                .status(app.getStatus())
                .atsMatchScore(app.getAtsMatchScore())
                .matchingSkills(matchingSkills)
                .missingSkills(missingSkills)
                .coverNote(app.getCoverNote())
                .recruiterNotes(app.getRecruiterNotes())
                .interviewScore(app.getInterviewScore())
                .chatbotScore(app.getChatbotScore())
                .finalAiScore(app.getFinalAiScore())
                .aiClassification(app.getAiClassification())
                .aiExplanation(app.getAiExplanation())
                .resumeId(resumeId)
                .resumeFileName(resumeFileName)
                .resumeDownloadUrl(resumeDownloadUrl)
                .appliedAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    private static List<String> splitSkillsSafely(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        // Splits by comma while preserving commas enclosed within parentheses
        String[] tokens = raw.split(",\\s*(?![^()]*\\))");
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
