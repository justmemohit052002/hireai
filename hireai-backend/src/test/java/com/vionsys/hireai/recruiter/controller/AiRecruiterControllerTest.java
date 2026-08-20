package com.vionsys.hireai.recruiter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateRequest;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateResponse;
import com.vionsys.hireai.ai.service.AiJdService;
import com.vionsys.hireai.ai.service.DecisionEngineService;
import com.vionsys.hireai.application.service.AtsMatchScoringService;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(AiRecruiterController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiRecruiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiJdService aiJdService;

    @MockBean
    private AtsMatchScoringService atsMatchScoringService;

    @MockBean
    private DecisionEngineService decisionEngineService;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private CandidateRepository candidateRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "recruiter@vionsys.com",
                "encodedPassword",
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    void testGenerateJobDescription() throws Exception {
        AiJdGenerateRequest request = AiJdGenerateRequest.builder()
                .jobTitle("Fullstack Developer")
                .requiredSkills(List.of("Java", "React", "Docker"))
                .experienceLevel("MID_LEVEL")
                .build();

        AiJdGenerateResponse response = AiJdGenerateResponse.builder()
                .description("Seeking a skilled Fullstack Developer...")
                .responsibilities(List.of("Develop frontend components", "Design backend microservices"))
                .interviewQuestions(List.of("Describe a scalable API you built."))
                .mustHaveSkills(List.of("Java", "React"))
                .build();

        when(aiJdService.generateJobDescription(any())).thenReturn(response);

        mockMvc.perform(post("/recruiter/ai/jd/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Seeking a skilled Fullstack Developer..."))
                .andExpect(jsonPath("$.data.responsibilities[0]").value("Develop frontend components"));
    }

    @Test
    void testFinalizeApplicationDecision() throws Exception {
        UUID applicationId = UUID.randomUUID();

        AiDecisionResponse decisionResponse = AiDecisionResponse.builder()
                .finalScore(88)
                .classification("shortlist")
                .explanation("Top candidate matching all technical requirements.")
                .build();

        when(decisionEngineService.finalizeApplicationDecisionForRecruiter(eq(userId), eq(applicationId))).thenReturn(decisionResponse);

        mockMvc.perform(post("/recruiter/ai/applications/" + applicationId + "/decision"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.finalScore").value(88))
                .andExpect(jsonPath("$.data.classification").value("shortlist"));
    }
}
