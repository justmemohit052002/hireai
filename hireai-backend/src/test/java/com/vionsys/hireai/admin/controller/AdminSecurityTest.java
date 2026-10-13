package com.vionsys.hireai.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.service.AdminService;
import com.vionsys.hireai.common.enums.RoleType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    // ==========================================
    // 1. Unauthenticated Requests -> 401
    // ==========================================

    @Test
    @DisplayName("GET /admin/health without token returns 401")
    void testGetHealth_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/stats without token returns 401")
    void testGetStats_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/logs without token returns 401")
    void testGetLogs_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/users without token returns 401")
    void testGetUsers_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/audit without token returns 401")
    void testGetAudit_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 2. Candidate Token -> 403
    // ==========================================

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    @DisplayName("GET /admin/health with CANDIDATE role returns 403")
    void testGetHealth_CandidateRole_Returns403() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    @DisplayName("GET /admin/users with CANDIDATE role returns 403")
    void testGetUsers_CandidateRole_Returns403() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    @DisplayName("PUT /admin/users/{id}/role with CANDIDATE role returns 403")
    void testPutUserRole_CandidateRole_Returns403() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_ADMIN);

        mockMvc.perform(put("/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. Recruiter Token -> 403
    // ==========================================

    @Test
    @WithMockUser(username = "recruiter@vionsys.com", roles = {"RECRUITER"})
    @DisplayName("GET /admin/health with RECRUITER role returns 403")
    void testGetHealth_RecruiterRole_Returns403() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter@vionsys.com", roles = {"RECRUITER"})
    @DisplayName("GET /admin/stats with RECRUITER role returns 403")
    void testGetStats_RecruiterRole_Returns403() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter@vionsys.com", roles = {"RECRUITER"})
    @DisplayName("GET /admin/audit with RECRUITER role returns 403")
    void testGetAudit_RecruiterRole_Returns403() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 4. Admin Token -> 200
    // ==========================================

    @Test
    @WithMockUser(username = "admin@vionsys.com", roles = {"ADMIN"})
    @DisplayName("GET /admin/health with ADMIN role returns 200")
    void testGetHealth_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@vionsys.com", roles = {"ADMIN"})
    @DisplayName("GET /admin/stats with ADMIN role returns 200")
    void testGetStats_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@vionsys.com", roles = {"ADMIN"})
    @DisplayName("GET /admin/providers with ADMIN role returns 200")
    void testGetProviders_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/providers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@vionsys.com", roles = {"ADMIN"})
    @DisplayName("GET /admin/audit with ADMIN role returns 200")
    void testGetAudit_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk());
    }
}
