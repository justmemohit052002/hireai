package com.vionsys.hireai.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
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
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtProperties;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;
import com.vionsys.hireai.security.ratelimit.AccountBackoffManager;

import java.security.SecureRandom;
import com.vionsys.hireai.auth.dto.OAuthRequest;
import com.vionsys.hireai.auth.dto.OAuthUserInfo;
import com.vionsys.hireai.auth.dto.ResendOtpRequest;
import com.vionsys.hireai.auth.dto.VerifyEmailOtpRequest;
import com.vionsys.hireai.auth.entity.EmailVerificationOtp;
import com.vionsys.hireai.auth.repository.EmailVerificationOtpRepository;
import com.vionsys.hireai.exception.AccountNotVerifiedException;
import com.vionsys.hireai.exception.InvalidOtpException;
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
    private final CandidateRepository candidateRepository;
    private final CandidateIdGenerator candidateIdGenerator;
    private final com.vionsys.hireai.email.EmailService emailService;
    private final com.vionsys.hireai.security.ratelimit.AccountBackoffManager accountBackoffManager;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final OAuthService oauthService;

    private String generate6DigitOtp() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public AuthResponse registerCandidate(RegisterRequest request) {
        return register(request, RoleType.ROLE_CANDIDATE);
    }

    @Transactional
    public AuthResponse registerRecruiter(RecruiterRegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        Role recruiterRole = roleRepository.findByName(RoleType.ROLE_RECRUITER)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (Boolean.TRUE.equals(existing.getEnabled())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            // Update unverified user's credentials and ensure role is ROLE_RECRUITER
            existing.setFirstName(request.getFirstName());
            existing.setLastName(request.getLastName());
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            existing.setPhoneNumber(request.getPhoneNumber());
            existing.setRole(recruiterRole);
            user = userRepository.save(existing);
        } else {
            user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phoneNumber(request.getPhoneNumber())
                    .enabled(false)
                    .accountNonLocked(true)
                    .failedLoginAttempts(0)
                    .role(recruiterRole)
                    .build();
            user = userRepository.save(user);
        }

        // Auto-create RecruiterProfile with company name
        ensureRecruiterProfile(user, request.getCompanyName());

        // Invalidate any older unused OTPs for this user
        emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(old -> {
                    old.setUsed(true);
                    emailVerificationOtpRepository.save(old);
                });

        // Issue and send 6-digit OTP
        String otpCode = generate6DigitOtp();
        EmailVerificationOtp otpEntity = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        emailVerificationOtpRepository.save(otpEntity);

        try {
            emailService.sendEmailVerificationOtp(user, otpCode, 10);
            log.info("Sent 6-digit verification OTP to recruiter: {}", user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send verification OTP to recruiter email: {}", ex.getMessage());
        }

        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .requiresVerification(true)
                .emailVerified(false)
                .message("Registration successful! A 6-digit verification code has been sent to your email.")
                .build();
    }

    private AuthResponse register(RegisterRequest request, RoleType roleType) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (Boolean.TRUE.equals(existing.getEnabled())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            // Update unverified user's credentials and role
            existing.setFirstName(request.getFirstName());
            existing.setLastName(request.getLastName());
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            existing.setPhoneNumber(request.getPhoneNumber());
            existing.setRole(role);
            user = userRepository.save(existing);
        } else {
            user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phoneNumber(request.getPhoneNumber())
                    .enabled(false)
                    .accountNonLocked(true)
                    .failedLoginAttempts(0)
                    .role(role)
                    .build();
            user = userRepository.save(user);
        }

        if (roleType == RoleType.ROLE_CANDIDATE) {
            ensureCandidateProfile(user);
        } else if (roleType == RoleType.ROLE_RECRUITER) {
            ensureRecruiterProfile(user, null);
        }

        // Invalidate any older unused OTPs for this user
        emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(old -> {
                    old.setUsed(true);
                    emailVerificationOtpRepository.save(old);
                });

        // Issue and send 6-digit OTP
        String otpCode = generate6DigitOtp();
        EmailVerificationOtp otpEntity = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        emailVerificationOtpRepository.save(otpEntity);

        try {
            emailService.sendEmailVerificationOtp(user, otpCode, 10);
            log.info("Sent 6-digit verification OTP to candidate: {}", user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to send verification OTP to candidate email: {}", ex.getMessage());
        }

        return AuthResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .requiresVerification(true)
                .emailVerified(false)
                .message("Registration successful! A 6-digit verification code has been sent to your email.")
                .build();
    }

    @Transactional
    public AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with email: " + email));

        if (Boolean.TRUE.equals(user.getEnabled())) {
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
                    .requiresVerification(false)
                    .emailVerified(true)
                    .message("Account is already verified.")
                    .build();
        }

        EmailVerificationOtp otpEntity = emailVerificationOtpRepository
                .findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No pending verification code found. Please request a new code."));

        if (otpEntity.isExpired()) {
            throw new InvalidOtpException("Verification code has expired. Please request a new code.");
        }

        if (otpEntity.getAttempts() >= 5) {
            throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
        }

        if (!otpEntity.getOtpCode().equals(request.getOtp().trim())) {
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            emailVerificationOtpRepository.save(otpEntity);
            int remaining = 5 - otpEntity.getAttempts();
            if (remaining <= 0) {
                throw new InvalidOtpException("Too many incorrect attempts. Please request a new verification code.");
            }
            throw new InvalidOtpException("Invalid verification code. " + remaining + " attempt(s) remaining.");
        }

        // OTP is correct!
        otpEntity.setUsed(true);
        emailVerificationOtpRepository.save(otpEntity);

        user.setEnabled(true);
        userRepository.save(user);

        // Ensure profile exists for the verified user
        if (user.getRole().getName() == RoleType.ROLE_RECRUITER) {
            ensureRecruiterProfile(user, null);
        } else {
            ensureCandidateProfile(user);
        }

        // Send welcome email asynchronously
        try {
            if (user.getRole().getName() == RoleType.ROLE_RECRUITER) {
                emailService.sendWelcomeRecruiterEmail(user);
            } else {
                emailService.sendWelcomeCandidateEmail(user);
            }
        } catch (Exception ex) {
            log.warn("Failed to send welcome email after OTP verification: {}", ex.getMessage());
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
                .requiresVerification(false)
                .emailVerified(true)
                .message("Email verified successfully! Welcome to HireAI.")
                .build();
    }

    @Transactional
    public void resendEmailOtp(ResendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("No account found with email: " + email));

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalStateException("Your account is already verified. Please log in.");
        }

        // Check 60-second cooldown
        emailVerificationOtpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .ifPresent(latestOtp -> {
                    if (latestOtp.getCreatedAt() != null) {
                        long secondsSinceLast = Duration.between(latestOtp.getCreatedAt(), LocalDateTime.now()).getSeconds();
                        if (secondsSinceLast < 60) {
                            long wait = 60 - secondsSinceLast;
                            throw new InvalidOtpException("Please wait " + wait + " second(s) before requesting a new code.");
                        }
                    }
                    latestOtp.setUsed(true);
                    emailVerificationOtpRepository.save(latestOtp);
                });

        String newOtpCode = generate6DigitOtp();
        EmailVerificationOtp newOtp = EmailVerificationOtp.builder()
                .user(user)
                .otpCode(newOtpCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .attempts(0)
                .build();
        emailVerificationOtpRepository.save(newOtp);

        try {
            emailService.sendEmailVerificationOtp(user, newOtpCode, 10);
            log.info("Resent 6-digit verification OTP to: {}", user.getEmail());
        } catch (Exception ex) {
            log.warn("Failed to resend verification OTP to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check email verification status before authenticating
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccountNotVerifiedException(
                    "Your email address is not verified. Please verify your account using the 6-digit OTP sent to your email.",
                    user.getEmail()
            );
        }

        // 1. Check account exponential backoff and lockout state
        AccountBackoffManager.BackoffResult inMemoryBackoff = accountBackoffManager.checkBackoff(request.getEmail());
        if (inMemoryBackoff.isBlocked()) {
            throw new AccountLockedException(
                    "Too many failed login attempts for this account. Exponential backoff active. Please wait "
                    + inMemoryBackoff.getRemainingCooldownSeconds() + " seconds before retrying.",
                    inMemoryBackoff.getRemainingCooldownSeconds()
            );
        }

        if (user.getLockoutUntil() != null) {
            if (LocalDateTime.now().isBefore(user.getLockoutUntil())) {
                long remainingSeconds = Math.max(1L, java.time.Duration.between(LocalDateTime.now(), user.getLockoutUntil()).toSeconds());
                throw new AccountLockedException(
                        "Account is temporarily locked due to failed login attempts. Please try again after " 
                        + remainingSeconds + " seconds or reset your password.",
                        remainingSeconds
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
            long backoffSeconds = accountBackoffManager.recordFailure(request.getEmail());
            int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            user.setFailedLoginAttempts(attempts);
            if (backoffSeconds > 0) {
                user.setLockoutUntil(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.warn("Account {} placed on exponential backoff for {}s due to {} failed login attempts",
                        user.getEmail(), backoffSeconds, attempts);
            }
            userRepository.save(user);
            throw ex;
        }

        // 3. Reset failed attempts counter and backoff on successful login
        accountBackoffManager.recordSuccess(request.getEmail());
        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getLockoutUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        // 4. If caller explicitly requested a specific role (e.g. user toggled RECRUITER tab on login screen)
        // Note: Never downgrade or alter ROLE_ADMIN
        if (user.getRole().getName() != RoleType.ROLE_ADMIN && request.getRole() != null && !request.getRole().isBlank()) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if (requestedRole.contains("RECRUITER")) {
                if (user.getRole().getName() != RoleType.ROLE_RECRUITER) {
                    Role recruiterRole = roleRepository.findByName(RoleType.ROLE_RECRUITER)
                            .orElseThrow(() -> new RoleNotFoundException("Role not found"));
                    user.setRole(recruiterRole);
                    user = userRepository.save(user);
                    log.info("Updated user {} active role to ROLE_RECRUITER", user.getEmail());
                }
                ensureRecruiterProfile(user, null);
            } else if (requestedRole.contains("CANDIDATE")) {
                if (user.getRole().getName() != RoleType.ROLE_CANDIDATE) {
                    Role candidateRole = roleRepository.findByName(RoleType.ROLE_CANDIDATE)
                            .orElseThrow(() -> new RoleNotFoundException("Role not found"));
                    user.setRole(candidateRole);
                    user = userRepository.save(user);
                    log.info("Updated user {} active role to ROLE_CANDIDATE", user.getEmail());
                }
                ensureCandidateProfile(user);
            }
        }

        // 5. Send security login notification email asynchronously
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
     * Authenticates or registers a user via Google or LinkedIn OAuth.
     * OAuth emails are pre-verified, so users are enabled immediately without requiring an OTP.
     */
    @Transactional
    public AuthResponse processOAuthLogin(OAuthRequest request, String provider) {
        OAuthUserInfo userInfo = oauthService.extractUserInfo(provider, request);
        String email = userInfo.getEmail().toLowerCase().trim();

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        RoleType desiredRoleType = RoleType.ROLE_CANDIDATE;
        if (request.getRole() != null && request.getRole().trim().toUpperCase().contains("RECRUITER")) {
            desiredRoleType = RoleType.ROLE_RECRUITER;
        }

        Role desiredRole = roleRepository.findByName(desiredRoleType)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Provider verified the email, so ensure enabled = true
            user.setEnabled(true);
            user.setAuthProvider(provider);
            if (userInfo.getProviderId() != null) {
                user.setProviderId(userInfo.getProviderId());
            }
            if (userInfo.getAvatarUrl() != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
                user.setAvatarUrl(userInfo.getAvatarUrl());
            }

            // Sync user role with requested login/signup role context (never alter ROLE_ADMIN)
            if (user.getRole().getName() != RoleType.ROLE_ADMIN && request.getRole() != null && !request.getRole().isBlank()) {
                user.setRole(desiredRole);
            }
            user = userRepository.save(user);

            // Ensure profile exists for the active role
            if (user.getRole().getName() == RoleType.ROLE_RECRUITER) {
                ensureRecruiterProfile(user, request.getCompanyName());
            } else {
                ensureCandidateProfile(user);
            }
        } else {
            user = User.builder()
                    .firstName(userInfo.getFirstName())
                    .lastName(userInfo.getLastName())
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .enabled(true) // Pre-verified via OAuth!
                    .accountNonLocked(true)
                    .failedLoginAttempts(0)
                    .authProvider(provider)
                    .providerId(userInfo.getProviderId())
                    .avatarUrl(userInfo.getAvatarUrl())
                    .role(desiredRole)
                    .build();
            user = userRepository.save(user);

            if (desiredRoleType == RoleType.ROLE_RECRUITER) {
                ensureRecruiterProfile(user, request.getCompanyName());
            } else {
                ensureCandidateProfile(user);
            }

            // Send welcome email asynchronously
            try {
                if (desiredRoleType == RoleType.ROLE_RECRUITER) {
                    emailService.sendWelcomeRecruiterEmail(user);
                } else {
                    emailService.sendWelcomeCandidateEmail(user);
                }
            } catch (Exception ex) {
                log.warn("Failed to send welcome email for OAuth user: {}", ex.getMessage());
            }
        }

        // Generate JWT session tokens
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
                .requiresVerification(false)
                .emailVerified(true)
                .message("Successfully authenticated via " + provider)
                .build();
    }

    private void ensureCandidateProfile(User user) {
        try {
            Optional<Candidate> existingCandidateOpt = candidateRepository.findByEmailNative(user.getEmail());
            if (existingCandidateOpt.isPresent()) {
                Candidate existingCandidate = existingCandidateOpt.get();
                if (existingCandidate.getUser() == null) {
                    existingCandidate.setUser(user);
                    existingCandidate.setDeleted(false);
                    candidateRepository.save(existingCandidate);
                    log.info("Linked existing Candidate profile ({}) to user: {}", existingCandidate.getCandidateId(), user.getEmail());
                }
            } else if (!candidateRepository.existsByUserIdNative(user.getId())) {
                Candidate candidate = Candidate.builder()
                        .user(user)
                        .candidateId(candidateIdGenerator.generateCandidateId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhoneNumber())
                        .candidateStatus(CandidateStatus.ACTIVE)
                        .deleted(false)
                        .build();
                candidateRepository.save(candidate);
                log.info("Auto-created Candidate profile for user: {}", user.getEmail());
            }
        } catch (Exception ex) {
            log.warn("Failed to ensure Candidate profile: {}", ex.getMessage());
        }
    }

    private void ensureRecruiterProfile(User user, String companyName) {
        try {
            if (!recruiterProfileRepository.existsByUserId(user.getId())) {
                String comp = companyName != null && !companyName.isBlank()
                        ? companyName
                        : (user.getLastName() + " Organization");
                RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                        .user(user)
                        .companyName(comp)
                        .verified(false)
                        .build();
                recruiterProfileRepository.save(recruiterProfile);
                log.info("Auto-created Recruiter profile for user: {}", user.getEmail());
            }
        } catch (Exception ex) {
            log.warn("Failed to ensure Recruiter profile: {}", ex.getMessage());
        }
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
        accountBackoffManager.recordSuccess(user.getEmail());

        // Invalidate all active refresh tokens on password reset for security
        refreshTokenRepository.revokeAllUserTokens(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

}