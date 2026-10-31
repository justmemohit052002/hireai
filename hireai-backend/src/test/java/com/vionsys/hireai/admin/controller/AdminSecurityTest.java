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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @Autowired
    private com.vionsys.hireai.security.jwt.JwtService jwtService;

    @Autowired
    private com.vionsys.hireai.user.repository.UserRepository userRepository;

    @Autowired
    private com.vionsys.hireai.role.repository.RoleRepository roleRepository;

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

    @Test
    @WithMockUser(username = "admin@vionsys.com", roles = {"ADMIN"})
    @DisplayName("GET /admin/platform-metrics with ADMIN role returns 200")
    void testGetPlatformMetrics_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/platform-metrics"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 5. Sub-Admin Role Permissions & Restrictions
    // ==========================================

    @Test
    @WithMockUser(username = "subadmin@vionsys.com", roles = {"SUB_ADMIN"})
    @DisplayName("GET /admin/health with SUB_ADMIN role returns 200")
    void testGetHealth_SubAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "subadmin@vionsys.com", roles = {"SUB_ADMIN"})
    @DisplayName("GET /admin/platform-metrics with SUB_ADMIN role returns 200")
    void testGetPlatformMetrics_SubAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/platform-metrics"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "subadmin@vionsys.com", roles = {"SUB_ADMIN"})
    @DisplayName("GET /admin/users with SUB_ADMIN role returns 200")
    void testGetUsers_SubAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "subadmin@vionsys.com", roles = {"SUB_ADMIN"})
    @DisplayName("PUT /admin/users/{id}/role with SUB_ADMIN role is forbidden (403)")
    void testPutUserRole_SubAdminRole_Returns403() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_RECRUITER);

        mockMvc.perform(put("/admin/users/" + UUID.randomUUID() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "subadmin@vionsys.com", roles = {"SUB_ADMIN"})
    @DisplayName("POST /admin/sub-admins with SUB_ADMIN role is forbidden (403)")
    void testCreateSubAdmin_SubAdminRole_Returns403() throws Exception {
        com.vionsys.hireai.admin.dto.CreateSubAdminRequest req = com.vionsys.hireai.admin.dto.CreateSubAdminRequest.builder()
                .firstName("New")
                .lastName("SubAdmin")
                .email("newsub@vionsys.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/sub-admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 6. Immediate Access Token Invalidation Tests
    // ==========================================

    @Test
    @DisplayName("Deactivated user access token immediately stops working (returns 401)")
    void testDeactivatedUser_TokenImmediatelyInvalidated() throws Exception {
        com.vionsys.hireai.role.entity.Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(com.vionsys.hireai.role.entity.Role.builder().name(RoleType.ROLE_ADMIN).build()));

        com.vionsys.hireai.user.entity.User testUser = com.vionsys.hireai.user.entity.User.builder()
                .email("test-deact-" + UUID.randomUUID() + "@vionsys.com")
                .firstName("Active")
                .lastName("Admin")
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$hashedPasswordHere")
                .build();
        testUser = userRepository.save(testUser);

        com.vionsys.hireai.security.CustomUserDetails userDetails =
                com.vionsys.hireai.security.CustomUserDetails.fromUser(testUser);
        String token = jwtService.generateAccessToken(userDetails);

        // When enabled, token authenticates successfully
        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Deactivate user in database
        testUser.setEnabled(false);
        userRepository.save(testUser);

        // Same access token must immediately fail with 401 Unauthorized
        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Locked user access token immediately stops working (returns 401)")
    void testLockedUser_TokenImmediatelyInvalidated() throws Exception {
        com.vionsys.hireai.role.entity.Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(com.vionsys.hireai.role.entity.Role.builder().name(RoleType.ROLE_ADMIN).build()));

        com.vionsys.hireai.user.entity.User testUser = com.vionsys.hireai.user.entity.User.builder()
                .email("test-lock-" + UUID.randomUUID() + "@vionsys.com")
                .firstName("Locked")
                .lastName("Admin")
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$hashedPasswordHere")
                .build();
        testUser = userRepository.save(testUser);

        com.vionsys.hireai.security.CustomUserDetails userDetails =
                com.vionsys.hireai.security.CustomUserDetails.fromUser(testUser);
        String token = jwtService.generateAccessToken(userDetails);

        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Lock user in database
        testUser.setAccountNonLocked(false);
        userRepository.save(testUser);

        // Same access token must immediately fail with 401 Unauthorized
        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User whose role is changed has old access token immediately invalidated (returns 401)")
    void testRoleChanged_TokenImmediatelyInvalidated() throws Exception {
        com.vionsys.hireai.role.entity.Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(com.vionsys.hireai.role.entity.Role.builder().name(RoleType.ROLE_ADMIN).build()));
        com.vionsys.hireai.role.entity.Role recruiterRole = roleRepository.findByName(RoleType.ROLE_RECRUITER)
                .orElseGet(() -> roleRepository.save(com.vionsys.hireai.role.entity.Role.builder().name(RoleType.ROLE_RECRUITER).build()));

        com.vionsys.hireai.user.entity.User testUser = com.vionsys.hireai.user.entity.User.builder()
                .email("test-rolechange-" + UUID.randomUUID() + "@vionsys.com")
                .firstName("Demoted")
                .lastName("Admin")
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$hashedPasswordHere")
                .build();
        testUser = userRepository.save(testUser);

        com.vionsys.hireai.security.CustomUserDetails userDetails =
                com.vionsys.hireai.security.CustomUserDetails.fromUser(testUser);
        String token = jwtService.generateAccessToken(userDetails);

        // Active admin token succeeds
        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Demote user to recruiter in database (explicitly remove admin role from roles set)
        testUser.getRoles().removeIf(r -> r.getName() == RoleType.ROLE_ADMIN);
        testUser.setRole(recruiterRole);
        userRepository.save(testUser);

        // Old admin access token must immediately fail with 401 Unauthorized (token role does not match user DB role)
        mockMvc.perform(get("/admin/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
