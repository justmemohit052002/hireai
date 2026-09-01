package com.vionsys.hireai.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.ForgotPasswordRequest;
import com.vionsys.hireai.auth.dto.ForgotPasswordResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.RecruiterRegisterRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.auth.dto.ResetPasswordRequest;
import com.vionsys.hireai.auth.dto.VerifyTokenResponse;
import com.vionsys.hireai.auth.entity.PasswordResetToken;
import com.vionsys.hireai.auth.repository.PasswordResetTokenRepository;
import com.vionsys.hireai.common.enums.RoleType;
import com.vionsys.hireai.exception.InvalidTokenException;
import com.vionsys.hireai.exception.RoleNotFoundException;
import com.vionsys.hireai.exception.UserAlreadyExistsException;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.role.entity.Role;
import com.vionsys.hireai.role.repository.RoleRepository;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
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
                .orElseThrow(() ->
                        new RoleNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountNonLocked(true)
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
            // log silently
        }

        CustomUserDetails userDetails =
                CustomUserDetails.fromUser(savedUser);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

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

    private AuthResponse register(
            RegisterRequest request,
            RoleType roleType) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() ->
                        new RoleNotFoundException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .enabled(true)
                .accountNonLocked(true)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        // Send automated welcome email asynchronously
        try {
            if (roleType == RoleType.ROLE_CANDIDATE) {
                emailService.sendWelcomeCandidateEmail(savedUser);
            }
        } catch (Exception ex) {
            // log silently
        }

        CustomUserDetails userDetails =
                CustomUserDetails.fromUser(savedUser);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

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

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        // Send security login notification email asynchronously
        try {
            emailService.sendLoginAlertEmail(user);
        } catch (Exception ex) {
            // log silently
        }

        CustomUserDetails userDetails =
                CustomUserDetails.fromUser(user);

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

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
            // log silently
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
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

}