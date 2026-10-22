package com.vionsys.hireai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.ModuleHealthDto;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.config.AiEngineProperties;
import com.vionsys.hireai.config.SchedulingConfig;
import com.vionsys.hireai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminHealthPollerServiceTest {

    @Mock
    private AiEngineClient aiEngineClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AiEngineProperties aiEngineProperties;

    @InjectMocks
    private AdminHealthPollerService pollerService;

    @Test
    @DisplayName("Health poller computes HEALTHY status when AI Engine and database are operational")
    void testPollHealth_Success() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(userRepository.countActiveUsers()).thenReturn(5L);
        when(userRepository.countTotalUsers()).thenReturn(10L);

        AdminHealthResponse engineHealth = AdminHealthResponse.builder()
                .overallStatus("HEALTHY")
                .service("HireAI AI Engine")
                .modules(List.of(ModuleHealthDto.builder().id("jd_generator").status("HEALTHY").build()))
                .build();

        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(true, engineHealth, "HEALTHY", "AI Engine operational", null));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("HEALTHY", snapshot.getOverallStatus());
        assertEquals("HEALTHY", snapshot.getStatusReasonCode());
        assertEquals("UP", snapshot.getDatabaseStatus());
        assertEquals("UP", snapshot.getBackendStatus());
        assertEquals(5L, snapshot.getActiveUsersCount());
        assertEquals(0, snapshot.getConsecutiveFailures());
        assertNotNull(snapshot.getLastSuccessfulPoll());
        assertFalse(snapshot.getIsStale());
    }

    @Test
    @DisplayName("Health poller applies threshold: single failure retains snapshot, second failure flips to SERVICE_UNREACHABLE")
    void testPollHealth_ThresholdAndServiceUnreachable() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(userRepository.countActiveUsers()).thenReturn(3L);
        when(userRepository.countTotalUsers()).thenReturn(8L);

        // First establish healthy state
        AdminHealthResponse engineHealth = AdminHealthResponse.builder()
                .overallStatus("HEALTHY")
                .service("HireAI AI Engine")
                .modules(List.of(ModuleHealthDto.builder().id("jd_generator").status("HEALTHY").build()))
                .build();
        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(true, engineHealth, "HEALTHY", "AI Engine operational", null));
        pollerService.pollHealth();
        assertEquals("HEALTHY", pollerService.getCachedHealthSnapshot().getOverallStatus());

        // Failure 1 (below default threshold of 2): retains previous snapshot marked as stale
        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(false, null, "SERVICE_UNREACHABLE", "Connection refused at http://localhost:8000", null));
        pollerService.pollHealth();

        AdminHealthResponse snapshotFail1 = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshotFail1);
        assertEquals(1, snapshotFail1.getConsecutiveFailures());
        assertEquals("HEALTHY", snapshotFail1.getOverallStatus());
        assertTrue(snapshotFail1.getIsStale());

        // Failure 2 (reaches threshold of 2): flips to SERVICE_UNREACHABLE
        pollerService.pollHealth();

        AdminHealthResponse snapshotFail2 = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshotFail2);
        assertEquals("SERVICE_UNREACHABLE", snapshotFail2.getOverallStatus());
        assertEquals("SERVICE_UNREACHABLE", snapshotFail2.getStatusReasonCode());
        assertEquals(2, snapshotFail2.getConsecutiveFailures());
        assertNotNull(snapshotFail2.getModules());
        assertEquals(6, snapshotFail2.getModules().size());
    }

    @Test
    @DisplayName("Health poller maps HTTP 500 or serialization errors to SERVICE_ERROR")
    void testPollHealth_ServiceError() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(false, null, "SERVICE_ERROR", "AI Engine HTTP 500 error: Internal Server Error", null));

        // Two failures to exceed default threshold
        pollerService.pollHealth();
        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("SERVICE_ERROR", snapshot.getOverallStatus());
        assertEquals("SERVICE_ERROR", snapshot.getStatusReasonCode());
        assertTrue(snapshot.getStatusMessage().contains("HTTP 500"));
    }

    @Test
    @DisplayName("Health poller maps reachable engine with degraded provider to MODULE_DEGRADED with reason")
    void testPollHealth_ModuleDegraded() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        AdminHealthResponse engineHealth = AdminHealthResponse.builder()
                .overallStatus("MODULE_DEGRADED")
                .service("HireAI AI Engine")
                .modules(List.of(
                        ModuleHealthDto.builder().id("jd_generator").name("Job Description Generator").status("DEGRADED").error("Ollama unreachable").build(),
                        ModuleHealthDto.builder().id("match_engine").name("Match Engine").status("HEALTHY").build()
                ))
                .build();

        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(true, engineHealth, "MODULE_DEGRADED", "AI Engine operational but degraded: Job Description Generator (Ollama unreachable)", null));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("MODULE_DEGRADED", snapshot.getOverallStatus());
        assertEquals("MODULE_DEGRADED", snapshot.getStatusReasonCode());
        assertTrue(snapshot.getStatusMessage().contains("Ollama unreachable"));
        assertEquals(0, snapshot.getConsecutiveFailures());
    }

    @Test
    @DisplayName("Health poller immediately recovers to HEALTHY on success after consecutive failures")
    void testPollHealth_ImmediateRecoveryAfterFailures() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        // 2 failures
        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(false, null, "SERVICE_UNREACHABLE", "Connection refused", null));
        pollerService.pollHealth();
        pollerService.pollHealth();
        assertEquals("SERVICE_UNREACHABLE", pollerService.getCachedHealthSnapshot().getOverallStatus());

        // Immediate recovery
        AdminHealthResponse healthyResp = AdminHealthResponse.builder()
                .overallStatus("HEALTHY")
                .service("HireAI AI Engine")
                .modules(List.of(ModuleHealthDto.builder().id("jd_generator").status("HEALTHY").build()))
                .build();
        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(true, healthyResp, "HEALTHY", "AI Engine operational", null));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("HEALTHY", snapshot.getOverallStatus());
        assertEquals(0, snapshot.getConsecutiveFailures());
    }

    @Test
    @DisplayName("Health poller detects database outage and reports DOWN")
    void testPollHealth_DatabaseDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenThrow(new RuntimeException("Connection refused"));
        when(aiEngineClient.checkAdminHealthDetailed()).thenReturn(
                new AiEngineClient.HealthCheckResult(false, null, "SERVICE_UNREACHABLE", "Connection refused", null));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("DOWN", snapshot.getDatabaseStatus());
    }

    @Test
    @DisplayName("getCachedHealthSnapshot() returns cached snapshot without hitting external engine on request")
    void testGetCachedHealthSnapshot_ReadsCacheOnly() {
        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        verifyNoInteractions(aiEngineClient);
    }

    @Test
    @DisplayName("SchedulingConfig provides dedicated ThreadPoolTaskScheduler with admin-health-poller- thread prefix")
    void testDedicatedSchedulerConfiguration() {
        SchedulingConfig config = new SchedulingConfig();
        ThreadPoolTaskScheduler scheduler = config.adminHealthTaskScheduler();
        assertNotNull(scheduler);
        assertEquals("admin-health-poller-", scheduler.getThreadNamePrefix());
        assertEquals(2, scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
    }

    @Test
    @DisplayName("Health poller init executes initial poll on dedicated scheduler rather than commonPool")
    void testInitUsesDedicatedScheduler() {
        ThreadPoolTaskScheduler mockScheduler = org.mockito.Mockito.mock(ThreadPoolTaskScheduler.class);
        AdminHealthPollerService service = new AdminHealthPollerService(aiEngineClient, userRepository, jdbcTemplate, mockScheduler);
        service.init();
        org.mockito.Mockito.verify(mockScheduler, org.mockito.Mockito.times(1)).submit(org.mockito.ArgumentMatchers.any(Runnable.class));
    }
}
