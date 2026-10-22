package com.vionsys.hireai.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminCallLogEntry {
    private String id;
    private String timestamp;

    @JsonAlias({"task_type", "taskType", "module"})
    private String taskType;

    private String provider;
    private String model;
    private String status;

    @JsonAlias({"latency_seconds", "latencySeconds"})
    private Double latencySeconds;

    private Integer attempt;

    @JsonAlias({"prompt_tokens", "promptTokens"})
    private Integer promptTokens;

    @JsonAlias({"completion_tokens", "completionTokens"})
    private Integer completionTokens;

    @JsonAlias({"total_tokens", "totalTokens"})
    private Integer totalTokens;

    @JsonAlias({"prompt_preview", "promptPreview"})
    private String promptPreview;

    @JsonAlias({"response_preview", "responsePreview"})
    private String responsePreview;

    @JsonAlias({"error_message", "errorMessage", "error"})
    private String errorMessage;

    @JsonAlias({"request_id", "requestId", "correlation_id", "correlationId"})
    private String requestId;

    @JsonAlias({"candidate_ref", "candidateRef"})
    private String candidateRef;

    public String getCorrelationId() {
        return requestId;
    }
}
