package com.vionsys.hireai.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.vionsys.hireai.admin.repository.AdminAuditLogRepository;
import com.vionsys.hireai.auth.dto.VerifyEmailOtpRequest;
import com.vionsys.hireai.auth.entity.EmailVerificationOtp;
import com.vionsys.hireai.auth.repository.EmailVerificationOtpRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.storage.LocalFileStorageService;
import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.chat.repository.DirectConversationRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.InvalidOtpException;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.notification.repository.NotificationRepository;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.security.ratelimit.AccountBackoffManager;
import com.vionsys.hireai.user.dto.DeleteAccountRequest;
import com.vionsys.hireai.user.dto.SetPasswordRequest;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;
import com.vionsys.hireai.user.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class OtpSecurityVerificationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AccountBackoffManager accountBackoffManager;

    @Mock
    private com.vionsys.hireai.security.jwt.JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ProfilePhotoStorageService profilePhotoStorageService;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private LocalFileStorageService localFileStorageService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private DirectConversationRepository directConversationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @InjectMocks
    private AuthService authService;

    @InjectMocks
    private UserServiceImpl userService;

    private User unverifiedCandidate;
    private User oauthCandidateWithoutPassword;
    private EmailVerificationOtp activeOtp;

    @BeforeEach
    void setUp() {
        Role candidateRole = Role.builder()
                .id(1L)
                .name(RoleType.ROLE_CANDIDATE)
                .build();

        unverifiedCandidate = User.builder()
                .id(UUID.randomUUID())
                .email("unverified.candidate@example.com")
                .firstName("Unverified")
                .lastName("Candidate")
                .password("encodedSecret")
                .enabled(false)
                .accountNonLocked(true)
                .role(candidateRole)
                .build();

        oauthCandidateWithoutPassword = User.builder()
                .id(UUID.randomUUID())
                .email("oauth.candidate@example.com")
                .firstName("OAuth")
                .lastName("User")
                .password(null)
                .enabled(true)
                .accountNonLocked(true)
                .role(candidateRole)
                .authProvider("GOOGLE")
                .build();

        activeOtp = EmailVerificationOtp.builder()
                .id(UUID.randomUUID())
                .user(unverifiedCandidate)
                .otpCode("842109")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
    }

    @Test
    @DisplayName("VerifyEmailOtp limits wrong attempts to 5, then invalidates code")
    void testVerifyEmailOtp_Exceeding5Attempts_LocksAndInvalidatesOtp() {
        when(userRepository.findByEmail("unverified.candidate@example.com"))
                .thenReturn(Optional.of(unverifiedCandidate));
        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(unverifiedCandidate))
                .thenReturn(Optional.of(activeOtp));

        // Attempts 1 to 4 should record failure, increment attempts, and inform remaining tries
        for (int i = 1; i <= 4; i++) {
            final int currentAttempt = i;
            VerifyEmailOtpRequest req = VerifyEmailOtpRequest.builder()
                    .email("unverified.candidate@example.com")
                    .otp("00000" + currentAttempt)
                    .build();

            InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> authService.verifyEmailOtp(req));
            assertTrue(ex.getMessage().contains((5 - currentAttempt) + " attempt(s) remaining"));
            assertEquals(currentAttempt, activeOtp.getAttempts());
            assertFalse(activeOtp.isUsed(), "OTP should remain active until 5 attempts are reached");
        }

        // 5th failed attempt invalidates the OTP code (used = true)
        VerifyEmailOtpRequest fifthReq = VerifyEmailOtpRequest.builder()
                .email("unverified.candidate@example.com")
                .otp("000005")
                .build();

        InvalidOtpException ex5 = assertThrows(InvalidOtpException.class, () -> authService.verifyEmailOtp(fifthReq));
        assertTrue(ex5.getMessage().contains("Too many incorrect attempts"));
        assertEquals(5, activeOtp.getAttempts());
        assertTrue(activeOtp.isUsed(), "OTP must be invalidated immediately upon reaching 5 failures");
        verify(emailVerificationOtpRepository, atLeast(5)).save(activeOtp);

        // A 6th attempt, even with the REAL OTP code "842109", must be rejected because the code is locked
        VerifyEmailOtpRequest sixthReq = VerifyEmailOtpRequest.builder()
                .email("unverified.candidate@example.com")
                .otp("842109")
                .build();

        InvalidOtpException ex6 = assertThrows(InvalidOtpException.class, () -> authService.verifyEmailOtp(sixthReq));
        assertTrue(ex6.getMessage().contains("Too many incorrect attempts"));
    }

    @Test
    @DisplayName("Brute-force of a 6-digit code against one account across 10 spoofed IPs is invalidated after 5 attempts")
    void testBruteForceAgainstOneAccountAcrossSpoofedIpsFailsAfter5Attempts() {
        when(userRepository.findByEmail("unverified.candidate@example.com"))
                .thenReturn(Optional.of(unverifiedCandidate));
        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(unverifiedCandidate))
                .thenReturn(Optional.of(activeOtp));

        // Attacker attempts 10 guesses, spoofing different client IPs on each HTTP request
        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            String spoofedIp = "198.51.100." + i;
            servletRequest.setRemoteAddr("10.0.0.1"); // Direct peer
            servletRequest.addHeader("X-Forwarded-For", spoofedIp);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

            try {
                String guess = String.format("%06d", i * 11111);
                VerifyEmailOtpRequest req = VerifyEmailOtpRequest.builder()
                        .email("unverified.candidate@example.com")
                        .otp(guess)
                        .build();

                InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> authService.verifyEmailOtp(req));
                if (i <= 4) {
                    assertTrue(ex.getMessage().contains("attempt(s) remaining"));
                } else {
                    assertTrue(ex.getMessage().contains("Too many incorrect attempts"));
                }
            } finally {
                RequestContextHolder.resetRequestAttributes();
            }
        }

        // Even across 10 distinct spoofed IPs, the single OTP entity reached 5 attempts and was marked used
        assertTrue(activeOtp.isUsed(), "OTP must be marked used despite IP spoofing attempts");
        assertTrue(activeOtp.getAttempts() >= 5);
    }

    @Test
    @DisplayName("Expired OTP cannot be verified")
    void testExpiredOtpIsRejected() {
        activeOtp.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // Expired
        when(userRepository.findByEmail("unverified.candidate@example.com"))
                .thenReturn(Optional.of(unverifiedCandidate));
        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(unverifiedCandidate))
                .thenReturn(Optional.of(activeOtp));

        VerifyEmailOtpRequest req = VerifyEmailOtpRequest.builder()
                .email("unverified.candidate@example.com")
                .otp("842109")
                .build();

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> authService.verifyEmailOtp(req));
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Single-use OTP cannot be reused after successful verification")
    void testSingleUseOtpCannotBeReused() {
        when(userRepository.findByEmail("unverified.candidate@example.com"))
                .thenReturn(Optional.of(unverifiedCandidate));
        when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(unverifiedCandidate))
                .thenReturn(Optional.of(activeOtp));
        when(jwtService.generateAccessToken(any())).thenReturn("valid-jwt-token");
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        VerifyEmailOtpRequest req = VerifyEmailOtpRequest.builder()
                .email("unverified.candidate@example.com")
                .otp("842109")
                .build();

        authService.verifyEmailOtp(req);

        // Verification succeeded: OTP must be marked used
        assertTrue(activeOtp.isUsed(), "OTP must be marked used after successful verification");
        assertTrue(Boolean.TRUE.equals(unverifiedCandidate.getEnabled()), "User must be enabled after successful verification");

        // When user is enabled, calling again returns already verified without accepting OTP
        var response = authService.verifyEmailOtp(req);
        assertEquals("Account is already verified.", response.getMessage());
    }

    @Test
    @DisplayName("confirmSetPassword enforces 5-attempt limit and invalidates OTP")
    void testConfirmSetPassword_Exceeding5Attempts_InvalidatesCode() {
        EmailVerificationOtp setPasswordOtp = EmailVerificationOtp.builder()
                .id(UUID.randomUUID())
                .user(oauthCandidateWithoutPassword)
                .otpCode("555123")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        com.vionsys.hireai.security.CustomUserDetails userDetails = com.vionsys.hireai.security.CustomUserDetails.fromUser(oauthCandidateWithoutPassword);
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext secCtx = mock(org.springframework.security.core.context.SecurityContext.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(secCtx.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(secCtx);

        try {
            when(userRepository.findByEmail("oauth.candidate@example.com"))
                    .thenReturn(Optional.of(oauthCandidateWithoutPassword));
            when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthCandidateWithoutPassword))
                    .thenReturn(Optional.of(setPasswordOtp));

            SetPasswordRequest req = SetPasswordRequest.builder()
                    .otp("999999") // wrong code
                    .newPassword("NewSecretPass1")
                    .confirmPassword("NewSecretPass1")
                    .build();

            // Fail 4 times
            for (int i = 1; i <= 4; i++) {
                InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> userService.confirmSetPassword(req));
                assertTrue(ex.getMessage().contains("attempt(s) remaining"));
            }

            // 5th failure invalidates code
            InvalidOtpException ex5 = assertThrows(InvalidOtpException.class, () -> userService.confirmSetPassword(req));
            assertTrue(ex5.getMessage().contains("Too many incorrect attempts"));
            assertTrue(setPasswordOtp.isUsed(), "Set-password OTP must be invalidated after 5 wrong attempts");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("deleteAccount enforces 5-attempt limit and invalidates OTP")
    void testDeleteAccount_Exceeding5Attempts_InvalidatesCode() {
        EmailVerificationOtp deleteOtp = EmailVerificationOtp.builder()
                .id(UUID.randomUUID())
                .user(oauthCandidateWithoutPassword)
                .otpCode("777888")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();

        com.vionsys.hireai.security.CustomUserDetails userDetails = com.vionsys.hireai.security.CustomUserDetails.fromUser(oauthCandidateWithoutPassword);
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext secCtx = mock(org.springframework.security.core.context.SecurityContext.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(secCtx.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(secCtx);

        try {
            when(userRepository.findByEmail("oauth.candidate@example.com"))
                    .thenReturn(Optional.of(oauthCandidateWithoutPassword));
            when(emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(oauthCandidateWithoutPassword))
                    .thenReturn(Optional.of(deleteOtp));

            DeleteAccountRequest req = DeleteAccountRequest.builder()
                    .confirmationText("DELETE")
                    .otp("111111") // wrong code
                    .build();

            // Fail 4 times
            for (int i = 1; i <= 4; i++) {
                InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> userService.deleteAccount(req));
                assertTrue(ex.getMessage().contains("attempt(s) remaining"));
            }

            // 5th failure invalidates code
            InvalidOtpException ex5 = assertThrows(InvalidOtpException.class, () -> userService.deleteAccount(req));
            assertTrue(ex5.getMessage().contains("Too many incorrect attempts"));
            assertTrue(deleteOtp.isUsed(), "Delete-account OTP must be invalidated after 5 wrong attempts");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
