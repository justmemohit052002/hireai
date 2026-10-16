package com.vionsys.hireai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointGroupMetricDto {
    private String groupName;
    private long requestCount;
    private long errorCount;
    private double avgResponseTimeMs;
    private double errorRate;
}
