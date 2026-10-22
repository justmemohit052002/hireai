package com.vionsys.hireai.admin.dto;

import java.util.List;
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
public class AdminStatsResponse {
    private Boolean available;
    private String message;
    private String range;
    private String windowStart;
    private String windowEnd;
    private String generatedAt;
    private String source;
    private Long totalCalls;
    private Long allTimeCalls;
    private Long todayCalls;
    private Long weekCalls;
    private Long failedCalls;
    private Double successRate;
    private Double avgLatencySeconds;
    private Double p50LatencySeconds;
    private Double p95LatencySeconds;
    private Long unknownTimeCount;
    private QuotaUsageDto quota;
    private List<ProviderStatDto> providerDistribution;
    private List<ModuleStatDto> moduleBreakdown;
}
