package com.vionsys.hireai.admin.dto;

import java.util.List;
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
public class AdminHealthResponse {
    private String overallStatus;
    private String service;
    private String checkedAt;
    private String lastCheckedAt;
    private Boolean isStale;
    private String backendStatus;
    private String databaseStatus;
    private Long activeUsersCount;
    private Long totalUsersCount;
    private List<ModuleHealthDto> modules;
    private ProvidersConfigDto providers;
    private EnvironmentConfigDto environment;
    private String error;
    private String statusReasonCode;
    private String statusMessage;
    private String lastSuccessfulPoll;
    private String lastAttempt;
    private Integer consecutiveFailures;
}
