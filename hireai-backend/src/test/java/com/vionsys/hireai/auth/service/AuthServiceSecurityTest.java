package com.vionsys.hireai.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.LogoutResponse;
import com.vionsys.hireai.auth.dto.RefreshTokenRequest;
import com.vionsys.hireai.auth.entity.RefreshToken;
import com.vionsys.hireai.auth.repository.PasswordResetTokenRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.AccountLockedException;
import com.vionsys.hireai.exception.InvalidTokenException;
import com.vionsys.hireai.exception.TokenReuseDetectedException;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.jwt.JwtProperties;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role candidateRole;

    @BeforeEach
    void setUp() {
        candidateRole = Role.builder()
                .id(1L)
                .name(RoleType.ROLE_CANDIDATE)
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("candidate@example.com")
                .firstName("Test")
                .lastName("User")
                .password("encodedPassword")
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .role(candidateRole)
                .build();
    }

    @Test
    void testRefreshToken_Success_RotatesTokens() {
        String oldTokenString = "old-refresh-token";
        RefreshToken tokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(oldTokenString)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(oldTokenString)).thenReturn(Optional.of(tokenEntity));
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");

        RefreshTokenRequest request = new RefreshTokenRequest(oldTokenString);
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertTrue(tokenEntity.isRevoked());
        assertEquals("new-refresh-token", tokenEntity.getReplacedByToken());
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void testRefreshToken_ReuseDetected_RevokesAllSessions() {
        String reusedToken = "already-used-refresh-token";
        RefreshToken compromisedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(reusedToken)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .replacedByToken("some-replacement-token")
                .build();

        when(refreshTokenRepository.findByToken(reusedToken)).thenReturn(Optional.of(compromisedToken));

        RefreshTokenRequest request = new RefreshTokenRequest(reusedToken);

        assertThrows(TokenReuseDetectedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
    }

    @Test
    void testRefreshToken_Expired_ThrowsInvalidTokenException() {
        String expiredToken = "expired-refresh-token";
        RefreshToken tokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(expiredToken)
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(expiredToken)).thenReturn(Optional.of(tokenEntity));

        RefreshTokenRequest request = new RefreshTokenRequest(expiredToken);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(request));
        assertTrue(tokenEntity.isRevoked());
    }

    @Test
    void testLogout_RevokesToken() {
        String activeToken = "active-token";
        RefreshToken tokenEntity = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(activeToken)
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(activeToken)).thenReturn(Optional.of(tokenEntity));

        LogoutResponse response = authService.logout(new RefreshTokenRequest(activeToken));

        assertTrue(response.isSuccess());
        assertTrue(tokenEntity.isRevoked());
        verify(refreshTokenRepository).save(tokenEntity);
    }

    @Test
    void testLogin_FailedAttempts_LocksAccountAfter5Failures() {
        testUser.setFailedLoginAttempts(4);
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("wrong-password")
                .build();

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals(5, testUser.getFailedLoginAttempts());
        assertNotNull(testUser.getLockoutUntil());
        verify(userRepository).save(testUser);
    }

    @Test
    void testLogin_WhenAccountLocked_ThrowsAccountLockedException() {
        testUser.setFailedLoginAttempts(5);
        testUser.setLockoutUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(testUser));

        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("any-password")
                .build();

        assertThrows(AccountLockedException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }
}
