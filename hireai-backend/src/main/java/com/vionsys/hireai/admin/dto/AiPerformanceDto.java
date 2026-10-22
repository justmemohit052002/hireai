package com.vionsys.hireai.admin.dto;

import java.util.List;
import java.util.Map;
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
public class AiPerformanceDto {
    private String range;
    private String windowStart;
    private String windowEnd;
    private String generatedAt;
    private String source;
    private long callsToday;
    private long callsThisWeek;
    private long totalCalls;
    private Long allTimeCalls;
    private long successCount;
    private long failureCount;
    private Double successRate;
    private Double avgLatencySeconds;
    private Double p50LatencySeconds;
    private Double p95LatencySeconds;
    private Map<String, Long> providerSplit;
    private List<ModuleAverageDto> moduleAverages;
    private QuotaUsageDto geminiQuota;
    private List<AdminCallLogEntry> recentFailures;
}
