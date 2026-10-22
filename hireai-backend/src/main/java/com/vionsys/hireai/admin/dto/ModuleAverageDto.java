package com.vionsys.hireai.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleAverageDto {
    private String moduleId;
    private String moduleName;
    private long totalCalls;
    private long failureCount;
    private Double avgLatencySeconds;
    private Double p50LatencySeconds;
    private Double p95LatencySeconds;
    private Double successRate;

    @JsonProperty("module")
    public String getModule() {
        return moduleId;
    }

    @JsonProperty("name")
    public String getName() {
        return moduleName != null ? moduleName : moduleId;
    }

    @JsonProperty("calls")
    public long getCalls() {
        return totalCalls;
    }

    @JsonProperty("callCount")
    public long getCallCount() {
        return totalCalls;
    }
}
