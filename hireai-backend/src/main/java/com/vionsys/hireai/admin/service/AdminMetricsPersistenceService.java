package com.vionsys.hireai.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.admin.dto.DailyTimeSeriesDto;
import com.vionsys.hireai.admin.entity.AdminDailyMetric;
import com.vionsys.hireai.admin.repository.AdminDailyMetricRepository;
import com.vionsys.hireai.security.ApiMetricsFilter;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMetricsPersistenceService {

    private final AdminDailyMetricRepository dailyMetricRepository;
    private final ApiMetricsFilter apiMetricsFilter;
    private final UserRepository userRepository;

    /**
     * Periodic flush of today's live traffic counters to PostgreSQL admin_daily_metrics table.
     * Scheduled every 60 seconds so in-flight data is safely persisted.
     */
    @Scheduled(fixedRateString = "${hireai.metrics.flush-rate-ms:60000}")
    @Transactional
    public void flushTodayMetrics() {
        try {
            LocalDate today = LocalDate.now();
            String todayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            ApiMetricsFilter.DayStats stats = apiMetricsFilter.getDayStats(todayKey);

            if (stats.getRequests() == 0 && stats.getTotalErrors() == 0) {
                return;
            }

            Optional<AdminDailyMetric> existingOpt = dailyMetricRepository.findByMetricDate(today);
            AdminDailyMetric metric = existingOpt.orElseGet(() -> AdminDailyMetric.builder()
                    .metricDate(today)
                    .createdAt(LocalDateTime.now())
                    .build());

            metric.setRequests(stats.getRequests());
            metric.setClientErrors4xx(stats.getErrors4xx());
            metric.setServerErrors5xx(stats.getErrors5xx());
            metric.setAvgLatencyMs(stats.getAvgLatencyMs());
            metric.setUpdatedAt(LocalDateTime.now());

            dailyMetricRepository.save(metric);
        } catch (Exception ex) {
            log.warn("Failed periodic flush of daily metrics: {}", ex.getMessage());
        }
    }

    /**
     * Flush live metrics during application shutdown so no history is lost.
     */
    @PreDestroy
    @Transactional
    public void onShutdown() {
        log.info("Flushing today's metrics before shutdown...");
        flushTodayMetrics();
    }

    /**
     * 90-day retention cleanup running daily at 02:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredMetrics() {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(90);
            int deleted = dailyMetricRepository.deleteByMetricDateBefore(cutoff);
            if (deleted > 0) {
                log.info("Purged {} daily metric records older than 90 days (cutoff: {})", deleted, cutoff);
            }
        } catch (Exception ex) {
            log.warn("Failed to purge expired metrics: {}", ex.getMessage());
        }
    }

    /**
     * Builds complete time-series data: past days from admin_daily_metrics table + today from live in-memory filter.
     */
    @Transactional(readOnly = true)
    public List<DailyTimeSeriesDto> getTimeSeries(LocalDate startDate, int daysBack) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;

        // 1. Fetch persisted metrics up to yesterday
        List<AdminDailyMetric> persistedMetrics = dailyMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(
                startDate, today.minusDays(1));
        Map<String, AdminDailyMetric> persistedMap = persistedMetrics.stream()
                .collect(Collectors.toMap(m -> m.getMetricDate().format(dtf), m -> m));

        // 2. Fetch recent user registrations and login activities
        List<User> recentCreatedUsers = userRepository.findUsersCreatedSince(startDate.atStartOfDay());
        List<User> recentActiveUsers = userRepository.findUsersActiveSince(startDate.atStartOfDay());

        Map<String, Long> signupsPerDay = recentCreatedUsers.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate().format(dtf), Collectors.counting()));

        Map<String, Long> activePerDay = recentActiveUsers.stream()
                .filter(u -> u.getLastLoginAt() != null)
                .collect(Collectors.groupingBy(u -> u.getLastLoginAt().toLocalDate().format(dtf), Collectors.counting()));

        // 3. Assemble each day in range
        List<DailyTimeSeriesDto> result = new ArrayList<>();
        for (int i = 0; i < daysBack; i++) {
            LocalDate day = startDate.plusDays(i);
            String dayStr = day.format(dtf);

            long reqs = 0;
            long client4xx = 0;
            long server5xx = 0;
            Double avgLat = null;
            Double p95Lat = null;
            boolean hasData = false;

            if (day.equals(today)) {
                // Today: read from live in-memory filter
                ApiMetricsFilter.DayStats ds = apiMetricsFilter.getDayStats(dayStr);
                reqs = ds.getRequests();
                client4xx = ds.getErrors4xx();
                server5xx = ds.getErrors5xx();
                avgLat = reqs > 0 ? ds.getAvgLatencyMs() : null;
                hasData = (reqs > 0);
            } else {
                // Past days: read from persisted table
                AdminDailyMetric persisted = persistedMap.get(dayStr);
                if (persisted != null) {
                    reqs = persisted.getRequests();
                    client4xx = persisted.getClientErrors4xx();
                    server5xx = persisted.getServerErrors5xx();
                    avgLat = persisted.getAvgLatencyMs();
                    p95Lat = persisted.getP95LatencyMs();
                    hasData = true;
                }
            }

            long signups = signupsPerDay.getOrDefault(dayStr, 0L);
            long active = activePerDay.getOrDefault(dayStr, 0L);
            long totalErr = client4xx + server5xx;
            double errorRate = reqs > 0 ? Math.round((double) totalErr / reqs * 1000.0) / 10.0 : 0.0;

            result.add(DailyTimeSeriesDto.builder()
                    .date(dayStr)
                    .signups(signups)
                    .activeUsers(active)
                    .apiRequests(reqs)
                    .apiErrors(totalErr)
                    .serverErrors(server5xx)
                    .clientErrors(client4xx)
                    .errorRate(errorRate)
                    .avgLatencyMs(avgLat)
                    .p95LatencyMs(p95Lat)
                    .hasData(hasData)
                    .aiCalls(0L)
                    .aiCallsByModule(Map.of())
                    .build());
        }

        return result;
    }
}
