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
public class ApplicationPerformanceDto {
    private double overallAvgResponseTimeMs;
    private long totalRequests;
    private long totalErrors;
    private double overallErrorRate;
    private List<EndpointGroupMetricDto> endpointGroups;
}
