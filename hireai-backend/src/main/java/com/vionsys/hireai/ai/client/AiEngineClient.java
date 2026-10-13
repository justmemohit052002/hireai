package com.vionsys.hireai.ai.client;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.vionsys.hireai.ai.config.AiEngineProperties;
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageRequest;
import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.decision.AiDecisionRequest;
import com.vionsys.hireai.ai.dto.decision.AiDecisionResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewEvaluateResponse;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsRequest;
import com.vionsys.hireai.ai.dto.interview.AiInterviewQuestionsResponse;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateRequest;
import com.vionsys.hireai.ai.dto.jd.AiJdGenerateResponse;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreRequest;
import com.vionsys.hireai.ai.dto.match.AiMatchScoreResponse;
import com.vionsys.hireai.ai.dto.resume.AiJobAcceptedResponse;
import com.vionsys.hireai.ai.dto.resume.AiJobStatusResponse;
import com.vionsys.hireai.ai.dto.resume.AiResumeParseRequest;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Component
public class AiEngineClient {

    private final RestClient restClient;
    private final RestClient adminRestClient;
    private final AiEngineProperties properties;

    public AiEngineClient(AiEngineProperties properties) {
        this.properties = properties != null ? properties : new AiEngineProperties();

        int timeout = this.properties.getTimeoutMs() > 0 ? this.properties.getTimeoutMs() : 120000;
        String baseUrl = this.properties.getBaseUrl() != null && !this.properties.getBaseUrl().isBlank()
                ? this.properties.getBaseUrl()
                : "http://localhost:8000";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        // Dedicated fast RestClient for admin telemetry and monitoring calls
        int adminConnectTimeout = this.properties.getHealthConnectTimeoutMs() > 0 ? this.properties.getHealthConnectTimeoutMs() : 2000;
        int adminReadTimeout = this.properties.getHealthReadTimeoutMs() > 0 ? this.properties.getHealthReadTimeoutMs() : 3000;

        SimpleClientHttpRequestFactory adminFactory = new SimpleClientHttpRequestFactory();
        adminFactory.setConnectTimeout(Duration.ofMillis(adminConnectTimeout));
        adminFactory.setReadTimeout(Duration.ofMillis(adminReadTimeout));

        String internalKey = this.properties.getInternalKey() != null ? this.properties.getInternalKey() : "";
        this.adminRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(adminFactory)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();
    }

    /**
     * Module 1: Job Description Generator
     */
    public Optional<AiJdGenerateResponse> generateJd(AiJdGenerateRequest request) {
        try {
            AiJdGenerateResponse response = restClient.post()
                    .uri("/api/v1/jd/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiJdGenerateResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine generateJd failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 2: Submit Resume for Asynchronous Parsing
     */
    public Optional<AiJobAcceptedResponse> submitResumeForParsing(AiResumeParseRequest request) {
        try {
            AiJobAcceptedResponse response = restClient.post()
                    .uri("/api/v1/resumes/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiJobAcceptedResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine submitResumeForParsing failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 2: Check Resume Parsing Job Status
     */
    public Optional<AiJobStatusResponse> checkResumeParseStatus(String jobId) {
        try {
            AiJobStatusResponse response = restClient.get()
                    .uri("/api/v1/resumes/status/{jobId}", jobId)
                    .retrieve()
                    .body(AiJobStatusResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine checkResumeParseStatus for job {} failed: {}", jobId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 3: Match Engine (Semantic Vector Similarity)
     */
    public Optional<AiMatchScoreResponse> calculateMatchScore(AiMatchScoreRequest request) {
        try {
            AiMatchScoreResponse response = restClient.post()
                    .uri("/api/v1/match/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiMatchScoreResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine calculateMatchScore failed ({}). Fallback may be used.", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 4: Pre-Screening Conversational Chatbot
     */
    public Optional<AiChatMessageResponse> sendChatMessage(AiChatMessageRequest request) {
        try {
            AiChatMessageResponse response = restClient.post()
                    .uri("/api/v1/chatbot/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiChatMessageResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine sendChatMessage failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 5A: Generate Interview Questions
     */
    public Optional<AiInterviewQuestionsResponse> generateInterviewQuestions(AiInterviewQuestionsRequest request) {
        try {
            AiInterviewQuestionsResponse response = restClient.post()
                    .uri("/api/v1/interview/questions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiInterviewQuestionsResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine generateInterviewQuestions failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 5B: Evaluate Interview Answers
     */
    public Optional<AiInterviewEvaluateResponse> evaluateInterview(AiInterviewEvaluateRequest request) {
        try {
            AiInterviewEvaluateResponse response = restClient.post()
                    .uri("/api/v1/interview/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiInterviewEvaluateResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine evaluateInterview failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 6: Finalize Decision
     */
    public Optional<AiDecisionResponse> finalizeDecision(AiDecisionRequest request) {
        try {
            AiDecisionResponse response = restClient.post()
                    .uri("/api/v1/decision/finalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiDecisionResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("AI Engine finalizeDecision failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Admin: AI Engine Health Summary for all 6 modules
     */
    public Optional<com.vionsys.hireai.admin.dto.AdminHealthResponse> getAdminHealthSummary() {
        try {
            com.vionsys.hireai.admin.dto.AdminHealthResponse response = adminRestClient.get()
                    .uri("/api/v1/admin/health-summary")
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminHealthResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine admin health summary check failed ({}). Marking modules as degraded.", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Admin: AI Engine LLM Usage, Cost & Quota Statistics
     */
    public Optional<com.vionsys.hireai.admin.dto.AdminStatsResponse> getAdminStats(String range) {
        try {
            com.vionsys.hireai.admin.dto.AdminStatsResponse response = adminRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/stats")
                            .queryParam("range", range != null && !range.isBlank() ? range : "all")
                            .build())
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminStatsResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine stats check failed ({}).", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Admin: AI Engine Call Logs and Audit Trail
     */
    public Optional<com.vionsys.hireai.admin.dto.AdminCallLogsResponse> getAdminLogs(
            String query, String taskType, String status, String provider,
            String from, String to, String correlationId, String candidateRef,
            int page, int size) {
        try {
            com.vionsys.hireai.admin.dto.AdminCallLogsResponse response = adminRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/logs")
                            .queryParam("query", query != null && !query.isBlank() ? query : null)
                            .queryParam("task_type", taskType != null && !taskType.isBlank() && !"ALL".equalsIgnoreCase(taskType) ? taskType : null)
                            .queryParam("status", status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status) ? status : null)
                            .queryParam("provider", provider != null && !provider.isBlank() && !"ALL".equalsIgnoreCase(provider) ? provider : null)
                            .queryParam("from_date", from != null && !from.isBlank() ? from : null)
                            .queryParam("to_date", to != null && !to.isBlank() ? to : null)
                            .queryParam("correlation_id", correlationId != null && !correlationId.isBlank() ? correlationId : null)
                            .queryParam("candidate_ref", candidateRef != null && !candidateRef.isBlank() ? candidateRef : null)
                            .queryParam("page", page > 0 ? page : 1)
                            .queryParam("size", size > 0 ? size : 20)
                            .build())
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminCallLogsResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine logs query failed ({}).", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Admin: Retrieve a single call log entry by ID
     */
    public Optional<com.vionsys.hireai.admin.dto.AdminCallLogEntry> getAdminLogById(String id) {
        try {
            com.vionsys.hireai.admin.dto.AdminCallLogEntry response = adminRestClient.get()
                    .uri("/api/v1/admin/logs/{id}", id)
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminCallLogEntry.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine single log query failed for ID {} ({}).", id, ex.getMessage());
            return Optional.empty();
        }
    }
}
