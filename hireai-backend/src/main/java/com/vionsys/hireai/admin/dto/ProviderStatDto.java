package com.vionsys.hireai.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class ProviderStatDto {
    private String provider;

    @JsonAlias({"calls", "callCount"})
    private Long calls;

    @JsonAlias({"callCount", "calls"})
    private Long callCount;

    private Double percentage;

    @JsonAlias({"avgLatencySeconds", "avg_latency_seconds"})
    private Double avgLatencySeconds;

    @JsonAlias({"failedCount", "failed_count"})
    private Long failedCount;

    public Long getCalls() {
        return calls != null ? calls : callCount;
    }

    public Long getCallCount() {
        return callCount != null ? callCount : calls;
    }
}
