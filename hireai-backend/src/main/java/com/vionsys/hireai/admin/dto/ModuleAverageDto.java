package com.vionsys.hireai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAverageDto {
    private String moduleId;
    private String moduleName;
    private long totalCalls;
    private long failureCount;
    private double avgLatencySeconds;
    private double p50LatencySeconds;
    private double p95LatencySeconds;
    private double successRate;
}
