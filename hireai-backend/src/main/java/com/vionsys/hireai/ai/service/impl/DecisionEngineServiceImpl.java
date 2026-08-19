package com.vionsys.hireai.ai.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.decision.AiDecisionRequest;
import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;
import com.vionsys.hireai.ai.service.DecisionEngineService;
import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.exception.ApplicationNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DecisionEngineServiceImpl implements DecisionEngineService {

    private final JobApplicationRepository jobApplicationRepository;
    private final AiEngineClient aiEngineClient;

    @Override
    public AiDecisionResponse finalizeApplicationDecision(UUID applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found with id: " + applicationId));

        return computeAndPersistDecision(application);
    }

    @Override
    public AiDecisionResponse finalizeApplicationDecisionForRecruiter(UUID recruiterUserId, UUID applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found with id: " + applicationId));

        if (application.getJob().getRecruiterProfile() == null ||
                application.getJob().getRecruiterProfile().getUser() == null ||
                !application.getJob().getRecruiterProfile().getUser().getId().equals(recruiterUserId)) {
            throw new AccessDeniedException("You do not have permission to finalize decision on this application.");
        }

        return computeAndPersistDecision(application);
    }

    private AiDecisionResponse computeAndPersistDecision(JobApplication application) {
        double resumeScore = application.getAtsMatchScore() != null ? application.getAtsMatchScore().doubleValue() : 70.0;
        double interviewScore = application.getInterviewScore() != null ? application.getInterviewScore().doubleValue() : 75.0;

        double chatbotScore = application.getChatbotScore() != null
                ? application.getChatbotScore().doubleValue()
                : computeChatbotSignal(application);

        AiDecisionRequest request = AiDecisionRequest.builder()
                .resumeScore(resumeScore)
                .interviewScore(interviewScore)
                .chatbotSignalScore(chatbotScore)
                .build();

        AiDecisionResponse response = aiEngineClient.finalizeDecision(request)
                .orElseGet(() -> fallbackDecision(resumeScore, interviewScore, chatbotScore));

        // Update JobApplication
        application.setChatbotScore((int) Math.round(chatbotScore));
        application.setFinalAiScore(response.getFinalScore());
        application.setAiClassification(response.getClassification());
        application.setAiExplanation(response.getExplanation());

        // Update application status based on AI classification
        if ("shortlist".equalsIgnoreCase(response.getClassification())) {
            application.setStatus(ApplicationStatus.SHORTLISTED);
        } else if ("reject".equalsIgnoreCase(response.getClassification())) {
            application.setStatus(ApplicationStatus.REJECTED);
        } else if ("hold".equalsIgnoreCase(response.getClassification())) {
            if (application.getStatus() == ApplicationStatus.APPLIED) {
                application.setStatus(ApplicationStatus.SCREENING);
            }
        }

        jobApplicationRepository.save(application);
        log.info("Finalized Decision for Application {}: Score={}, Classification={}",
                application.getId(), response.getFinalScore(), response.getClassification());

        return response;
    }

    private double computeChatbotSignal(JobApplication application) {
        if (application.getCandidate() == null) {
            return 70.0;
        }
        double score = 50.0;
        if (application.getCandidate().getExpectedCtc() != null) score += 15.0;
        if (application.getCandidate().getNoticePeriod() != null) score += 15.0;
        if (application.getCandidate().getExperience() != null) score += 10.0;
        if (application.getCandidate().getSkills() != null && !application.getCandidate().getSkills().isEmpty()) score += 10.0;
        return Math.min(100.0, score);
    }

    private AiDecisionResponse fallbackDecision(double resumeScore, double interviewScore, double chatbotScore) {
        int finalScore = (int) Math.round(resumeScore * 0.4 + interviewScore * 0.3 + chatbotScore * 0.3);
        String classification;
        String explanation;

        if (finalScore >= 75) {
            classification = "shortlist";
            explanation = "Candidate passed benchmark criteria across resume, interview, and screening.";
        } else if (finalScore < 40) {
            classification = "reject";
            explanation = "Candidate score is below the minimum qualification threshold.";
        } else {
            classification = "hold";
            explanation = "Candidate composite score requires manual recruiter review.";
        }

        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("resumeScore", resumeScore);
        breakdown.put("resumeWeight", 0.4);
        breakdown.put("interviewScore", interviewScore);
        breakdown.put("interviewWeight", 0.3);
        breakdown.put("chatbotSignalScore", chatbotScore);
        breakdown.put("chatbotWeight", 0.3);

        return AiDecisionResponse.builder()
                .finalScore(finalScore)
                .classification(classification)
                .breakdown(breakdown)
                .explanation(explanation)
                .build();
    }
}
