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
public class QuotaUsageDto {
    private String provider;
    private String tier;
    private Integer dailyLimit;
    private Long usedToday;
    private Long remainingToday;
    private Double percentageUsed;
    private String timezone;
}
