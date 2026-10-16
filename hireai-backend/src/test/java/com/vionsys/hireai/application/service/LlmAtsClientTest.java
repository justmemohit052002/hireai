package com.vionsys.hireai.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreRequest;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreResponse;
import com.vionsys.hireai.application.config.AtsProperties;
import com.vionsys.hireai.application.dto.llm.LlmAtsResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.ExperienceLevel;

@ExtendWith(MockitoExtension.class)
class LlmAtsClientTest {

    @Mock
    private AtsProperties atsProperties;

    @Mock
    private AiEngineClient aiEngineClient;

    @InjectMocks
    private LlmAtsClient llmAtsClient;

    private Candidate candidate;
    private Job job;
    private AtsProperties.LlmProperties llmProps;

    @BeforeEach
    void setUp() {
        llmProps = new AtsProperties.LlmProperties();
        llmProps.setEnabled(true);

        candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-999")
                .firstName("Mohit")
                .lastName("Chouhan")
                .experience(BigDecimal.valueOf(3.5))
                .skills(Set.of(Skill.builder().name("Java").build(), Skill.builder().name("AWS").build()))
                .build();

        job = Job.builder()
                .id(UUID.randomUUID())
                .title("Java Cloud Engineer")
                .skills(List.of("Java", "AWS", "MongoDB"))
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .build();
    }

    @Test
    void testScoreWithLlm_WhenDisabled_ReturnsEmpty() {
        llmProps.setEnabled(false);
        when(atsProperties.getLlm()).thenReturn(llmProps);

        Optional<LlmAtsResponse> result = llmAtsClient.scoreWithLlm(candidate, job);

        assertTrue(result.isEmpty());
    }

    @Test
    void testScoreWithLlm_WhenEnabled_MapsAllV2Fields() {
        when(atsProperties.getLlm()).thenReturn(llmProps);

        AiMatchScoreResponse aiResponse = AiMatchScoreResponse.builder()
                .matchScore(88)
                .autoAction("shortlist")
                .matchedSkills(List.of("Java", "AWS"))
                .missingSkills(List.of("MongoDB"))
                .bonusSkills(List.of("Spring Boot"))
                .skillCoveragePercent(87.5)
                .experienceMatchPercent(100.0)
                .rationale("Matched 2 of 3 required skills.")
                .build();

        when(aiEngineClient.calculateMatchScore(any(AiMatchScoreRequest.class))).thenReturn(Optional.of(aiResponse));

        Optional<LlmAtsResponse> result = llmAtsClient.scoreWithLlm(candidate, job);

        assertTrue(result.isPresent());
        LlmAtsResponse res = result.get();
        assertEquals(88, res.getAtsScore());
        assertEquals("shortlist", res.getRecommendation());
        assertTrue(res.isRecommended());
        assertEquals(List.of("Java", "AWS"), res.getMatchingSkills());
        assertEquals(List.of("MongoDB"), res.getMissingSkills());
        assertEquals(List.of("Spring Boot"), res.getBonusSkills());
        assertEquals(87.5, res.getSkillCoveragePercent());
        assertEquals(100.0, res.getExperienceMatchPercent());
        assertEquals("Matched 2 of 3 required skills.", res.getRationale());

        // Verify request payload was constructed with experience
        ArgumentCaptor<AiMatchScoreRequest> captor = ArgumentCaptor.forClass(AiMatchScoreRequest.class);
        verify(aiEngineClient).calculateMatchScore(captor.capture());
        AiMatchScoreRequest req = captor.getValue();
        assertNotNull(req);
        assertEquals(3.5, req.getCandidateExperienceYears());
        assertEquals(3.0, req.getRequiredExperienceYears());
    }
}
