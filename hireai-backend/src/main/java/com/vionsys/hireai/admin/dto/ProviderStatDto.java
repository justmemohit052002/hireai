package com.vionsys.hireai.admin.dto;

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
public class ProviderStatDto {
    private String provider;
    private Long calls;
    private Double percentage;
    private Double avgLatencySeconds;
    private Long failedCount;
}
