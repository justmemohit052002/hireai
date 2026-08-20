package com.vionsys.hireai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.llm.LlmAtsResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

@ExtendWith(MockitoExtension.class)
class AtsMatchScoringServiceTest {

    @Mock
    private LlmAtsClient llmAtsClient;

    @InjectMocks
    private AtsMatchScoringService scoringService;

    private Candidate candidate;
    private Job job;

    @BeforeEach
    void setUp() {
        Skill java = Skill.builder().id(UUID.randomUUID()).name("Java").build();
        Skill spring = Skill.builder().id(UUID.randomUUID()).name("Spring Boot").build();
        Skill postgres = Skill.builder().id(UUID.randomUUID()).name("PostgreSQL").build();

        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-001")
                .firstName("Mohit")
                .lastName("Kumar")
                .skills(Set.of(java, spring, postgres))
                .experience(BigDecimal.valueOf(4.0))
                .currentDesignation("Senior Java Developer")
                .build();

        job = Job.builder()
                .id(UUID.randomUUID())
                .title("Senior Java Backend Engineer")
                .skills(List.of("Java", "Spring Boot", "PostgreSQL", "Docker"))
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .build();
    }

    @Test
    void testComputeScore_WithLlmResponse() {
        LlmAtsResponse mockLlm = LlmAtsResponse.builder()
                .atsScore(88)
                .matchingSkills(List.of("Java", "Spring Boot", "PostgreSQL"))
                .missingSkills(List.of("Docker"))
                .recommendation("shortlist")
                .build();

        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.of(mockLlm));

        AtsMatchResult result = scoringService.computeAtsScore(candidate, job);

        assertNotNull(result);
        assertEquals(88, result.getMatchScore());
        assertEquals(3, result.getMatchingSkills().size());
        assertEquals(1, result.getMissingSkills().size());
    }

    @Test
    void testComputeScore_WithRuleBasedFallback() {
        when(llmAtsClient.scoreWithLlm(any(), any())).thenReturn(Optional.empty());

        AtsMatchResult result = scoringService.computeAtsScore(candidate, job);

        assertNotNull(result);
        assertTrue(result.getMatchScore() >= 70, "Rule based score should be high for matching skills and experience");
        assertTrue(result.getMatchingSkills().contains("Java"));
        assertTrue(result.getMissingSkills().contains("Docker"));
    }
}
