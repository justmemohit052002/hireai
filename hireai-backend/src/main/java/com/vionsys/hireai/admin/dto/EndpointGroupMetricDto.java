package com.vionsys.hireai.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("group")
    public String getGroup() {
        return groupName;
    }

    @JsonProperty("totalRequests")
    public long getTotalRequests() {
        return requestCount;
    }

    @JsonProperty("avgLatencyMs")
    public double getAvgLatencyMs() {
        return avgResponseTimeMs;
    }
}
