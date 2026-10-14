package com.vionsys.hireai.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDetailsDto {
    private long backendUptimeSeconds;
    private String backendStartedAt;
    private String databaseConnectivity; // UP / DOWN
    private String aiEngineReachability; // HEALTHY / DEGRADED / OFFLINE
    private boolean emailSmtpConfigured;
    private String lastHealthPollAt;
    private List<ModuleHealthStateDto> modules;
}
