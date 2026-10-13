package com.vionsys.hireai.admin.dto;

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
public class AdminCallLogEntry {
    private String id;
    private String timestamp;
    private String taskType;
    private String provider;
    private String model;
    private String status;
    private Double latencySeconds;
    private Integer attempt;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String promptPreview;
    private String responsePreview;
    private String errorMessage;
    private String requestId;
    private String candidateRef;
}
