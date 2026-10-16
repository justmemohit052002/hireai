package com.vionsys.hireai.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vionsys.hireai.auth.entity.EmailVerificationOtp;
import com.vionsys.hireai.auth.repository.EmailVerificationOtpRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.admin.entity.AdminAuditLog;
import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Resume;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.storage.LocalFileStorageService;
import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.chat.entity.DirectConversation;
import com.vionsys.hireai.chat.repository.DirectConversationRepository;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.InvalidOtpException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.notification.repository.NotificationRepository;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.user.dto.ChangePasswordRequest;
import com.vionsys.hireai.user.dto.DeleteAccountRequest;
import com.vionsys.hireai.user.dto.SetPasswordRequest;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.enums.AccountStatus;
import com.vionsys.hireai.user.repository.UserRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ProfilePhotoStorageService photoStorageService;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private LocalFileStorageService fileStorageService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private DirectConversationRepository conversationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private User oauthUser;
    private User localUser;
    private Role candidateRole;

    @BeforeEach
    void setUp() {
        candidateRole = Role.builder().id(1L).name(RoleType.ROLE_CANDIDATE).build();

        oauthUser = User.builder()
                .id(UUID.randomUUID())
                .email("oauth.user@gmail.com")
                .firstName("OAuth")
                .lastName("Candidate")
                .password(null)
                .passwordSet(false)
                .authProvider("GOOGLE")
                .providerId("google-123456")
                .role(candidateRole)
                .enabled(true)
                .build();

        localUser = User.builder()
                .id(UUID.randomUUID())
                .email("local.user@example.com")
                .firstName("Local")
                .lastName("User")
                .password("$2a$10$encodedHash123")
                .passwordSet(true)
                .authProvider("LOCAL")
                .role(candidateRole)
                .enabled(true)
                .build();

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(User user) {
        CustomUserDetails userDetails = CustomUserDetails.fromUser(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // =========================================================================
    // CHANGE PASSWORD TESTS
    // =========================================================================

    @Test
    @DisplayName("changePassword throws IllegalArgumentException for OAuth user who has no password")
    void changePassword_OAuthUserWithoutPassword_ThrowsIllegalArgumentException() {
        mockAuthentication(oauthUser);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("SomeCurrentPass1")
                .newPassword("NewPass123")
                .confirmPassword("NewPass123")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword(request));

        assertTrue(ex.getMessage().contains("does not have a password set"));
        verify(passwordEncoder, never()).matches(anyString(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword succeeds for normal local user with password")
    void changePassword_Success_ForNormalUser() {
        mockAuthentication(localUser);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456")
                .confirmPassword("NewPassword456")
                .build();

        when(passwordEncoder.matches("OldPassword123", localUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPassword456", localUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPassword456")).thenReturn("$2a$10$newHashedPassword456");

        userService.changePassword(request);

        verify(userRepository).save(localUser);
        verify(refreshTokenRepository).revokeAllUserTokens(localUser);
        assertEquals("$2a$10$newHashedPassword456", localUser.getPassword());
        assertTrue(localUser.getPasswordSet());
    }

    @Test
    @DisplayName("changePassword throws BadCredentialsException when current password does not match")
    void changePassword_BadCredentials_WhenCurrentPasswordIncorrect() {
        mockAuthentication(localUser);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123")
                .newPassword("NewPassword456")
                .confirmPassword("NewPassword456")
                .build();

        when(passwordEncoder.matches("WrongPassword123", localUser.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.changePassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    // =========================================================================
    // REQUEST SET PASSWORD OTP TESTS
    // =========================================================================

    @Test
    @DisplayName("requestSetPasswordOtp succeeds and sends OTP to registered email for OAuth-only user")
    void requestSetPasswordOtp_OAuthUserWithoutPassword_Success() {
        mockAuthentication(oauthUser);
        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.empty());

        userService.requestSetPasswordOtp();

        verify(emailVerificationOtpRepository).save(any(EmailVerificationOtp.class));
        verify(emailService).sendSetPasswordOtp(eq(oauthUser), anyString(), eq(10));
    }

    @Test
    @DisplayName("requestSetPasswordOtp rejected for user who already has a password")
    void requestSetPasswordOtp_UserWithPassword_ThrowsIllegalArgumentException() {
        mockAuthentication(localUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.requestSetPasswordOtp());

        assertTrue(ex.getMessage().contains("already has a password set"));
        verify(emailVerificationOtpRepository, never()).save(any());
        verify(emailService, never()).sendSetPasswordOtp(any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("requestSetPasswordOtp throws InvalidOtpException if cooldown is active")
    void requestSetPasswordOtp_RateLimited_ThrowsInvalidOtpException() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp recentOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("111222")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(recentOtp, "createdAt", LocalDateTime.now().minusSeconds(20)); // Only 20s ago

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(recentOtp));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class,
                () -> userService.requestSetPasswordOtp());

        assertTrue(ex.getMessage().contains("Please wait"));
        verify(emailService, never()).sendSetPasswordOtp(any(), anyString(), anyInt());
    }

    // =========================================================================
    // CONFIRM SET PASSWORD TESTS
    // =========================================================================

    @Test
    @DisplayName("confirmSetPassword succeeds, marks passwordSet = true, and revokes tokens")
    void confirmSetPassword_OAuthUser_Success() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp pendingOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("654321")
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(0)
                .build();

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(pendingOtp));
        when(passwordEncoder.encode("SecurePass123")).thenReturn("$2a$10$encodedSecurePass");

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("654321")
                .newPassword("SecurePass123")
                .confirmPassword("SecurePass123")
                .build();

        userService.confirmSetPassword(request);

        assertTrue(pendingOtp.isUsed());
        verify(emailVerificationOtpRepository).save(pendingOtp);
        verify(userRepository).save(oauthUser);
        verify(refreshTokenRepository).revokeAllUserTokens(oauthUser);
        assertEquals("$2a$10$encodedSecurePass", oauthUser.getPassword());
        assertTrue(oauthUser.getPasswordSet());
        assertTrue(oauthUser.hasPassword());
    }

    @Test
    @DisplayName("confirmSetPassword rejects mismatched newPassword and confirmPassword")
    void confirmSetPassword_MismatchedPasswords_ThrowsIllegalArgumentException() {
        mockAuthentication(oauthUser);

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("654321")
                .newPassword("SecurePass123")
                .confirmPassword("DifferentPass123")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.confirmSetPassword(request));

        assertTrue(ex.getMessage().contains("do not match"));
        verify(emailVerificationOtpRepository, never()).findTopByUserAndUsedFalseOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("confirmSetPassword rejects weak password without letters or numbers")
    void confirmSetPassword_WeakPassword_ThrowsIllegalArgumentException() {
        mockAuthentication(oauthUser);

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("654321")
                .newPassword("allletters")
                .confirmPassword("allletters")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.confirmSetPassword(request));

        assertTrue(ex.getMessage().contains("must contain at least one letter and one number"));
    }

    @Test
    @DisplayName("confirmSetPassword throws InvalidOtpException when OTP is expired")
    void confirmSetPassword_ExpiredOtp_ThrowsInvalidOtpException() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp expiredOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("654321")
                .expiryDate(LocalDateTime.now().minusMinutes(1)) // Expired
                .used(false)
                .attempts(0)
                .build();

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(expiredOtp));

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("654321")
                .newPassword("SecurePass123")
                .confirmPassword("SecurePass123")
                .build();

        InvalidOtpException ex = assertThrows(InvalidOtpException.class,
                () -> userService.confirmSetPassword(request));

        assertTrue(ex.getMessage().contains("expired"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("confirmSetPassword increments attempts counter and throws when wrong OTP provided")
    void confirmSetPassword_WrongOtp_IncrementsAttemptsAndThrows() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp pendingOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("654321")
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(1)
                .build();

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(pendingOtp));

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("999999") // Wrong OTP
                .newPassword("SecurePass123")
                .confirmPassword("SecurePass123")
                .build();

        InvalidOtpException ex = assertThrows(InvalidOtpException.class,
                () -> userService.confirmSetPassword(request));

        assertEquals(2, pendingOtp.getAttempts());
        verify(emailVerificationOtpRepository).save(pendingOtp);
        assertTrue(ex.getMessage().contains("Invalid verification code"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("confirmSetPassword throws when attempts reach 5 (brute force protection)")
    void confirmSetPassword_TooManyAttempts_ThrowsInvalidOtpException() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp exhaustedOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("654321")
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(5)
                .build();

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(exhaustedOtp));

        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("654321")
                .newPassword("SecurePass123")
                .confirmPassword("SecurePass123")
                .build();

        InvalidOtpException ex = assertThrows(InvalidOtpException.class,
                () -> userService.confirmSetPassword(request));

        assertTrue(ex.getMessage().contains("Too many incorrect attempts"));
        verify(userRepository, never()).save(any(User.class));
    }

    // =========================================================================
    // DELETE ACCOUNT TESTS
    // =========================================================================

    @Test
    @DisplayName("deleteAccount throws IllegalArgumentException when confirmationText is not DELETE")
    void deleteAccount_InvalidConfirmation_ThrowsException() {
        mockAuthentication(localUser);

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("OldPassword123")
                .confirmationText("NO")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.deleteAccount(request));
        assertTrue(ex.getMessage().contains("Confirmation text must be 'DELETE'"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteAccount throws BadCredentialsException for password user with wrong password")
    void deleteAccount_PasswordUser_WrongPassword_ThrowsBadCredentialsException() {
        mockAuthentication(localUser);

        when(passwordEncoder.matches("WrongPassword", localUser.getPassword())).thenReturn(false);

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("WrongPassword")
                .confirmationText("DELETE")
                .build();

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> userService.deleteAccount(request));
        assertTrue(ex.getMessage().contains("Incorrect current password"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteAccount succeeds for password user and anonymizes data")
    void deleteAccount_PasswordUser_Success() {
        mockAuthentication(localUser);

        when(passwordEncoder.matches("CorrectPassword", localUser.getPassword())).thenReturn(true);

        // Candidate profile present
        Candidate candidate = Candidate.builder()
                .id(UUID.randomUUID())
                .firstName("Local")
                .lastName("User")
                .email("local.user@example.com")
                .user(localUser)
                .profilePhotoPath("uploads/profile-photos/local.jpg")
                .build();
        when(candidateRepository.findByUserId(localUser.getId())).thenReturn(Optional.of(candidate));

        // Resume present
        Resume resume = Resume.builder()
                .id(UUID.randomUUID())
                .candidate(candidate)
                .filePath("uploads/resumes/resume.pdf")
                .build();
        when(resumeRepository.findByCandidateUserIdAndDeletedFalse(localUser.getId())).thenReturn(Optional.of(resume));
        when(conversationRepository.findAllByAnyUserId(localUser.getId())).thenReturn(Collections.emptyList());

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("CorrectPassword")
                .confirmationText("DELETE")
                .build();

        userService.deleteAccount(request);

        // Verify User state
        assertTrue(localUser.getDeleted());
        assertEquals(AccountStatus.DELETED, localUser.getAccountStatus());
        assertFalse(localUser.getEnabled());
        assertFalse(localUser.getAccountNonLocked());
        assertTrue(localUser.getEmail().startsWith("deleted_"));
        assertEquals("Deleted", localUser.getFirstName());
        assertEquals("User", localUser.getLastName());
        assertEquals("DELETED", localUser.getAuthProvider());

        // Verify Candidate state
        assertTrue(candidate.isDeleted());
        assertEquals("Deleted", candidate.getFirstName());
        assertTrue(candidate.getEmail().startsWith("deleted_"));

        // Verify Resume state
        assertTrue(resume.isDeleted());
        assertEquals(ResumeStatus.DELETED, resume.getResumeStatus());

        // Verify sessions revoked and audit logged
        verify(refreshTokenRepository).revokeAllUserTokens(localUser);
        verify(notificationRepository).deleteAllByUserId(localUser.getId());
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
        verify(userRepository).save(localUser);
    }

    @Test
    @DisplayName("deleteAccount throws IllegalStateException if user is last admin")
    void deleteAccount_LastAdmin_ThrowsIllegalStateException() {
        Role adminRole = Role.builder().id(2L).name(RoleType.ROLE_ADMIN).build();
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@hireai.com")
                .role(adminRole)
                .password("AdminPass123!")
                .passwordSet(true)
                .build();

        mockAuthentication(adminUser);
        when(userRepository.countActiveUnlockedAdmins()).thenReturn(1L);

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("AdminPass123!")
                .confirmationText("DELETE")
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.deleteAccount(request));
        assertTrue(ex.getMessage().contains("Cannot delete the last remaining administrator account"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteAccount succeeds for OAuth user with valid OTP")
    void deleteAccount_OAuthUser_Success() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp validOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("987654")
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(0)
                .build();

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(validOtp));
        when(candidateRepository.findByUserId(oauthUser.getId())).thenReturn(Optional.empty());
        when(recruiterProfileRepository.findByUserId(oauthUser.getId())).thenReturn(Optional.empty());
        when(conversationRepository.findAllByAnyUserId(oauthUser.getId())).thenReturn(Collections.emptyList());

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .otp("987654")
                .confirmationText("DELETE")
                .build();

        userService.deleteAccount(request);

        assertTrue(validOtp.isUsed());
        assertTrue(oauthUser.getDeleted());
        assertEquals(AccountStatus.DELETED, oauthUser.getAccountStatus());
        assertFalse(oauthUser.getEnabled());
        verify(refreshTokenRepository).revokeAllUserTokens(oauthUser);
        verify(userRepository).save(oauthUser);
    }

    @Test
    @DisplayName("deleteAccount for recruiter closes all open jobs")
    void deleteAccount_Recruiter_ClosesOpenJobs() {
        Role recruiterRole = Role.builder().id(3L).name(RoleType.ROLE_RECRUITER).build();
        User recruiterUser = User.builder()
                .id(UUID.randomUUID())
                .email("recruiter@company.com")
                .role(recruiterRole)
                .password("$2a$10$encodedHash123")
                .passwordSet(true)
                .build();

        mockAuthentication(recruiterUser);
        when(passwordEncoder.matches("RecruiterPass123", recruiterUser.getPassword())).thenReturn(true);

        RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                .id(UUID.randomUUID())
                .user(recruiterUser)
                .companyName("Test Corp")
                .companyEmail("recruiter@company.com")
                .verified(true)
                .build();

        Job openJob1 = Job.builder().id(UUID.randomUUID()).title("Job 1").status(JobStatus.OPEN).build();
        Job pausedJob2 = Job.builder().id(UUID.randomUUID()).title("Job 2").status(JobStatus.PAUSED).build();
        Job closedJob3 = Job.builder().id(UUID.randomUUID()).title("Job 3").status(JobStatus.CLOSED).build();

        when(recruiterProfileRepository.findByUserId(recruiterUser.getId())).thenReturn(Optional.of(recruiterProfile));
        when(jobRepository.findByRecruiterProfileId(recruiterProfile.getId())).thenReturn(List.of(openJob1, pausedJob2, closedJob3));
        when(conversationRepository.findAllByAnyUserId(recruiterUser.getId())).thenReturn(Collections.emptyList());

        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("RecruiterPass123")
                .confirmationText("DELETE")
                .build();

        userService.deleteAccount(request);

        assertEquals(JobStatus.CLOSED, openJob1.getStatus());
        assertEquals(JobStatus.CLOSED, pausedJob2.getStatus());
        assertEquals(JobStatus.CLOSED, closedJob3.getStatus());
        assertFalse(recruiterProfile.getVerified());
        assertTrue(recruiterUser.getDeleted());
        verify(jobRepository).save(openJob1);
        verify(jobRepository).save(pausedJob2);
    }

    @Test
    @DisplayName("requestDeleteAccountOtp throws IllegalArgumentException if user already has a password")
    void requestDeleteAccountOtp_PasswordUser_ThrowsException() {
        mockAuthentication(localUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.requestDeleteAccountOtp());
        assertTrue(ex.getMessage().contains("Account has a password configured"));
        verify(emailService, never()).sendDeleteAccountOtp(any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("requestDeleteAccountOtp succeeds for OAuth user and dispatches email")
    void requestDeleteAccountOtp_OAuthUser_Success() {
        mockAuthentication(oauthUser);

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.empty());

        userService.requestDeleteAccountOtp();

        verify(emailVerificationOtpRepository).save(any(EmailVerificationOtp.class));
        verify(emailService).sendDeleteAccountOtp(eq(oauthUser), anyString(), eq(10));
    }

    @Test
    @DisplayName("requestDeleteAccountOtp respects 60s cooldown")
    void requestDeleteAccountOtp_CooldownActive_ThrowsException() {
        mockAuthentication(oauthUser);

        EmailVerificationOtp recentOtp = EmailVerificationOtp.builder()
                .user(oauthUser)
                .otpCode("112233")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        ReflectionTestUtils.setField(recentOtp, "createdAt", LocalDateTime.now().minusSeconds(20));

        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthUser))
                .thenReturn(Optional.of(recentOtp));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class,
                () -> userService.requestDeleteAccountOtp());

        assertTrue(ex.getMessage().contains("Please wait"));
        verify(emailService, never()).sendDeleteAccountOtp(any(), anyString(), anyInt());
    }
}
