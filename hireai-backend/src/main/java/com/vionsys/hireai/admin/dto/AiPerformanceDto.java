package com.vionsys.hireai.admin.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPerformanceDto {
    private long callsToday;
    private long callsThisWeek;
    private long totalCalls;
    private long successCount;
    private long failureCount;
    private double successRate;
    private double avgLatencySeconds;
    private double p50LatencySeconds;
    private double p95LatencySeconds;
    private Map<String, Long> providerSplit;
    private List<ModuleAverageDto> moduleAverages;
    private QuotaUsageDto geminiQuota;
    private List<AdminCallLogEntry> recentFailures;
}
