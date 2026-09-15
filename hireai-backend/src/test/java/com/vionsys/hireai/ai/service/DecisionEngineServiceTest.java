package com.vionsys.hireai.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;
import com.vionsys.hireai.ai.service.impl.DecisionEngineServiceImpl;
import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.candidate.entity.Candidate;

@ExtendWith(MockitoExtension.class)
class DecisionEngineServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private AiEngineClient aiEngineClient;

    @InjectMocks
    private DecisionEngineServiceImpl decisionEngineService;

    private JobApplication application;

    @BeforeEach
    void setUp() {
        Candidate candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .candidateId("CAND-101")
                .firstName("Alex")
                .lastName("Taylor")
                .build();

        application = JobApplication.builder()
                .id(UUID.randomUUID())
                .candidate(candidate)
                .status(ApplicationStatus.APPLIED)
                .atsMatchScore(85)
                .interviewScore(90)
                .chatbotScore(80)
                .build();
    }

    @Test
    void testFinalizeDecision_WithAiEngine() {
        AiDecisionResponse aiResponse = AiDecisionResponse.builder()
                .finalScore(86)
                .classification("shortlist")
                .explanation("Candidate strongly qualified across all metrics.")
                .build();

        when(jobApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiEngineClient.finalizeDecision(any())).thenReturn(Optional.of(aiResponse));
        when(jobApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiDecisionResponse result = decisionEngineService.finalizeApplicationDecision(application.getId());

        assertNotNull(result);
        assertEquals(86, result.getFinalScore());
        assertEquals("shortlist", result.getClassification());
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
    }

    @Test
    void testFinalizeDecision_WithFallbackFormula() {
        when(jobApplicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(aiEngineClient.finalizeDecision(any())).thenReturn(Optional.empty());
        when(jobApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // 85 * 0.4 + 90 * 0.3 + 80 * 0.3 = 34 + 27 + 24 = 85 -> shortlist (>=75)
        AiDecisionResponse result = decisionEngineService.finalizeApplicationDecision(application.getId());

        assertNotNull(result);
        assertEquals(85, result.getFinalScore());
        assertEquals("shortlist", result.getClassification());
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
    }
}
