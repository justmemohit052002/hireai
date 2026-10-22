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
public class PlatformMetricsResponse {
    private String range; // today | 7d | 30d | all
    private String windowStart;
    private String windowEnd;
    private String generatedAt;
    private String source;
    private String environment;
    private String lastUpdatedAt;
    private boolean cached;
    private OverviewMetricsDto overview;
    private List<DailyTimeSeriesDto> timeSeries;
    private AiPerformanceDto aiPerformance;
    private SystemHealthDetailsDto systemHealth;
    private ApplicationPerformanceDto appPerformance;
}
