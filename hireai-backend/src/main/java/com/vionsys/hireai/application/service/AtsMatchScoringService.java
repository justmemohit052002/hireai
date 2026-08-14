package com.vionsys.hireai.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.llm.LlmAtsResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtsMatchScoringService {

    private final LlmAtsClient llmAtsClient;

    /**
     * Compute ATS match score (0-100) between a Candidate profile and a Job posting.
     * Uses LLM microservice if enabled/available, with graceful rule-based fallback.
     */
    public AtsMatchResult computeAtsScore(Candidate candidate, Job job) {

        // 1. Try external LLM scoring first
        Optional<LlmAtsResponse> llmResponse = llmAtsClient.scoreWithLlm(candidate, job);
        if (llmResponse.isPresent()) {
            LlmAtsResponse res = llmResponse.get();
            return AtsMatchResult.builder()
                    .matchScore(Math.min(100, Math.max(0, res.getAtsScore())))
                    .matchingSkills(res.getMatchingSkills() != null ? res.getMatchingSkills() : Collections.emptyList())
                    .missingSkills(res.getMissingSkills() != null ? res.getMissingSkills() : Collections.emptyList())
                    .skillMatchPercentage((double) res.getAtsScore())
                    .experienceMatchPercentage((double) res.getAtsScore())
                    .build();
        }

        // 2. Rule-Based Intelligent Matching Engine (Fallback)
        List<String> jobSkills = job.getSkills() != null
                ? job.getSkills().stream().map(String::trim).filter(s -> !s.isBlank()).toList()
                : Collections.emptyList();

        Set<String> candidateSkillNames = new HashSet<>();
        if (candidate.getSkills() != null) {
            for (Skill s : candidate.getSkills()) {
                if (s.getName() != null) {
                    candidateSkillNames.add(s.getName().trim().toLowerCase());
                }
            }
        }

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        if (jobSkills.isEmpty()) {
            // If no explicit skills required on job, default to candidate skills
            matchedSkills.addAll(candidateSkillNames);
        } else {
            for (String required : jobSkills) {
                if (candidateSkillNames.contains(required.toLowerCase())) {
                    matchedSkills.add(required);
                } else {
                    missingSkills.add(required);
                }
            }
        }

        double skillScore = 60.0;
        if (!jobSkills.isEmpty()) {
            double skillRatio = (double) matchedSkills.size() / jobSkills.size();
            skillScore = skillRatio * 60.0;
        }

        // 2. Experience Level Matching (Weight: 25%)
        double expScore = calculateExperienceScore(candidate.getExperience(), job.getExperienceLevel());

        // 3. Designation / Title Relevance (Weight: 15%)
        double titleScore = calculateTitleScore(candidate.getCurrentDesignation(), job.getTitle());

        int finalScore = (int) Math.min(100, Math.round(skillScore + expScore + titleScore));

        return AtsMatchResult.builder()
                .matchScore(finalScore)
                .matchingSkills(matchedSkills)
                .missingSkills(missingSkills)
                .skillMatchPercentage(jobSkills.isEmpty() ? 100.0 : ((double) matchedSkills.size() / jobSkills.size()) * 100.0)
                .experienceMatchPercentage((expScore / 25.0) * 100.0)
                .build();
    }

    private double calculateExperienceScore(BigDecimal candidateExp, ExperienceLevel requiredLevel) {
        if (candidateExp == null || requiredLevel == null) {
            return 15.0; // Moderate default
        }

        double exp = candidateExp.doubleValue();
        double minExpected = switch (requiredLevel) {
            case FRESHER -> 0.0;
            case JUNIOR -> 1.0;
            case MID_LEVEL -> 3.0;
            case SENIOR -> 5.0;
            case LEAD -> 8.0;
        };

        if (exp >= minExpected) {
            return 25.0;
        } else if (exp >= minExpected - 1.0) {
            return 18.0;
        } else {
            return Math.max(5.0, (exp / Math.max(1.0, minExpected)) * 25.0);
        }
    }

    private double calculateTitleScore(String candidateDesignation, String jobTitle) {
        if (candidateDesignation == null || jobTitle == null) {
            return 10.0;
        }

        String candLower = candidateDesignation.toLowerCase();
        String jobLower = jobTitle.toLowerCase();

        String[] jobWords = jobLower.split("\\s+");
        int matchedWords = 0;
        for (String word : jobWords) {
            if (word.length() > 2 && candLower.contains(word)) {
                matchedWords++;
            }
        }

        if (matchedWords > 0) {
            return 15.0;
        }
        return 7.0;
    }
}
