package com.vionsys.hireai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewMetricsDto {
    private long totalUsers;
    private long candidatesCount;
    private long recruitersCount;
    private long adminsCount;
    private long subAdminsCount;
    private long activeUsersCount;
    private long deactivatedUsersCount;

    // Signups
    private long signupsToday;
    private long signups7Days;
    private long signups30Days;

    // Active users by login/refresh
    private long activeUsers24h;
    private long activeUsers7d;
    private long activeUsers30d;

    // Jobs and Applications
    private long totalJobs;
    private long openJobs;
    private long totalApplications;

    // Averages
    private double avgApplicationsPerJob;
    private double avgDailySignups;
    private double avgDailyActiveUsers;
}
