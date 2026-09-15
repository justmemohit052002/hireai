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

            AiMatchScoreRequest request = AiMatchScoreRequest.builder()
                    .resumeSkills(candidateSkills)
                    .jobSkills(jobSkills)
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
                        .recommendation(res.getAutoAction())
                        .build();

                return Optional.of(response);
            }

        } catch (Exception ex) {
            log.warn("AI Match Engine unavailable ({}). Using rule-based fallback.", ex.getMessage());
        }

        return Optional.empty();
    }
}
