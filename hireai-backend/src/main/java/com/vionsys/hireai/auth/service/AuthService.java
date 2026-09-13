package com.vionsys.hireai.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.ForgotPasswordRequest;
import com.vionsys.hireai.auth.dto.ForgotPasswordResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.LogoutResponse;
import com.vionsys.hireai.auth.dto.RecruiterRegisterRequest;
import com.vionsys.hireai.auth.dto.RefreshTokenRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.auth.dto.ResetPasswordRequest;
import com.vionsys.hireai.auth.dto.VerifyTokenResponse;
import com.vionsys.hireai.auth.entity.PasswordResetToken;
import com.vionsys.hireai.auth.entity.RefreshToken;
import com.vionsys.hireai.auth.repository.PasswordResetTokenRepository;
import com.vionsys.hireai.auth.repository.RefreshTokenRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.AccountLockedException;
import com.vionsys.hireai.exception.InvalidTokenException;
import com.vionsys.hireai.exception.RoleNotFoundException;
import com.vionsys.hireai.exception.TokenReuseDetectedException;
import com.vionsys.hireai.exception.UserAlreadyExistsException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtProperties;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final com.vionsys.hireai.email.EmailService emailService;

    @Transactional
    public AuthResponse registerCandidate(RegisterRequest request) {
        return register(request, RoleType.ROLE_CANDIDATE);
    }

    @Transactional
    public AuthResponse registerRecruiter(RecruiterRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Role role = roleRepository.findByName(RoleType.ROLE_RECRUITER)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-create RecruiterProfile with company name
        RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                .user(savedUser)
                .companyName(request.getCompanyName())
                .verified(false)
                .build();
        recruiterProfileRepository.save(recruiterProfile);

        // Send automated welcome email asynchronously
        try {
            emailService.sendWelcomeRecruiterEmail(savedUser);
        } catch (Exception ex) {
            log.warn("Failed to send welcome recruiter email: {}", ex.getMessage());
        }

        CustomUserDetails userDetails = CustomUserDetails.fromUser(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createAndSaveRefreshToken(savedUser);

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getName().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private AuthResponse register(RegisterRequest request, RoleType roleType) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Send automated welcome email asynchronously
        try {
            if (roleType == RoleType.ROLE_CANDIDATE) {
                emailService.sendWelcomeCandidateEmail(savedUser);
            }
        } catch (Exception ex) {
            log.warn("Failed to send welcome candidate email: {}", ex.getMessage());
        }

        CustomUserDetails userDetails = CustomUserDetails.fromUser(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createAndSaveRefreshToken(savedUser);

        return AuthResponse.builder()
                .userId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().getName().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // 1. Check account lockout
        if (user.getLockoutUntil() != null) {
            if (LocalDateTime.now().isBefore(user.getLockoutUntil())) {
                throw new AccountLockedException(
                        "Account is temporarily locked due to consecutive failed login attempts. Please try again after " 
                        + user.getLockoutUntil() + " or reset your password."
                );
            } else {
                // Lockout period has elapsed, reset lockout state
                user.setLockoutUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }

        // 2. Authenticate credentials
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(15));
                log.warn("Account {} locked until {} due to 5 failed login attempts", user.getEmail(), user.getLockoutUntil());
            }
            userRepository.save(user);
            throw ex;
        }

        // 3. Reset failed attempts counter on successful login
        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getLockoutUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        // 4. Send security login notification email asynchronously
        try {
            emailService.sendLoginAlertEmail(user);
        } catch (Exception ex) {
            log.warn("Failed to send login alert email: {}", ex.getMessage());
        }

        CustomUserDetails userDetails = CustomUserDetails.fromUser(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createAndSaveRefreshToken(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Refresh Token Rotation (RTR) with token theft / reuse detection.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String incomingToken = request.getRefreshToken();
        if (incomingToken == null || incomingToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // 1. Check for token reuse / compromised session
        if (tokenEntity.isRevoked() || tokenEntity.getReplacedByToken() != null) {
            // Revoke all tokens for this user immediately
            refreshTokenRepository.revokeAllUserTokens(tokenEntity.getUser());
            log.error("Token reuse detected for user {}. Revoking all active sessions.", tokenEntity.getUser().getEmail());
            throw new TokenReuseDetectedException(
                    "Compromised refresh token reuse detected. All sessions for this account have been invalidated."
            );
        }

        // 2. Check expiration
        if (tokenEntity.isExpired()) {
            tokenEntity.setRevoked(true);
            refreshTokenRepository.save(tokenEntity);
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        // 3. Check user status
        User user = tokenEntity.getUser();
        if (!Boolean.TRUE.equals(user.getEnabled()) || !Boolean.TRUE.equals(user.getAccountNonLocked())) {
            throw new InvalidTokenException("User account is disabled or locked");
        }

        // 4. Generate new tokens (RTR)
        CustomUserDetails userDetails = CustomUserDetails.fromUser(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        LocalDateTime newExpiryDate = LocalDateTime.now().plus(
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
        );

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .expiryDate(newExpiryDate)
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        // 5. Invalidate old token and link to new replacement
        tokenEntity.setRevoked(true);
        tokenEntity.setReplacedByToken(newRefreshToken);
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * User logout - Revokes the active refresh token.
     */
    @Transactional
    public LogoutResponse logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenRepository.findByToken(request.getRefreshToken())
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }

        return LogoutResponse.builder()
                .success(true)
                .message("User logged out successfully. Session has been terminated.")
                .build();
    }

    private String createAndSaveRefreshToken(User user) {
        CustomUserDetails userDetails = CustomUserDetails.fromUser(user);
        String rawToken = jwtService.generateRefreshToken(userDetails);

        LocalDateTime expiryDate = LocalDateTime.now().plus(
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration())
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawToken)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiresAt)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email asynchronously
        try {
            emailService.sendPasswordResetEmail(user, token, expiresAt);
        } catch (Exception ex) {
            log.warn("Failed to send password reset email: {}", ex.getMessage());
        }

        return ForgotPasswordResponse.builder()
                .success(true)
                .message("Password reset token generated successfully. Valid for 15 minutes.")
                .resetToken(token)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional(readOnly = true)
    public VerifyTokenResponse verifyResetToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Reset token is required");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        return VerifyTokenResponse.builder()
                .valid(true)
                .email(resetToken.getUser().getEmail())
                .message("Password reset token is valid")
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent password reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        // Invalidate all active refresh tokens on password reset for security
        refreshTokenRepository.revokeAllUserTokens(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

}