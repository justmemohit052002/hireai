package com.vionsys.hireai.candidate.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.chatbot.AiChatTurn;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsResponse;
import com.vionsys.hireai.ai.service.AiChatbotService;
import com.vionsys.hireai.ai.service.InterviewAiService;
import com.vionsys.hireai.candidate.dto.ApiResponse;
import com.vionsys.hireai.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/candidate")
@RequiredArgsConstructor
@Tag(name = "Candidate AI Features", description = "AI Pre-Screening Chatbot and Interview Assessment for Candidates")
@PreAuthorize("hasRole('CANDIDATE')")
public class AiCandidateController {

    private final AiChatbotService chatbotService;
    private final InterviewAiService interviewAiService;

    // =========================================================================
    // AI CHATBOT PRE-SCREENING ENDPOINTS
    // =========================================================================

    @Operation(summary = "Send a message to the AI Pre-Screening Chatbot")
    @PostMapping("/chat/message")
    public ResponseEntity<ApiResponse<AiChatMessageResponse>> sendChatMessage(
            Authentication authentication,
            @RequestBody Map<String, String> payload) {

        UUID userId = getUserId(authentication);
        String message = payload != null ? payload.getOrDefault("message", "") : "";

        AiChatMessageResponse response = chatbotService.sendMessage(userId, message);

        return ResponseEntity.ok(ApiResponse.<AiChatMessageResponse>builder()
                .success(true)
                .message("Chatbot responded successfully")
                .data(response)
                .build());
    }

    @Operation(summary = "Get full chat conversation history with AI Screening Bot")
    @GetMapping("/chat/history")
    public ResponseEntity<ApiResponse<List<AiChatTurn>>> getChatHistory(Authentication authentication) {
        UUID userId = getUserId(authentication);
        List<AiChatTurn> history = chatbotService.getConversationHistory(userId);

        return ResponseEntity.ok(ApiResponse.<List<AiChatTurn>>builder()
                .success(true)
                .message("Conversation history retrieved")
                .data(history)
                .build());
    }

    @Operation(summary = "Reset or complete candidate AI chat session")
    @PostMapping("/chat/reset")
    public ResponseEntity<ApiResponse<Void>> resetChat(Authentication authentication) {
        UUID userId = getUserId(authentication);
        chatbotService.resetConversation(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Conversation session reset successfully")
                .build());
    }

    // =========================================================================
    // INTERVIEW AI ASSESSMENT ENDPOINTS
    // =========================================================================

    @Operation(summary = "Fetch or generate AI screening interview questions for a job application")
    @GetMapping("/applications/{applicationId}/interview")
    public ResponseEntity<ApiResponse<AiInterviewQuestionsResponse>> getInterviewQuestions(
            Authentication authentication,
            @PathVariable UUID applicationId) {

        UUID userId = getUserId(authentication);
        AiInterviewQuestionsResponse response = interviewAiService.getOrGenerateQuestions(userId, applicationId);

        return ResponseEntity.ok(ApiResponse.<AiInterviewQuestionsResponse>builder()
                .success(true)
                .message("Interview questions retrieved successfully")
                .data(response)
                .build());
    }

    @Operation(summary = "Submit interview answers for AI grading and evaluation")
    @PostMapping("/applications/{applicationId}/interview/submit")
    public ResponseEntity<ApiResponse<AiInterviewEvaluateResponse>> submitInterviewAnswers(
            Authentication authentication,
            @PathVariable UUID applicationId,
            @RequestBody AiInterviewEvaluateRequest request) {

        UUID userId = getUserId(authentication);
        AiInterviewEvaluateResponse response = interviewAiService.submitAndEvaluateInterview(userId, applicationId, request);

        return ResponseEntity.ok(ApiResponse.<AiInterviewEvaluateResponse>builder()
                .success(true)
                .message("Interview answers submitted and evaluated by AI Engine")
                .data(response)
                .build());
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            return user.getId();
        }
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            return user.getId();
        }
        throw new IllegalStateException("Authenticated user not found");
    }
}
