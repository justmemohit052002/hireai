package com.vionsys.hireai.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vionsys.hireai.admin.dto.AdminCallLogEntry;
import com.vionsys.hireai.admin.dto.AdminCallLogsResponse;
import com.vionsys.hireai.admin.dto.AdminStatsResponse;
import com.vionsys.hireai.admin.dto.AdminUserResponse;
import com.vionsys.hireai.admin.dto.UpdateUserRoleRequest;
import com.vionsys.hireai.admin.dto.UpdateUserStatusRequest;
import com.vionsys.hireai.admin.entity.AdminAuditLog;
import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.admin.service.impl.AdminServiceImpl;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.AdminConflictException;
import com.vionsys.hireai.exception.AiCallLogNotFoundException;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private AdminHealthPollerService healthPollerService;

    @Mock
    private AiEngineClient aiEngineClient;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User otherAdminUser;
    private User candidateUser;
    private Role adminRole;
    private Role recruiterRole;
    private Role candidateRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name(RoleType.ROLE_ADMIN).build();
        recruiterRole = Role.builder().id(2L).name(RoleType.ROLE_RECRUITER).build();
        candidateRole = Role.builder().id(3L).name(RoleType.ROLE_CANDIDATE).build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@vionsys.com")
                .firstName("Admin")
                .lastName("User")
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$hashedPasswordHere")
                .build();

        otherAdminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin2@vionsys.com")
                .firstName("Second")
                .lastName("Admin")
                .role(adminRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$anotherHashedPassword")
                .build();

        candidateUser = User.builder()
                .id(UUID.randomUUID())
                .email("candidate@example.com")
                .firstName("Candidate")
                .lastName("User")
                .role(candidateRole)
                .enabled(true)
                .accountNonLocked(true)
                .password("$2a$10$candidatePassword")
                .build();

        // Mock current authenticated user as admin@vionsys.com
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@vionsys.com", "password")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // 1. Self-Protection Tests
    // ==========================================

    @Test
    @DisplayName("Admin attempting self-demotion throws AdminConflictException (409)")
    void testSelfDemotion_ThrowsConflictException() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_RECRUITER);

        AdminConflictException ex = assertThrows(AdminConflictException.class, () ->
                adminService.updateUserRole(adminUser.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Admins cannot revoke their own administrator privileges"));
    }

    @Test
    @DisplayName("Admin attempting self-deactivation throws AdminConflictException (409)")
    void testSelfDeactivation_ThrowsConflictException() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setEnabled(false);

        AdminConflictException ex = assertThrows(AdminConflictException.class, () ->
                adminService.updateUserStatus(adminUser.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Admins cannot deactivate their own user account"));
    }

    @Test
    @DisplayName("Admin attempting to self-lock throws AdminConflictException (409)")
    void testSelfLock_ThrowsConflictException() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));

        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setAccountNonLocked(false);

        AdminConflictException ex = assertThrows(AdminConflictException.class, () ->
                adminService.updateUserStatus(adminUser.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Admins cannot lock their own user account"));
    }

    // ==========================================
    // 2. Last-Admin Protection Tests
    // ==========================================

    @Test
    @DisplayName("Demoting the last active unlocked admin throws AdminConflictException (409)")
    void testDemoteLastAdmin_ThrowsConflictException() {
        // Authenticated as a separate service actor, attempting to demote the only remaining admin
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("superadmin@vionsys.com", "password")
        );

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveUnlockedAdmins()).thenReturn(1L);

        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_RECRUITER);

        AdminConflictException ex = assertThrows(AdminConflictException.class, () ->
                adminService.updateUserRole(adminUser.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Cannot modify role of the last active administrator account"));
    }

    @Test
    @DisplayName("Deactivating or locking the last active unlocked admin throws AdminConflictException (409)")
    void testDeactivateLastAdmin_ThrowsConflictException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("superadmin@vionsys.com", "password")
        );

        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveUnlockedAdmins()).thenReturn(1L);

        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setEnabled(false);

        AdminConflictException ex = assertThrows(AdminConflictException.class, () ->
                adminService.updateUserStatus(adminUser.getId(), req)
        );
        assertTrue(ex.getMessage().contains("Cannot deactivate or lock the last active administrator account"));
    }

    // ==========================================
    // 3. Audit Log Creation Tests
    // ==========================================

    @Test
    @DisplayName("Updating user role persists an AdminAuditLog record")
    void testUpdateUserRole_PersistsAuditLog() {
        when(userRepository.findById(candidateUser.getId())).thenReturn(Optional.of(candidateUser));
        when(roleRepository.findByName(RoleType.ROLE_RECRUITER)).thenReturn(Optional.of(recruiterRole));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRoleRequest req = new UpdateUserRoleRequest();
        req.setRole(RoleType.ROLE_RECRUITER);

        AdminUserResponse response = adminService.updateUserRole(candidateUser.getId(), req);
        assertEquals("ROLE_RECRUITER", response.getRole());

        verify(refreshTokenRepository).revokeAllUserTokens(candidateUser);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AdminAuditLog log = captor.getValue();
        assertEquals("UPDATE_ROLE", log.getAction());
        assertEquals("admin@vionsys.com", log.getActorEmail());
        assertEquals(candidateUser.getId(), log.getTargetUserId());
        assertTrue(log.getOldValue().contains("ROLE_CANDIDATE"));
        assertTrue(log.getNewValue().contains("ROLE_RECRUITER"));
    }

    @Test
    @DisplayName("Updating user status persists an AdminAuditLog record and revokes tokens if deactivated")
    void testUpdateUserStatus_PersistsAuditLog() {
        when(userRepository.findById(candidateUser.getId())).thenReturn(Optional.of(candidateUser));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setEnabled(false);

        AdminUserResponse response = adminService.updateUserStatus(candidateUser.getId(), req);
        assertFalse(response.getEnabled());

        verify(refreshTokenRepository).revokeAllUserTokens(candidateUser);

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AdminAuditLog log = captor.getValue();
        assertEquals("UPDATE_STATUS", log.getAction());
        assertEquals("admin@vionsys.com", log.getActorEmail());
        assertEquals(candidateUser.getId(), log.getTargetUserId());
        assertTrue(log.getNewValue().contains("enabled=false"));
    }

    @Test
    @DisplayName("getLlmStats returns available=false and message when AI engine is down")
    void testGetLlmStats_AiEngineDown_ReturnsAvailableFalse() {
        when(aiEngineClient.getAdminStats("today")).thenReturn(Optional.empty());

        AdminStatsResponse res = adminService.getLlmStats("today");
        assertNotNull(res);
        assertFalse(res.getAvailable());
        assertEquals("AI engine unreachable", res.getMessage());
        assertEquals(0L, res.getTotalCalls());
        assertEquals(0.0, res.getSuccessRate());
        assertNull(res.getQuota(), "Fallback quota must be null, not hardcoded data");
    }

    @Test
    @DisplayName("getLlmStats returns available=true when AI engine is up")
    void testGetLlmStats_AiEngineUp_ReturnsAvailableTrue() {
        AdminStatsResponse engineStats = AdminStatsResponse.builder()
                .totalCalls(42L)
                .successRate(98.5)
                .build();
        when(aiEngineClient.getAdminStats("today")).thenReturn(Optional.of(engineStats));

        AdminStatsResponse res = adminService.getLlmStats("today");
        assertNotNull(res);
        assertTrue(res.getAvailable());
        assertEquals(42L, res.getTotalCalls());
    }

    @Test
    @DisplayName("getCallLogs records VIEW_CALL_LOGS audit log and returns available=false when engine is down")
    void testGetCallLogs_AiEngineDown_RecordsAuditLogAndReturnsUnavailable() {
        when(aiEngineClient.getAdminLogs(any(), any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(Optional.empty());

        AdminCallLogsResponse res = adminService.getCallLogs(
                "senior", "jd_generation", "SUCCESS", "ollama", "2026-01-01", "2026-01-02", "corr-1", "cand-1", 1, 20);

        assertNotNull(res);
        assertFalse(res.getAvailable());
        assertEquals("AI engine unreachable", res.getMessage());
        assertEquals(0L, res.getTotalElements());

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog auditLog = captor.getValue();
        assertEquals("VIEW_CALL_LOGS", auditLog.getAction());
        assertEquals("admin@vionsys.com", auditLog.getActorEmail());
        assertNull(auditLog.getTargetUserId());
        assertTrue(auditLog.getNewValue().contains("query=senior"));
        assertTrue(auditLog.getNewValue().contains("taskType=jd_generation"));
    }

    @Test
    @DisplayName("getCallLogById throws AiCallLogNotFoundException when log does not exist")
    void testGetCallLogById_NotFound_ThrowsAiCallLogNotFoundException() {
        when(aiEngineClient.getAdminLogById("missing-id")).thenReturn(Optional.empty());

        assertThrows(AiCallLogNotFoundException.class, () ->
                adminService.getCallLogById("missing-id")
        );
    }

    @Test
    @DisplayName("getCallLogById records VIEW_CALL_LOG audit log with logId and NEVER stores prompt content")
    void testGetCallLogById_Success_RecordsAuditWithoutPrompt() {
        AdminCallLogEntry logEntry = AdminCallLogEntry.builder()
                .id("log-999")
                .taskType("interview_ai")
                .promptPreview("Sensitive user resume text and personal prompt")
                .responsePreview("Sensitive AI response output")
                .build();
        when(aiEngineClient.getAdminLogById("log-999")).thenReturn(Optional.of(logEntry));

        AdminCallLogEntry result = adminService.getCallLogById("log-999");
        assertNotNull(result);
        assertEquals("log-999", result.getId());

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AdminAuditLog auditLog = captor.getValue();
        assertEquals("VIEW_CALL_LOG", auditLog.getAction());
        assertEquals("admin@vionsys.com", auditLog.getActorEmail());
        assertNull(auditLog.getTargetUserId());
        assertTrue(auditLog.getNewValue().contains("logId=log-999"));
        assertFalse(auditLog.getNewValue().contains("Sensitive user resume text"));
        assertFalse(auditLog.getNewValue().contains("Sensitive AI response output"));
    }

    // ==========================================
    // 4. Privacy & Secrets Hygiene Verification
    // ==========================================

    @Test
    @DisplayName("AdminUserResponse serialization contains NO password hashes, tokens, or sensitive fields")
    void testAdminUserResponse_ContainsNoSensitiveFields() throws Exception {
        AdminUserResponse res = AdminUserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("9999999999")
                .role("ROLE_RECRUITER")
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(res);

        assertFalse(json.contains("password"), "JSON must never contain 'password'");
        assertFalse(json.contains("hash"), "JSON must never contain password hash");
        assertFalse(json.contains("token"), "JSON must never contain auth token");
        assertFalse(json.contains("secret"), "JSON must never contain secret");
    }
}
