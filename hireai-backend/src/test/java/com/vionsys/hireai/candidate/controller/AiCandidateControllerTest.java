package com.vionsys.hireai.candidate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsResponse;
import com.vionsys.hireai.ai.service.AiChatbotService;
import com.vionsys.hireai.ai.service.InterviewAiService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(AiCandidateController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiCandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatbotService aiChatbotService;

    @MockBean
    private InterviewAiService interviewAiService;

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
                "candidate@example.com",
                "encodedPassword",
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    void testSendChatMessage() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("message", "I have 4 years of experience and expect 12 LPA");

        AiChatMessageResponse response = AiChatMessageResponse.builder()
                .botReply("Thank you! What is your current notice period?")
                .conversationComplete(false)
                .build();

        when(aiChatbotService.sendMessage(eq(userId), eq("I have 4 years of experience and expect 12 LPA")))
                .thenReturn(response);

        mockMvc.perform(post("/candidate/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.botReply").value("Thank you! What is your current notice period?"));
    }

    @Test
    void testGetChatHistory() throws Exception {
        when(aiChatbotService.getConversationHistory(eq(userId))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/candidate/chat/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetInterviewQuestions() throws Exception {
        UUID applicationId = UUID.randomUUID();

        AiInterviewQuestionsResponse response = AiInterviewQuestionsResponse.builder()
                .questions(List.of(
                        AiInterviewQuestionsResponse.QuestionItem.builder()
                                .questionId("q1")
                                .text("Explain how Spring Boot autoconfiguration works.")
                                .type("open_text")
                                .build()
                ))
                .build();

        when(interviewAiService.getOrGenerateQuestions(eq(userId), eq(applicationId))).thenReturn(response);

        mockMvc.perform(get("/candidate/applications/" + applicationId + "/interview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].questionId").value("q1"))
                .andExpect(jsonPath("$.data.questions[0].text").value("Explain how Spring Boot autoconfiguration works."));
    }

    @Test
    void testSubmitInterviewAnswers() throws Exception {
        UUID applicationId = UUID.randomUUID();

        AiInterviewEvaluateRequest request = AiInterviewEvaluateRequest.builder()
                .candidateId(userId.toString())
                .answers(List.of(
                        AiInterviewEvaluateRequest.AnswerItem.builder()
                                .questionId("q1")
                                .answerText("Spring Boot scans @ConditionalOnClass and @ConditionalOnProperty annotations.")
                                .build()
                ))
                .build();

        AiInterviewEvaluateResponse response = AiInterviewEvaluateResponse.builder()
                .interviewScore(90)
                .evaluatedAnswers(List.of(
                        AiInterviewEvaluateResponse.EvaluatedAnswerItem.builder()
                                .questionId("q1")
                                .score(90)
                                .feedback("Accurate and concise explanation.")
                                .build()
                ))
                .build();

        when(interviewAiService.submitAndEvaluateInterview(eq(userId), eq(applicationId), any())).thenReturn(response);

        mockMvc.perform(post("/candidate/applications/" + applicationId + "/interview/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewScore").value(90))
                .andExpect(jsonPath("$.data.evaluatedAnswers[0].feedback").value("Accurate and concise explanation."));
    }
}
