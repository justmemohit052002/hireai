package com.vionsys.hireai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.DailyTimeSeriesDto;
import com.vionsys.hireai.admin.dto.EndpointGroupMetricDto;
import com.vionsys.hireai.admin.dto.ModuleStatDto;
import com.vionsys.hireai.admin.dto.PlatformMetricsResponse;
import com.vionsys.hireai.admin.dto.ProviderStatDto;
import com.vionsys.hireai.admin.entity.AdminDailyMetric;
import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.admin.repository.AdminDailyMetricRepository;
import com.vionsys.hireai.admin.service.impl.AdminServiceImpl;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.config.AiEngineProperties;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.email.EmailProperties;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.ApiMetricsFilter;
import com.vionsys.hireai.user.repository.UserRepository;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AdminMetricsAndConsistencyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private AdminHealthPollerService healthPollerService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminMetricsPersistenceService metricsPersistenceService;

    @Mock
    private Environment environment;

    private TestAiEngineClient aiEngineClient;
    private ApiMetricsFilter apiMetricsFilter;
    private AdminServiceImpl adminService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    static class TestAiEngineClient extends AiEngineClient {
        private AdminStatsResponse statsResponse;

        public TestAiEngineClient() {
            super(new AiEngineProperties());
        }

        public void setStatsResponse(AdminStatsResponse statsResponse) {
            this.statsResponse = statsResponse;
        }

        @Override
        public Optional<AdminStatsResponse> getAdminStats(String range) {
            return Optional.ofNullable(statsResponse);
        }

        @Override
        public Optional<AdminCallLogsResponse> getAdminLogs(
                String query, String taskType, String status, String provider,
                String from, String to, String correlationId, String candidateRef,
                int page, int size) {
            return Optional.of(com.vionsys.hireai.admin.dto.AdminCallLogsResponse.builder()
                    .available(true)
                    .items(List.of())
                    .totalElements(0L)
                    .page(page)
                    .size(size)
                    .totalPages(0)
                    .build());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.lenient().when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        aiEngineClient = new TestAiEngineClient();
        apiMetricsFilter = new ApiMetricsFilter();

        adminService = new AdminServiceImpl(
                userRepository,
                roleRepository,
                auditLogRepository,
                healthPollerService,
                aiEngineClient,
                refreshTokenRepository,
                emailService,
                new EmailProperties(),
                passwordEncoder,
                jobRepository,
                jobApplicationRepository,
                apiMetricsFilter
        );

        // Inject optional autowired dependencies via reflection
        Field metricsField = AdminServiceImpl.class.getDeclaredField("metricsPersistenceService");
        metricsField.setAccessible(true);
        metricsField.set(adminService, metricsPersistenceService);

        Field envField = AdminServiceImpl.class.getDeclaredField("environment");
        envField.setAccessible(true);
        envField.set(adminService, environment);
    }

    // =========================================================================
    // 1. Invariant Tests: Jobs & Applications
    // =========================================================================

    @Test
    @DisplayName("Platform metrics enforce totalJobs >= openJobs invariant even if DB counts differ")
    void testTotalJobsEnforcesInvariant() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countActiveUsersSince(any())).thenReturn(2L);
        // Simulate dirty DB state: open jobs = 5, total jobs count = 2
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(5L);
        when(jobRepository.count()).thenReturn(2L);
        when(jobApplicationRepository.count()).thenReturn(6L);
        aiEngineClient.setStatsResponse(null);

        PlatformMetricsResponse metrics = adminService.getPlatformMetrics("7d");

        assertNotNull(metrics);
        assertNotNull(metrics.getOverview());
        assertEquals(5L, metrics.getOverview().getOpenJobs());
        // Invariant: totalJobs must be at least openJobs (5 >= 5)
        assertEquals(5L, metrics.getOverview().getTotalJobs());
        // Ratio: 6 applications / 5 open jobs = 1.2
        assertEquals(1.2, metrics.getOverview().getAvgApplicationsPerOpenJob(), 0.001);
    }

    @Test
    @DisplayName("Application ratio safely returns 0.0 when openJobs is 0 (no division by zero)")
    void testApplicationRatioZeroOpenJobs() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countActiveUsersSince(any())).thenReturn(0L);
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(0L);
        when(jobRepository.count()).thenReturn(0L);
        when(jobApplicationRepository.count()).thenReturn(0L);
        aiEngineClient.setStatsResponse(null);

        PlatformMetricsResponse metrics = adminService.getPlatformMetrics("7d");

        assertEquals(0L, metrics.getOverview().getOpenJobs());
        assertEquals(0.0, metrics.getOverview().getAvgApplicationsPerOpenJob());
    }

    // =========================================================================
    // 2. AI Performance Scoping & Empty-Window Nulls
    // =========================================================================

    @Test
    @DisplayName("AI Performance returns null for averages/rates when 0 calls occur in the window")
    void testAiPerformanceEmptyWindowReturnsNullAverages() {
        when(userRepository.count()).thenReturn(10L);
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(1L);
        when(jobRepository.count()).thenReturn(1L);
        when(jobApplicationRepository.count()).thenReturn(1L);

        // Window returns 0 calls
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalCalls(0L)
                .avgLatencySeconds(null)
                .p50LatencySeconds(null)
                .p95LatencySeconds(null)
                .successRate(null)
                .range("today")
                .build();
        aiEngineClient.setStatsResponse(stats);

        PlatformMetricsResponse metrics = adminService.getPlatformMetrics("today");

        assertNotNull(metrics.getAiPerformance());
        assertEquals(0L, metrics.getAiPerformance().getTotalCalls());
        assertNull(metrics.getAiPerformance().getAvgLatencySeconds(), "Avg latency must be null when 0 calls");
        assertNull(metrics.getAiPerformance().getP95LatencySeconds(), "p95 latency must be null when 0 calls");
        assertNull(metrics.getAiPerformance().getSuccessRate(), "Success rate must be null when 0 calls");
    }

    @Test
    @DisplayName("AI Performance returns computed metrics when window has calls")
    void testAiPerformanceWithCallsReturnsMetrics() {
        when(userRepository.count()).thenReturn(10L);
        when(jobRepository.countByStatus(JobStatus.OPEN)).thenReturn(1L);
        when(jobRepository.count()).thenReturn(1L);
        when(jobApplicationRepository.count()).thenReturn(1L);

        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalCalls(10L)
                .avgLatencySeconds(1.85)
                .p50LatencySeconds(1.50)
                .p95LatencySeconds(3.20)
                .successRate(90.0)
                .range("7d")
                .build();
        aiEngineClient.setStatsResponse(stats);

        PlatformMetricsResponse metrics = adminService.getPlatformMetrics("7d");

        assertNotNull(metrics.getAiPerformance());
        assertEquals(10L, metrics.getAiPerformance().getTotalCalls());
        assertEquals(1.85, metrics.getAiPerformance().getAvgLatencySeconds());
        assertEquals(3.20, metrics.getAiPerformance().getP95LatencySeconds());
        assertEquals(90.0, metrics.getAiPerformance().getSuccessRate());
    }

    // =========================================================================
    // 3. Dual-Case & Snake_Case Deserialization
    // =========================================================================

    @Test
    @DisplayName("AdminCallLogEntry deserializes snake_case Python AI engine payload seamlessly")
    void testAdminCallLogEntryDeserialization() throws Exception {
        String json = """
            {
                "id": "log-abc-123",
                "timestamp": "2026-10-21T18:00:00Z",
                "task_type": "jd_generation",
                "provider": "ollama",
                "model": "llama3.2:3b",
                "latency_seconds": 2.45,
                "attempt": 1,
                "status": "SUCCESS",
                "error_message": null,
                "prompt_preview": "Create a Senior Java Developer JD",
                "response_preview": "Senior Java Developer responsibilities...",
                "correlation_id": "corr-uuid-777",
                "candidate_ref": "cand-999"
            }
            """;

        AdminCallLogEntry entry = objectMapper.readValue(json, AdminCallLogEntry.class);

        assertNotNull(entry);
        assertEquals("log-abc-123", entry.getId());
        assertEquals("jd_generation", entry.getTaskType());
        assertEquals("ollama", entry.getProvider());
        assertEquals(2.45, entry.getLatencySeconds());
        assertEquals("SUCCESS", entry.getStatus());
        assertEquals("Create a Senior Java Developer JD", entry.getPromptPreview());
        assertEquals("Senior Java Developer responsibilities...", entry.getResponsePreview());
        assertEquals("corr-uuid-777", entry.getCorrelationId());
        assertEquals("cand-999", entry.getCandidateRef());
    }

    @Test
    @DisplayName("ModuleStatDto & ProviderStatDto support both 'calls' and 'callCount'")
    void testStatDtosSupportCallCountAlias() throws Exception {
        String moduleJson = """
            {
                "id": "jd_generator",
                "name": "Job Description Generator",
                "callCount": 25,
                "percentage": 50.0,
                "avgLatencySeconds": 1.5
            }
            """;
        ModuleStatDto mod = objectMapper.readValue(moduleJson, ModuleStatDto.class);
        assertEquals("jd_generator", mod.getId());
        assertEquals("Job Description Generator", mod.getName());
        assertEquals(25L, mod.getCalls());
        assertEquals(25L, mod.getCallCount());

        String providerJson = """
            {
                "provider": "ollama",
                "name": "Ollama (Local)",
                "callCount": 40,
                "percentage": 80.0,
                "avgLatencySeconds": 1.2
            }
            """;
        ProviderStatDto prov = objectMapper.readValue(providerJson, ProviderStatDto.class);
        assertEquals("ollama", prov.getProvider());
        assertEquals(40L, prov.getCalls());
        assertEquals(40L, prov.getCallCount());
    }

    @Test
    @DisplayName("EndpointGroupMetricDto getters match frontend expectations")
    void testEndpointGroupMetricDtoGetters() {
        EndpointGroupMetricDto dto = EndpointGroupMetricDto.builder()
                .groupName("AUTH")
                .avgResponseTimeMs(45.5)
                .requestCount(120L)
                .build();

        assertEquals("AUTH", dto.getGroup());
        assertEquals("AUTH", dto.getGroupName());
        assertEquals(45.5, dto.getAvgLatencyMs());
        assertEquals(45.5, dto.getAvgResponseTimeMs());
        assertEquals(120L, dto.getTotalRequests());
        assertEquals(120L, dto.getRequestCount());
    }

    // =========================================================================
    // 4. ApiMetricsFilter: 4xx vs 5xx Separation
    // =========================================================================

    @Test
    @DisplayName("ApiMetricsFilter separates 4xx client errors from 5xx server errors in DayStats")
    void testApiMetricsFilterErrorSeparation() throws ServletException, IOException {
        ApiMetricsFilter filter = new ApiMetricsFilter();
        String todayKey = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 1. Success 200
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/v1/jobs");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        res1.setStatus(200);
        filter.doFilter(req1, res1, new MockFilterChain());

        // 2. Client error 401
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        res2.setStatus(401);
        filter.doFilter(req2, res2, new MockFilterChain());

        // 3. Client error 429
        MockHttpServletRequest req3 = new MockHttpServletRequest("POST", "/api/v1/auth/otp");
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        res3.setStatus(429);
        filter.doFilter(req3, res3, new MockFilterChain());

        // 4. Server error 500
        MockHttpServletRequest req4 = new MockHttpServletRequest("GET", "/api/v1/admin/stats");
        MockHttpServletResponse res4 = new MockHttpServletResponse();
        res4.setStatus(500);
        filter.doFilter(req4, res4, new MockFilterChain());

        ApiMetricsFilter.DayStats stats = filter.getDayStats(todayKey);

        // Verify day stats counts
        assertEquals(4L, stats.getRequests());
        assertEquals(2L, stats.getErrors4xx(), "401 and 429 must be counted in errors4xx");
        assertEquals(1L, stats.getErrors5xx(), "500 must be counted in errors5xx");
        assertEquals(3L, stats.getTotalErrors(), "Total API errors must equal 4xx + 5xx");
    }

    // =========================================================================
    // 5. AdminMetricsPersistenceService: Flush & Merge
    // =========================================================================

    @Test
    @DisplayName("AdminMetricsPersistenceService persists snapshot and merges with historical series")
    void testMetricsPersistenceServiceFlushAndMerge() {
        AdminDailyMetricRepository repo = mock(AdminDailyMetricRepository.class);
        ApiMetricsFilter filter = mock(ApiMetricsFilter.class);
        UserRepository userRepo = mock(UserRepository.class);

        ApiMetricsFilter.DayStats mockDayStats = new ApiMetricsFilter.DayStats();
        mockDayStats.requests.add(150L);
        mockDayStats.errors4xx.add(5L);
        mockDayStats.errors5xx.add(2L);
        when(filter.getDayStats(any())).thenReturn(mockDayStats);

        AdminMetricsPersistenceService service = new AdminMetricsPersistenceService(repo, filter, userRepo);

        // Historical persisted metric for yesterday
        LocalDate yesterday = LocalDate.now().minusDays(1);
        AdminDailyMetric pastMetric = AdminDailyMetric.builder()
                .metricDate(yesterday)
                .requests(200L)
                .clientErrors4xx(10L)
                .serverErrors5xx(1L)
                .build();
        when(repo.findByMetricDateBetweenOrderByMetricDateAsc(any(), any())).thenReturn(List.of(pastMetric));

        // Flush today
        service.flushTodayMetrics();
        verify(repo).save(any(AdminDailyMetric.class));

        // Get time series
        List<DailyTimeSeriesDto> series = service.getTimeSeries(yesterday, 2);
        assertNotNull(series);
        assertTrue(series.size() >= 2);

        // Check yesterday's series data
        DailyTimeSeriesDto yDto = series.stream()
                .filter(d -> yesterday.toString().equals(d.getDate()))
                .findFirst()
                .orElse(null);
        assertNotNull(yDto);
        assertEquals(200L, yDto.getApiRequests());
        assertEquals(10L, yDto.getClientErrors());
        assertEquals(1L, yDto.getServerErrors());

        // Check today's series data
        DailyTimeSeriesDto tDto = series.stream()
                .filter(d -> LocalDate.now().toString().equals(d.getDate()))
                .findFirst()
                .orElse(null);
        assertNotNull(tDto);
        assertEquals(150L, tDto.getApiRequests());
        assertEquals(5L, tDto.getClientErrors());
        assertEquals(2L, tDto.getServerErrors());
    }
}
