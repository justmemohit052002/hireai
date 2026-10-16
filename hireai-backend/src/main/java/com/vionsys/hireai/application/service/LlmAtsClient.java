package com.vionsys.hireai.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreRequest;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreResponse;
import com.vionsys.hireai.application.config.AtsProperties;
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
public class LlmAtsClient {

    private final AtsProperties atsProperties;
    private final AiEngineClient aiEngineClient;

    /**
     * Call external AI Match Engine microservice to compute ATS match score.
     * Returns Optional.empty() if service is disabled, down, or times out.
     */
    public Optional<LlmAtsResponse> scoreWithLlm(Candidate candidate, Job job) {

        if (!atsProperties.getLlm().isEnabled()) {
            return Optional.empty();
        }

        try {
            List<String> candidateSkills = candidate.getSkills() != null
                    ? candidate.getSkills().stream().map(Skill::getName).filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            List<String> jobSkills = job.getSkills() != null
                    ? job.getSkills().stream().filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            List<String> preferredSkills = job.getPreferredSkills() != null
                    ? job.getPreferredSkills().stream().filter(s -> s != null && !s.isBlank()).toList()
                    : Collections.emptyList();

            Double candExpYears = candidate.getExperience() != null
                    ? candidate.getExperience().doubleValue()
                    : null;

            Double reqExpYears = job.getExperienceLevel() != null
                    ? getMinExpectedExperience(job.getExperienceLevel())
                    : null;

            String resumeText = candidate.getResume() != null ? candidate.getResume().getRawText() : null;

            AiMatchScoreRequest request = AiMatchScoreRequest.builder()
                    .resumeSkills(candidateSkills)
                    .jobSkills(jobSkills)
                    .niceToHaveSkills(preferredSkills)
                    .resumeText(resumeText)
                    .shortlistThreshold((double) atsProperties.getShortlistThreshold())
                    .candidateExperienceYears(candExpYears)
                    .requiredExperienceYears(reqExpYears)
                    .candidateRole(candidate.getCurrentDesignation())
                    .jobTitle(job.getTitle())
                    .build();

            Optional<AiMatchScoreResponse> aiResponse = aiEngineClient.calculateMatchScore(request);

            if (aiResponse.isPresent()) {
                AiMatchScoreResponse res = aiResponse.get();
                log.info("AI Match Engine computed score: {}% (Action: {}) for candidate {}",
                        res.getMatchScore(), res.getAutoAction(), candidate.getCandidateId());

                LlmAtsResponse response = LlmAtsResponse.builder()
                        .atsScore(res.getMatchScore())
                        .matchingSkills(res.getMatchedSkills() != null ? res.getMatchedSkills() : Collections.emptyList())
                        .missingSkills(res.getMissingSkills() != null ? res.getMissingSkills() : Collections.emptyList())
                        .relatedSkills(res.getRelatedSkills() != null ? res.getRelatedSkills() : Collections.emptyList())
                        .bonusSkills(res.getBonusSkills() != null ? res.getBonusSkills() : Collections.emptyList())
                        .skillCoveragePercent(res.getSkillCoveragePercent())
                        .experienceMatchPercent(res.getExperienceMatchPercent())
                        .rationale(res.getRationale())
                        .recommendation(res.getAutoAction())
                        .recommended("shortlist".equalsIgnoreCase(res.getAutoAction()))
                        .build();

                return Optional.of(response);
            }

        } catch (Exception ex) {
            log.warn("AI Match Engine unavailable ({}). Using rule-based fallback.", ex.getMessage());
        }

        return Optional.empty();
    }

    private double getMinExpectedExperience(ExperienceLevel level) {
        if (level == null) return 0.0;
        return switch (level) {
            case FRESHER -> 0.0;
            case JUNIOR -> 1.0;
            case MID_LEVEL -> 3.0;
            case SENIOR -> 5.0;
            case LEAD -> 8.0;
            case EXECUTIVE -> 10.0;
        };
    }
}
