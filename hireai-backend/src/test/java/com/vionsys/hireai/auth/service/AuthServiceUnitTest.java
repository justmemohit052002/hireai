package com.vionsys.hireai.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vionsys.hireai.auth.dto.AddRoleRequest;
import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.SwitchRoleRequest;
import com.vionsys.hireai.auth.entity.RefreshToken;
import com.vionsys.hireai.auth.repository.EmailVerificationOtpRepository;
import com.vionsys.hireai.auth.repository.PasswordResetTokenRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.email.EmailService;
import com.vionsys.hireai.exception.AccountLockedException;
import com.vionsys.hireai.exception.AccountNotVerifiedException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtProperties;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.security.ratelimit.AccountBackoffManager;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

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
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateIdGenerator candidateIdGenerator;

    @Mock
    private EmailService emailService;

    @Mock
    private AccountBackoffManager accountBackoffManager;

    @Mock
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @Mock
    private OAuthService oauthService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private AuthService authService;

    private Role candidateRole;
    private Role recruiterRole;
    private Role adminRole;
    private User candidateUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        candidateRole = Role.builder()
                .id(1L)
                .name(RoleType.ROLE_CANDIDATE)
                .build();

        recruiterRole = Role.builder()
                .id(2L)
                .name(RoleType.ROLE_RECRUITER)
                .build();

        adminRole = Role.builder()
                .id(3L)
                .name(RoleType.ROLE_ADMIN)
                .build();

        candidateUser = User.builder()
                .id(userId)
                .email("candidate@example.com")
                .password("$2a$10$encodedPasswordHash")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .role(candidateRole)
                .roles(new HashSet<>(Set.of(candidateRole)))
                .build();
    }

    // =========================================================================
    // 1. LOGIN TESTS
    // =========================================================================

    @Test
    @DisplayName("shouldLoginSuccessfully_whenCredentialsAreValid")
    void shouldLoginSuccessfully_whenCredentialsAreValid() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidateUser));
        when(accountBackoffManager.checkBackoff("candidate@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("candidate@example.com");
        assertThat(response.getActiveRole()).isEqualTo("ROLE_CANDIDATE");
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(accountBackoffManager).recordSuccess("candidate@example.com");
    }

    @Test
    @DisplayName("shouldIgnoreForgedRole_whenLoginRequestContainsRoleField")
    void shouldIgnoreForgedRole_whenLoginRequestContainsRoleField() {
        // Arrange: client submits request attempting to escalate to ROLE_ADMIN
        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidateUser));
        when(accountBackoffManager.checkBackoff("candidate@example.com")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("valid-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.login(request);

        // Assert: activeRole must strictly remain ROLE_CANDIDATE from database, NOT forged ROLE_ADMIN
        assertThat(response.getActiveRole()).isEqualTo("ROLE_CANDIDATE");
        assertThat(response.getRole()).isEqualTo("ROLE_CANDIDATE");
        verify(userRepository, never()).findByEmail("admin@example.com");
    }

    @Test
    @DisplayName("shouldThrowBadCredentialsException_whenPasswordIsIncorrect")
    void shouldThrowBadCredentialsException_whenPasswordIsIncorrect() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("WrongPassword!")
                .build();

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidateUser));
        when(accountBackoffManager.checkBackoff("candidate@example.com")).thenReturn(null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(accountBackoffManager.recordFailure("candidate@example.com")).thenReturn(15L);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldThrowUserNotFoundException_whenUserDoesNotExist")
    void shouldThrowUserNotFoundException_whenUserDoesNotExist() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authService.login(request));
        // Verify constant-time BCrypt call was executed to prevent timing attacks
        verify(passwordEncoder).matches(eq("Password123!"), anyString());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("shouldThrowAccountNotVerifiedException_whenEmailNotVerified")
    void shouldThrowAccountNotVerifiedException_whenEmailNotVerified() {
        // Arrange
        candidateUser.setEnabled(false);
        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidateUser));

        // Act & Assert
        assertThrows(AccountNotVerifiedException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("shouldThrowAccountLockedException_whenAccountIsTemporarilyLocked")
    void shouldThrowAccountLockedException_whenAccountIsTemporarilyLocked() {
        // Arrange
        candidateUser.setLockoutUntil(LocalDateTime.now().plusMinutes(10));
        LoginRequest request = LoginRequest.builder()
                .email("candidate@example.com")
                .password("Password123!")
                .build();

        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidateUser));
        when(accountBackoffManager.checkBackoff("candidate@example.com")).thenReturn(null);

        // Act & Assert
        assertThrows(AccountLockedException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    // =========================================================================
    // 2. SWITCH-ROLE TESTS
    // =========================================================================

    @Test
    @DisplayName("shouldSwitchRoleSuccessfully_whenUserPossessesTargetRole")
    void shouldSwitchRoleSuccessfully_whenUserPossessesTargetRole() {
        // Arrange: User has both ROLE_CANDIDATE and ROLE_RECRUITER
        candidateUser.getRoles().add(recruiterRole);
        SwitchRoleRequest request = new SwitchRoleRequest();
        request.setRole("ROLE_RECRUITER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
        when(roleRepository.findByName(RoleType.ROLE_RECRUITER)).thenReturn(Optional.of(recruiterRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("switched-role-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.switchRole(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getActiveRole()).isEqualTo("ROLE_RECRUITER");
        assertThat(response.getAccessToken()).isEqualTo("switched-role-jwt");
    }

    @Test
    @DisplayName("shouldThrowAccessDeniedException_whenSwitchingToRoleNotOwned")
    void shouldThrowAccessDeniedException_whenSwitchingToRoleNotOwned() {
        // Arrange: User only has ROLE_CANDIDATE, tries to switch to ROLE_RECRUITER
        SwitchRoleRequest request = new SwitchRoleRequest();
        request.setRole("ROLE_RECRUITER");
        when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authService.switchRole(userId, request));
        assertThat(ex.getMessage()).contains("You do not possess the requested role");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenSwitchRoleWithInvalidRoleName")
    void shouldThrowIllegalArgumentException_whenSwitchRoleWithInvalidRoleName() {
        // Arrange
        SwitchRoleRequest request = new SwitchRoleRequest();
        request.setRole("NON_EXISTENT_ROLE_NAME");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.switchRole(userId, request));
        verify(userRepository, never()).findById(any());
    }

    // =========================================================================
    // 3. ADD-ROLE TESTS
    // =========================================================================

    @Test
    @DisplayName("shouldAddRoleSuccessfully_whenRoleNotYetOwned")
    void shouldAddRoleSuccessfully_whenRoleNotYetOwned() {
        // Arrange: User currently only has CANDIDATE; adds RECRUITER
        AddRoleRequest request = new AddRoleRequest();
        request.setRole("RECRUITER");
        request.setCompanyName("Acme Corp");

        when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
        when(roleRepository.findByName(RoleType.ROLE_RECRUITER)).thenReturn(Optional.of(recruiterRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("add-role-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthResponse response = authService.addRole(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(candidateUser.getRoles()).contains(recruiterRole);
        assertThat(response.getAccessToken()).isEqualTo("add-role-jwt");
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenAddingDuplicateRole")
    void shouldThrowIllegalArgumentException_whenAddingDuplicateRole() {
        // Arrange: User already has CANDIDATE
        AddRoleRequest request = new AddRoleRequest();
        request.setRole("CANDIDATE");

        when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.addRole(userId, request));
        assertThat(ex.getMessage()).contains("User already has role: ROLE_CANDIDATE");
    }

    @Test
    @DisplayName("shouldThrowAccessDeniedException_whenSelfAssigningAdminRole")
    void shouldThrowAccessDeniedException_whenSelfAssigningAdminRole() {
        // Arrange: User attempts to self-assign ROLE_ADMIN
        AddRoleRequest request = new AddRoleRequest();
        request.setRole("ADMIN");

        // Act & Assert
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> authService.addRole(userId, request));
        assertThat(ex.getMessage()).contains("Cannot self-assign administrative role");
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("shouldThrowIllegalArgumentException_whenAddRoleHasBlankRole")
    void shouldThrowIllegalArgumentException_whenAddRoleHasBlankRole() {
        // Arrange
        AddRoleRequest request = new AddRoleRequest();
        request.setRole("   ");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.addRole(userId, request));
    }
}
