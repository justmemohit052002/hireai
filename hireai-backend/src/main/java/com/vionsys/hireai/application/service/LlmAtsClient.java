package com.vionsys.hireai.application.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.vionsys.hireai.application.config.AtsProperties;
import com.vionsys.hireai.application.dto.llm.LlmAtsRequest;
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

    /**
     * Call external LLM microservice to compute ATS match score.
     * Returns Optional.empty() if service is disabled, down, or times out.
     */
    public Optional<LlmAtsResponse> scoreWithLlm(Candidate candidate, Job job) {

        if (!atsProperties.getLlm().isEnabled()) {
            return Optional.empty();
        }

        try {
            int timeoutMs = atsProperties.getLlm().getTimeoutMs();
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
            requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

            RestClient restClient = RestClient.builder()
                    .requestFactory(requestFactory)
                    .baseUrl(atsProperties.getLlm().getServiceUrl())
                    .build();

            List<String> candidateSkills = candidate.getSkills() != null
                    ? candidate.getSkills().stream().map(Skill::getName).toList()
                    : Collections.emptyList();

            LlmAtsRequest payload = LlmAtsRequest.builder()
                    .candidate(LlmAtsRequest.CandidateInfo.builder()
                            .candidateId(candidate.getCandidateId())
                            .name(candidate.getFirstName() + " " + candidate.getLastName())
                            .experienceYears(candidate.getExperience())
                            .currentDesignation(candidate.getCurrentDesignation())
                            .currentCompany(candidate.getCurrentCompany())
                            .skills(candidateSkills)
                            .build())
                    .job(LlmAtsRequest.JobInfo.builder()
                            .jobId(job.getId() != null ? job.getId().toString() : null)
                            .title(job.getTitle())
                            .experienceLevel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null)
                            .requiredSkills(job.getSkills() != null ? job.getSkills() : Collections.emptyList())
                            .description(job.getDescription())
                            .build())
                    .build();

            LlmAtsResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(LlmAtsResponse.class);

            if (response != null) {
                log.info("LLM ATS Service computed score: {}% for candidate {}",
                        response.getAtsScore(), candidate.getCandidateId());
                return Optional.of(response);
            }

        } catch (Exception ex) {
            log.warn("LLM ATS Service unavailable or timed out ({}). Using rule-based fallback.", ex.getMessage());
        }

        return Optional.empty();
    }
}
