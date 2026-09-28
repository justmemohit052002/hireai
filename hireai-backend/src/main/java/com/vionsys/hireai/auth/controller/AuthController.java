package com.vionsys.hireai.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.ForgotPasswordRequest;
import com.vionsys.hireai.auth.dto.ForgotPasswordResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.OAuthRequest;
import com.vionsys.hireai.auth.dto.RecruiterRegisterRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.auth.dto.ResendOtpRequest;
import com.vionsys.hireai.auth.dto.ResetPasswordRequest;
import com.vionsys.hireai.auth.dto.VerifyEmailOtpRequest;
import com.vionsys.hireai.auth.dto.VerifyTokenResponse;
import com.vionsys.hireai.auth.service.AuthService;
import com.vionsys.hireai.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication & Password Management", description = "Endpoints for registration, login, JWT tokens, and password reset workflows")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register Candidate", description = "Creates a new candidate account and sends 6-digit email OTP verification code")
    @PostMapping("/register/candidate")
    public ResponseEntity<AuthResponse> registerCandidate(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.registerCandidate(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Register Recruiter", description = "Creates a new recruiter account with company name and sends 6-digit email OTP verification code")
    @PostMapping("/register/recruiter")
    public ResponseEntity<AuthResponse> registerRecruiter(
            @Valid @RequestBody RecruiterRegisterRequest request) {

        AuthResponse response = authService.registerRecruiter(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Verify Email OTP", description = "Verifies 6-digit OTP code sent to user email and activates the account, returning JWT tokens")
    @PostMapping("/verify-email-otp")
    public ResponseEntity<AuthResponse> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {

        AuthResponse response = authService.verifyEmailOtp(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Resend Email OTP", description = "Resends a new 6-digit OTP code with rate limit cooldown of 60 seconds")
    @PostMapping("/resend-email-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        authService.resendEmailOtp(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("A new 6-digit verification code has been sent to your email.")
                        .build()
        );
    }

    @Operation(summary = "Google OAuth SSO", description = "Authenticates or registers user via Google ID Token, returning active JWT session tokens")
    @PostMapping("/oauth/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody OAuthRequest request) {

        AuthResponse response = authService.processOAuthLogin(request, "GOOGLE");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "LinkedIn OAuth SSO", description = "Authenticates or registers user via LinkedIn access token, returning active JWT session tokens")
    @PostMapping("/oauth/linkedin")
    public ResponseEntity<AuthResponse> loginWithLinkedIn(
            @Valid @RequestBody OAuthRequest request) {

        AuthResponse response = authService.processOAuthLogin(request, "LINKEDIN");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "User Login", description = "Authenticates user credentials and returns JWT access and refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh Access Token", description = "Rotates refresh token and issues a new access token pair (Refresh Token Rotation with reuse detection)")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody com.vionsys.hireai.auth.dto.RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "User Logout", description = "Revokes the active refresh token and invalidates the session")
    @PostMapping("/logout")
    public ResponseEntity<com.vionsys.hireai.auth.dto.LogoutResponse> logout(
            @RequestBody(required = false) com.vionsys.hireai.auth.dto.RefreshTokenRequest request) {

        com.vionsys.hireai.auth.dto.LogoutResponse response = authService.logout(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Forgot Password", description = "Generates a 15-minute time-limited password reset token for the specified user email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        ForgotPasswordResponse response = authService.forgotPassword(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify Reset Token", description = "Validates if a password reset token is active, unused, and not expired")
    @GetMapping("/verify-reset-token")
    public ResponseEntity<VerifyTokenResponse> verifyResetToken(
            @RequestParam("token") String token) {

        VerifyTokenResponse response = authService.verifyResetToken(token);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset Password", description = "Resets the user's password using a valid reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success(null, "Password has been reset successfully. You can now log in with your new password."));
    }

}