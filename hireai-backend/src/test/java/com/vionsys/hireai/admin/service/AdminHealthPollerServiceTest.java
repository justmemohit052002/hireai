package com.vionsys.hireai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.ModuleHealthDto;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.config.AiEngineProperties;
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

    @BeforeEach
    void setUp() {
        when(aiEngineProperties.getHealthPollRateMs()).thenReturn(45000L);
    }

    @Test
    @DisplayName("Health poller computes UP database status when JDBC query succeeds")
    void testPollHealth_Success() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(userRepository.countActiveUsers()).thenReturn(5L);
        when(userRepository.countTotalUsers()).thenReturn(10L);

        AdminHealthResponse engineHealth = AdminHealthResponse.builder()
                .overallStatus("HEALTHY")
                .service("HireAI AI Engine")
                .modules(List.of(ModuleHealthDto.builder().id("jd_generator").status("HEALTHY").build()))
                .build();

        when(aiEngineClient.getAdminHealthSummary()).thenReturn(Optional.of(engineHealth));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("HEALTHY", snapshot.getOverallStatus());
        assertEquals("UP", snapshot.getDatabaseStatus());
        assertEquals("UP", snapshot.getBackendStatus());
        assertEquals(5L, snapshot.getActiveUsersCount());
        assertFalse(snapshot.getIsStale());
    }

    @Test
    @DisplayName("Health poller handles AI engine failure gracefully and returns fallback snapshot without crashing")
    void testPollHealth_AiEngineDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);
        when(userRepository.countActiveUsers()).thenReturn(3L);
        when(userRepository.countTotalUsers()).thenReturn(8L);

        // AI Engine is unreachable
        when(aiEngineClient.getAdminHealthSummary()).thenReturn(Optional.empty());

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("OFFLINE", snapshot.getOverallStatus());
        assertEquals("AI Engine unreachable", snapshot.getError());
        assertEquals("UP", snapshot.getDatabaseStatus());
        assertNotNull(snapshot.getModules());
        assertEquals(6, snapshot.getModules().size());
    }

    @Test
    @DisplayName("Health poller detects database outage and reports DOWN")
    void testPollHealth_DatabaseDown() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenThrow(new RuntimeException("Connection refused"));

        pollerService.pollHealth();

        AdminHealthResponse snapshot = pollerService.getCachedHealthSnapshot();
        assertNotNull(snapshot);
        assertEquals("DOWN", snapshot.getDatabaseStatus());
    }
}
