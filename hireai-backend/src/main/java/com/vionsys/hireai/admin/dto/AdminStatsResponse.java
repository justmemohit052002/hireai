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
public class AdminStatsResponse {
    private Boolean available;
    private String message;
    private Long totalCalls;
    private Long todayCalls;
    private Long weekCalls;
    private Long failedCalls;
    private Double successRate;
    private Double avgLatencySeconds;
    private Long unknownTimeCount;
    private QuotaUsageDto quota;
    private List<ProviderStatDto> providerDistribution;
    private List<ModuleStatDto> moduleBreakdown;
}
