package com.vionsys.hireai.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.admin.dto.AdminAuditLogResponse;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminHealthResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.ModuleHealthDto;
import com.vionsys.hireai.admin.dto.ModuleStatDto;
import com.vionsys.hireai.admin.dto.ProviderDetailsDto;
import com.vionsys.hireai.admin.dto.ProviderStatDto;
import com.vionsys.hireai.admin.dto.ProvidersConfigDto;
import com.vionsys.hireai.admin.dto.QuotaUsageDto;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Test
    @DisplayName("GET /admin/health returns 200 with structured health response")
    void testGetHealthSummary() throws Exception {
        AdminHealthResponse healthResponse = AdminHealthResponse.builder()
                .overallStatus("HEALTHY")
                .service("HireAI AI Engine")
                .backendStatus("UP")
                .databaseStatus("UP")
                .activeUsersCount(10L)
                .totalUsersCount(15L)
                .modules(List.of(
                        ModuleHealthDto.builder().id("jd_generator").name("Job Description Generator").status("HEALTHY").build()
                ))
                .build();

        when(adminService.getHealthSummary()).thenReturn(healthResponse);

        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.backendStatus").value("UP"))
                .andExpect(jsonPath("$.data.databaseStatus").value("UP"))
                .andExpect(jsonPath("$.data.activeUsersCount").value(10));
    }

    @Test
    @DisplayName("GET /admin/stats returns 200 with quota and telemetry")
    void testGetStats() throws Exception {
        AdminStatsResponse statsResponse = AdminStatsResponse.builder()
                .totalCalls(50L)
                .todayCalls(12L)
                .weekCalls(35L)
                .failedCalls(2L)
                .successRate(96.0)
                .avgLatencySeconds(1.4)
                .quota(QuotaUsageDto.builder()
                        .dailyLimit(250)
                        .usedToday(12L)
                        .remainingToday(238L)
                        .percentageUsed(4.8)
                        .timezone("Asia/Kolkata")
                        .build())
                .providerDistribution(List.of(
                        ProviderStatDto.builder().provider("ollama").calls(40L).percentage(80.0).avgLatencySeconds(1.2).failedCount(1L).build()
                ))
                .moduleBreakdown(List.of(
                        ModuleStatDto.builder().id("jd_generator").name("JD Generator").calls(25L).percentage(50.0).avgLatencySeconds(1.5).failedCount(1L).build()
                ))
                .build();

        when(adminService.getLlmStats("today")).thenReturn(statsResponse);

        mockMvc.perform(get("/admin/stats?range=today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCalls").value(50))
                .andExpect(jsonPath("$.data.quota.dailyLimit").value(250))
                .andExpect(jsonPath("$.data.quota.timezone").value("Asia/Kolkata"));
    }

    @Test
    @DisplayName("GET /admin/logs returns 200 with paginated call logs")
    void testGetCallLogs() throws Exception {
        AdminCallLogsResponse logsResponse = AdminCallLogsResponse.builder()
                .items(List.of(
                        AdminCallLogEntry.builder()
                                .id("log-uuid-1")
                                .taskType("jd_generation")
                                .provider("ollama")
                                .model("llama3.2:3b")
                                .status("SUCCESS")
                                .latencySeconds(1.1)
                                .promptPreview("Generate Senior Engineer JD")
                                .responsePreview("Here is the job description...")
                                .build()
                ))
                .totalElements(1L)
                .page(1)
                .size(20)
                .totalPages(1)
                .build();

        when(adminService.getCallLogs(any(), any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(logsResponse);

        mockMvc.perform(get("/admin/logs?page=1&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value("log-uuid-1"))
                .andExpect(jsonPath("$.data.items[0].taskType").value("jd_generation"));
    }

    @Test
    @DisplayName("GET /admin/logs/{id} returns 200 with detailed log entry")
    void testGetSingleCallLog() throws Exception {
        AdminCallLogEntry entry = AdminCallLogEntry.builder()
                .id("log-uuid-1")
                .taskType("resume_parsing")
                .provider("gemini")
                .model("gemini-1.5-flash")
                .status("FAILED")
                .errorMessage("Rate limit exceeded on Gemini API")
                .build();

        when(adminService.getCallLogById("log-uuid-1")).thenReturn(entry);

        mockMvc.perform(get("/admin/logs/log-uuid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("log-uuid-1"))
                .andExpect(jsonPath("$.data.errorMessage").value("Rate limit exceeded on Gemini API"));
    }

    @Test
    @DisplayName("GET /admin/providers returns 200 with masked key hints")
    void testGetProvidersStatus() throws Exception {
        ProvidersConfigDto providers = ProvidersConfigDto.builder()
                .activeProvider("ollama")
                .activeModel("llama3.2:3b")
                .ollama(ProviderDetailsDto.builder().name("Ollama (Local)").isConfigured(true).reachable(true).build())
                .gemini(ProviderDetailsDto.builder().name("Google Gemini").isConfigured(true).keyMasked("...a91f").build())
                .build();

        when(adminService.getProvidersStatus()).thenReturn(providers);

        mockMvc.perform(get("/admin/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeProvider").value("ollama"))
                .andExpect(jsonPath("$.data.gemini.keyMasked").value("...a91f"));
    }

    @Test
    @DisplayName("GET /admin/users with pagination returns 200")
    void testGetUsers() throws Exception {
        AdminUserResponse userResponse = AdminUserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .role("ROLE_RECRUITER")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        when(adminService.getUsers(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(userResponse)));

        mockMvc.perform(get("/admin/users?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/role updates user role")
    void testUpdateUserRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_ADMIN);

        AdminUserResponse updated = AdminUserResponse.builder()
                .id(userId)
                .email("user@vionsys.com")
                .role("ROLE_ADMIN")
                .enabled(true)
                .build();

        when(adminService.updateUserRole(eq(userId), any())).thenReturn(updated);

        mockMvc.perform(put("/admin/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("PUT /admin/users/{id}/status updates user account status")
    void testUpdateUserStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setEnabled(false);
        req.setAccountNonLocked(true);

        AdminUserResponse updated = AdminUserResponse.builder()
                .id(userId)
                .email("user@vionsys.com")
                .enabled(false)
                .accountNonLocked(true)
                .build();

        when(adminService.updateUserStatus(eq(userId), any())).thenReturn(updated);

        mockMvc.perform(put("/admin/users/" + userId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("GET /admin/audit returns 200 with paginated audit logs")
    void testGetAuditLogs() throws Exception {
        AdminAuditLogResponse auditLog = AdminAuditLogResponse.builder()
                .id(UUID.randomUUID())
                .actorEmail("admin@vionsys.com")
                .action("UPDATE_ROLE")
                .targetUserId(UUID.randomUUID())
                .oldValue("role=ROLE_CANDIDATE")
                .newValue("role=ROLE_RECRUITER")
                .createdAt(LocalDateTime.now())
                .build();

        when(adminService.getAuditLogs(any()))
                .thenReturn(new PageImpl<>(List.of(auditLog)));

        mockMvc.perform(get("/admin/audit?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].action").value("UPDATE_ROLE"))
                .andExpect(jsonPath("$.data.content[0].actorEmail").value("admin@vionsys.com"));
    }
}
