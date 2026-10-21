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

        String internalKey = this.properties.getInternalKey() != null ? this.properties.getInternalKey() : "";

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Internal-Key", internalKey)
                .build();

        // Dedicated fast RestClient for admin telemetry and monitoring calls
        int adminConnectTimeout = this.properties.getHealthConnectTimeoutMs() > 0 ? this.properties.getHealthConnectTimeoutMs() : 2000;
        int adminReadTimeout = this.properties.getHealthReadTimeoutMs() > 0 ? this.properties.getHealthReadTimeoutMs() : 3000;

        SimpleClientHttpRequestFactory adminFactory = new SimpleClientHttpRequestFactory();
        adminFactory.setConnectTimeout(Duration.ofMillis(adminConnectTimeout));
        adminFactory.setReadTimeout(Duration.ofMillis(adminReadTimeout));

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

    public record HealthCheckResult(
            boolean isSuccessful,
            com.vionsys.hireai.admin.dto.AdminHealthResponse response,
            String statusReasonCode,
            String statusMessage,
            Throwable cause
    ) {}

    /**
     * Admin: AI Engine Health Summary for all 6 modules with granular error classification.
     */
    public HealthCheckResult checkAdminHealthDetailed() {
        try {
            com.vionsys.hireai.admin.dto.AdminHealthResponse response = adminRestClient.get()
                    .uri("/api/v1/admin/health-summary")
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminHealthResponse.class);

            if (response == null) {
                return new HealthCheckResult(false, null, "SERVICE_ERROR", "AI Engine returned empty response payload", null);
            }

            boolean anyDegraded = false;
            String degradedReason = null;
            if (response.getModules() != null) {
                for (com.vionsys.hireai.admin.dto.ModuleHealthDto mod : response.getModules()) {
                    String st = mod.getStatus() != null ? mod.getStatus().toUpperCase() : "UNKNOWN";
                    if (!"HEALTHY".equals(st) && !"UP".equals(st)) {
                        anyDegraded = true;
                        if (mod.getError() != null && !mod.getError().isBlank()) {
                            degradedReason = mod.getName() + " (" + mod.getError() + ")";
                        } else if (degradedReason == null) {
                            degradedReason = mod.getName() + " is " + st;
                        }
                    }
                }
            }

            if ("DEGRADED".equalsIgnoreCase(response.getOverallStatus()) || "DOWN".equalsIgnoreCase(response.getOverallStatus()) || anyDegraded) {
                String msg = degradedReason != null
                        ? "AI Engine operational but degraded: " + degradedReason
                        : "AI Engine operational but one or more modules/providers are degraded";
                response.setOverallStatus("MODULE_DEGRADED");
                return new HealthCheckResult(true, response, "MODULE_DEGRADED", msg, null);
            }

            response.setOverallStatus("HEALTHY");
            return new HealthCheckResult(true, response, "HEALTHY", "AI Engine and all modules operational", null);

        } catch (org.springframework.web.client.ResourceAccessException ex) {
            String detail = ex.getMessage();
            boolean isTimeout = detail != null && (detail.toLowerCase().contains("timeout") || detail.toLowerCase().contains("timed out"));
            String msg = isTimeout
                    ? "AI Engine request timed out connecting to " + properties.getBaseUrl()
                    : "AI Engine connection refused at " + properties.getBaseUrl() + " (Service Down)";
            return new HealthCheckResult(false, null, "SERVICE_UNREACHABLE", msg, ex);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            String msg = "AI Engine HTTP " + ex.getStatusCode().value() + " error: " + ex.getStatusText();
            return new HealthCheckResult(false, null, "SERVICE_ERROR", msg, ex);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            String msg = "AI Engine REST error (" + ex.getStatusCode() + "): " + ex.getMessage();
            return new HealthCheckResult(false, null, "SERVICE_ERROR", msg, ex);
        } catch (Exception ex) {
            String msg = "AI Engine check failed: " + ex.getMessage();
            return new HealthCheckResult(false, null, "SERVICE_ERROR", msg, ex);
        }
    }

    /**
     * Admin: AI Engine Health Summary (Backward-compatible Optional wrapper).
     */
    public Optional<com.vionsys.hireai.admin.dto.AdminHealthResponse> getAdminHealthSummary() {
        HealthCheckResult result = checkAdminHealthDetailed();
        return result.isSuccessful() ? Optional.ofNullable(result.response()) : Optional.empty();
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
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/v1/admin/logs");
                        if (query != null && !query.isBlank()) uriBuilder.queryParam("query", query.trim());
                        if (taskType != null && !taskType.isBlank() && !"ALL".equalsIgnoreCase(taskType)) uriBuilder.queryParam("task_type", taskType.trim());
                        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) uriBuilder.queryParam("status", status.trim());
                        if (provider != null && !provider.isBlank() && !"ALL".equalsIgnoreCase(provider)) uriBuilder.queryParam("provider", provider.trim());
                        if (from != null && !from.isBlank()) uriBuilder.queryParam("from_date", from.trim());
                        if (to != null && !to.isBlank()) uriBuilder.queryParam("to_date", to.trim());
                        if (correlationId != null && !correlationId.isBlank()) uriBuilder.queryParam("correlation_id", correlationId.trim());
                        if (candidateRef != null && !candidateRef.isBlank()) uriBuilder.queryParam("candidate_ref", candidateRef.trim());
                        uriBuilder.queryParam("page", page > 0 ? page : 1);
                        uriBuilder.queryParam("size", size > 0 ? size : 20);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(com.vionsys.hireai.admin.dto.AdminCallLogsResponse.class);

            if (response != null && response.getItems() != null) {
                response.getItems().forEach(entry -> {
                    if (entry.getPromptPreview() != null && entry.getPromptPreview().length() > 200) {
                        entry.setPromptPreview(entry.getPromptPreview().substring(0, 200));
                    }
                    if (entry.getResponsePreview() != null && entry.getResponsePreview().length() > 200) {
                        entry.setResponsePreview(entry.getResponsePreview().substring(0, 200));
                    }
                });
            }

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

            if (response != null) {
                if (response.getPromptPreview() != null && response.getPromptPreview().length() > 200) {
                    response.setPromptPreview(response.getPromptPreview().substring(0, 200));
                }
                if (response.getResponsePreview() != null && response.getResponsePreview().length() > 200) {
                    response.setResponsePreview(response.getResponsePreview().substring(0, 200));
                }
            }

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Engine single log query failed for ID {} ({}).", id, ex.getMessage());
            return Optional.empty();
        }
    }
}
