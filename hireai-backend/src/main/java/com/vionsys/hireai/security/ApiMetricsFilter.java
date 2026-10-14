package com.vionsys.hireai.security;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vionsys.hireai.admin.dto.ApplicationPerformanceDto;
import com.vionsys.hireai.admin.dto.EndpointGroupMetricDto;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiMetricsFilter extends OncePerRequestFilter {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();

    private final Map<String, GroupStats> groupStatsMap = new ConcurrentHashMap<>();
    private final Map<String, DayStats> dayStatsMap = new ConcurrentHashMap<>();

    public static class GroupStats {
        public final LongAdder requests = new LongAdder();
        public final LongAdder errors = new LongAdder();
        public final LongAdder latencyMs = new LongAdder();
    }

    public static class DayStats {
        public final LongAdder requests = new LongAdder();
        public final LongAdder errors = new LongAdder();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String group = resolveEndpointGroup(request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = Math.max(0, System.currentTimeMillis() - startTime);
            int status = response.getStatus();
            boolean isError = status >= 400;

            totalRequests.increment();
            totalLatencyMs.add(duration);
            if (isError) {
                totalErrors.increment();
            }

            GroupStats gStats = groupStatsMap.computeIfAbsent(group, k -> new GroupStats());
            gStats.requests.increment();
            gStats.latencyMs.add(duration);
            if (isError) {
                gStats.errors.increment();
            }

            String todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            DayStats dStats = dayStatsMap.computeIfAbsent(todayKey, k -> new DayStats());
            dStats.requests.increment();
            if (isError) {
                dStats.errors.increment();
            }
        }
    }

    private String resolveEndpointGroup(String uri) {
        if (uri == null) return "General APIs";
        String lower = uri.toLowerCase();
        if (lower.startsWith("/auth")) return "Authentication & Identity";
        if (lower.startsWith("/jobs")) return "Job Management & Search";
        if (lower.startsWith("/candidate") || lower.startsWith("/candidates")) return "Candidate Portal";
        if (lower.startsWith("/recruiter")) return "Recruiter Pipeline";
        if (lower.startsWith("/admin")) return "Admin & Telemetry";
        if (lower.startsWith("/applications")) return "Applications & ATS";
        return "General Platform APIs";
    }

    public ApplicationPerformanceDto getSnapshot() {
        long reqs = totalRequests.sum();
        long errs = totalErrors.sum();
        long lats = totalLatencyMs.sum();

        double avgLatency = reqs > 0 ? Math.round((double) lats / reqs * 10.0) / 10.0 : 0.0;
        double errorRate = reqs > 0 ? Math.round((double) errs / reqs * 1000.0) / 10.0 : 0.0;

        List<EndpointGroupMetricDto> groupList = new ArrayList<>();
        groupStatsMap.forEach((name, g) -> {
            long gReqs = g.requests.sum();
            long gErrs = g.errors.sum();
            long gLats = g.latencyMs.sum();
            double gAvg = gReqs > 0 ? Math.round((double) gLats / gReqs * 10.0) / 10.0 : 0.0;
            double gErrRate = gReqs > 0 ? Math.round((double) gErrs / gReqs * 1000.0) / 10.0 : 0.0;

            groupList.add(EndpointGroupMetricDto.builder()
                    .groupName(name)
                    .requestCount(gReqs)
                    .errorCount(gErrs)
                    .avgResponseTimeMs(gAvg)
                    .errorRate(gErrRate)
                    .build());
        });

        // Ensure default groups exist even if zero traffic yet
        String[] defaults = {
                "Authentication & Identity", "Job Management & Search",
                "Candidate Portal", "Recruiter Pipeline",
                "Admin & Telemetry", "Applications & ATS"
        };
        for (String def : defaults) {
            if (groupList.stream().noneMatch(g -> g.getGroupName().equals(def))) {
                groupList.add(EndpointGroupMetricDto.builder()
                        .groupName(def)
                        .requestCount(0)
                        .errorCount(0)
                        .avgResponseTimeMs(0.0)
                        .errorRate(0.0)
                        .build());
            }
        }

        return ApplicationPerformanceDto.builder()
                .totalRequests(reqs)
                .totalErrors(errs)
                .overallAvgResponseTimeMs(avgLatency)
                .overallErrorRate(errorRate)
                .endpointGroups(groupList)
                .build();
    }

    public DayStats getDayStats(String dateStr) {
        return dayStatsMap.getOrDefault(dateStr, new DayStats());
    }
}
