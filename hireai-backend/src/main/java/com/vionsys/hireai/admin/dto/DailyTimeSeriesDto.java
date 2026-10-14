package com.vionsys.hireai.admin.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTimeSeriesDto {
    private String date; // YYYY-MM-DD
    private long signups;
    private long activeUsers;
    private long apiRequests;
    private long apiErrors;
    private double errorRate;
    private long aiCalls;
    private Map<String, Long> aiCallsByModule;
}
