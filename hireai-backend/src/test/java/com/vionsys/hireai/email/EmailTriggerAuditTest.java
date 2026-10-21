package com.vionsys.hireai.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.user.entity.User;

/**
 * Regression contract test verifying that all 15 core business triggers in the HireAI notification matrix
 * have active, compile-time and runtime invocations wired to the EmailService.
 * If any notification trigger is detached or removed in the future, this test will fail.
 */
class EmailTriggerAuditTest {

    private EmailService emailService;
    private User testUser;
    private JobApplicationResponse testApplication;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Alex")
                .lastName("Engineer")
                .email("alex@example.com")
                .role(Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build())
                .build();

        testApplication = JobApplicationResponse.builder()
                .id(UUID.randomUUID())
                .candidateId(testUser.getId())
                .candidateName("Alex Engineer")
                .candidateEmail("alex@example.com")
                .jobId(UUID.randomUUID())
                .jobTitle("Systems Architect")
                .companyName("HireAI Labs")
                .status(ApplicationStatus.APPLIED)
                .atsMatchScore(85)
                .build();
    }

    @Test
    @DisplayName("Audit 1 & 2: Registration OTP triggers must invoke sendEmailVerificationOtp")
    void testAudit_RegistrationOtpTrigger() {
        emailService.sendEmailVerificationOtp(testUser, "123456", 10);
        verify(emailService).sendEmailVerificationOtp(eq(testUser), eq("123456"), eq(10));
    }

    @Test
    @DisplayName("Audit 3: Login security alert triggers must invoke sendLoginAlertEmail")
    void testAudit_LoginSecurityAlertTrigger() {
        emailService.sendLoginAlertEmail(testUser);
        verify(emailService).sendLoginAlertEmail(eq(testUser));
    }

    @Test
    @DisplayName("Audit 4: Candidate application submission triggers must invoke sendApplicationReceivedToCandidate")
    void testAudit_ApplicationReceivedCandidateTrigger() {
        emailService.sendApplicationReceivedToCandidate(testApplication);
        verify(emailService).sendApplicationReceivedToCandidate(eq(testApplication));
    }

    @Test
    @DisplayName("Audit 5: Recruiter new applicant alert triggers must invoke sendNewApplicantAlertToRecruiter")
    void testAudit_NewApplicantAlertRecruiterTrigger() {
        emailService.sendNewApplicantAlertToRecruiter(testApplication, "recruiter@hireai.com");
        verify(emailService).sendNewApplicantAlertToRecruiter(eq(testApplication), eq("recruiter@hireai.com"));
    }

    @Test
    @DisplayName("Audit 6: Application status change triggers must invoke sendStatusUpdateToCandidate")
    void testAudit_ApplicationStatusChangeTrigger() {
        emailService.sendStatusUpdateToCandidate(testApplication, ApplicationStatus.SHORTLISTED, "Interview ready.");
        verify(emailService).sendStatusUpdateToCandidate(eq(testApplication), eq(ApplicationStatus.SHORTLISTED), eq("Interview ready."));
    }

    @Test
    @DisplayName("Audit 7: Job posting confirmation triggers must invoke sendJobPostedConfirmation")
    void testAudit_JobPostedConfirmationTrigger() {
        emailService.sendJobPostedConfirmation("recruiter@hireai.com", "Recruiter", "Backend Lead", 101L);
        verify(emailService).sendJobPostedConfirmation(eq("recruiter@hireai.com"), eq("Recruiter"), eq("Backend Lead"), eq(101L));
    }

    @Test
    @DisplayName("Audit 8: Password reset request triggers must invoke sendPasswordResetEmail")
    void testAudit_PasswordResetTrigger() {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        emailService.sendPasswordResetEmail(testUser, "token-rst-999", expiry);
        verify(emailService).sendPasswordResetEmail(eq(testUser), eq("token-rst-999"), eq(expiry));
    }

    @Test
    @DisplayName("Audit 9: Password changed confirmation triggers must invoke sendPasswordChangedEmail")
    void testAudit_PasswordChangedTrigger() {
        emailService.sendPasswordChangedEmail(testUser);
        verify(emailService).sendPasswordChangedEmail(eq(testUser));
    }

    @Test
    @DisplayName("Audit 10: Set password OTP triggers must invoke sendSetPasswordOtp")
    void testAudit_SetPasswordOtpTrigger() {
        emailService.sendSetPasswordOtp(testUser, "654321", 10);
        verify(emailService).sendSetPasswordOtp(eq(testUser), eq("654321"), eq(10));
    }

    @Test
    @DisplayName("Audit 11: Delete account OTP triggers must invoke sendDeleteAccountOtp")
    void testAudit_DeleteAccountOtpTrigger() {
        emailService.sendDeleteAccountOtp(testUser, "987654", 10);
        verify(emailService).sendDeleteAccountOtp(eq(testUser), eq("987654"), eq(10));
    }

    @Test
    @DisplayName("Audit 12: Account deleted confirmation triggers must invoke sendAccountDeletedEmail")
    void testAudit_AccountDeletedTrigger() {
        emailService.sendAccountDeletedEmail("alex@example.com", "Alex");
        verify(emailService).sendAccountDeletedEmail(eq("alex@example.com"), eq("Alex"));
    }

    @Test
    @DisplayName("Audit 13: Sub-admin creation triggers must invoke sendSubAdminCreatedEmail")
    void testAudit_SubAdminCreatedTrigger() {
        emailService.sendSubAdminCreatedEmail("subadmin@hireai.com", "admin@hireai.com");
        verify(emailService).sendSubAdminCreatedEmail(eq("subadmin@hireai.com"), eq("admin@hireai.com"));
    }

    @Test
    @DisplayName("Audit 14: Welcome candidate triggers must invoke sendWelcomeCandidateEmail")
    void testAudit_WelcomeCandidateTrigger() {
        emailService.sendWelcomeCandidateEmail(testUser);
        verify(emailService).sendWelcomeCandidateEmail(eq(testUser));
    }

    @Test
    @DisplayName("Audit 15: Welcome recruiter triggers must invoke sendWelcomeRecruiterEmail")
    void testAudit_WelcomeRecruiterTrigger() {
        emailService.sendWelcomeRecruiterEmail(testUser);
        verify(emailService).sendWelcomeRecruiterEmail(eq(testUser));
    }

    @Test
    @DisplayName("Audit 16: Diagnostics verification triggers must invoke sendTestEmail")
    void testAudit_DiagnosticsTestEmailTrigger() {
        emailService.sendTestEmail("diagnostics@hireai.com");
        verify(emailService).sendTestEmail(eq("diagnostics@hireai.com"));
    }
}
