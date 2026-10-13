package com.vionsys.hireai.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import com.vionsys.hireai.exception.AccountLockedException;
import com.vionsys.hireai.exception.AccountNotVerifiedException;
import com.vionsys.hireai.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.auth.dto.AuthResponse;
import com.vionsys.hireai.auth.dto.LoginRequest;
import com.vionsys.hireai.auth.dto.OAuthRequest;
import com.vionsys.hireai.auth.dto.RecruiterRegisterRequest;
import com.vionsys.hireai.auth.dto.RegisterRequest;
import com.vionsys.hireai.auth.service.AuthService;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Test
    void testRegisterCandidate_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("candidate@example.com");
        request.setPassword("Password123!");
        request.setPhoneNumber("9876543210");

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Doe")
                .email("candidate@example.com")
                .role("CANDIDATE")
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();

        when(authService.registerCandidate(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register/candidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.email").value("candidate@example.com"));
    }

    @Test
    void testRegisterRecruiter_Success() throws Exception {
        RecruiterRegisterRequest request = new RecruiterRegisterRequest();
        request.setFirstName("Alice");
        request.setLastName("Smith");
        request.setEmail("recruiter@vionsys.com");
        request.setPassword("SecurePass123!");
        request.setCompanyName("Vionsys Technologies");

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("recruiter@vionsys.com")
                .role("RECRUITER")
                .accessToken("mock-recruiter-token")
                .refreshToken("mock-recruiter-refresh-token")
                .build();

        when(authService.registerRecruiter(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register/recruiter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-recruiter-token"));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password123!");

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .role("CANDIDATE")
                .accessToken("mock-logged-in-token")
                .refreshToken("mock-refresh-token")
                .build();

        when(authService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-logged-in-token"));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        com.vionsys.hireai.auth.dto.RefreshTokenRequest request = new com.vionsys.hireai.auth.dto.RefreshTokenRequest();
        request.setRefreshToken("mock-valid-refresh-token");

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .role("CANDIDATE")
                .accessToken("mock-new-access-token")
                .refreshToken("mock-new-refresh-token")
                .build();

        when(authService.refreshToken(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-new-refresh-token"));
    }

    @Test
    void testLogout_Success() throws Exception {
        com.vionsys.hireai.auth.dto.RefreshTokenRequest request = new com.vionsys.hireai.auth.dto.RefreshTokenRequest();
        request.setRefreshToken("mock-valid-refresh-token");

        com.vionsys.hireai.auth.dto.LogoutResponse logoutResponse = com.vionsys.hireai.auth.dto.LogoutResponse.builder()
                .success(true)
                .message("User logged out successfully. Session has been terminated.")
                .build();

        when(authService.logout(any())).thenReturn(logoutResponse);

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testVerifyEmailOtp_Success() throws Exception {
        com.vionsys.hireai.auth.dto.VerifyEmailOtpRequest request = com.vionsys.hireai.auth.dto.VerifyEmailOtpRequest.builder()
                .email("candidate@example.com")
                .otp("123456")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("candidate@example.com")
                .role("CANDIDATE")
                .accessToken("mock-verified-access-token")
                .refreshToken("mock-verified-refresh-token")
                .emailVerified(true)
                .requiresVerification(false)
                .build();

        when(authService.verifyEmailOtp(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/verify-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-verified-access-token"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void testResendEmailOtp_Success() throws Exception {
        com.vionsys.hireai.auth.dto.ResendOtpRequest request = com.vionsys.hireai.auth.dto.ResendOtpRequest.builder()
                .email("candidate@example.com")
                .build();

        mockMvc.perform(post("/auth/resend-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testLoginWithGoogle_Success() throws Exception {
        com.vionsys.hireai.auth.dto.OAuthRequest request = com.vionsys.hireai.auth.dto.OAuthRequest.builder()
                .token("mock-google-token-12345")
                .role("CANDIDATE")
                .email("google.user@example.com")
                .firstName("Google")
                .lastName("Candidate")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("google.user@example.com")
                .role("CANDIDATE")
                .accessToken("mock-google-jwt-access-token")
                .refreshToken("mock-google-jwt-refresh-token")
                .emailVerified(true)
                .requiresVerification(false)
                .build();

        when(authService.processOAuthLogin(any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-google-jwt-access-token"))
                .andExpect(jsonPath("$.email").value("google.user@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void testLoginWithLinkedIn_Success() throws Exception {
        com.vionsys.hireai.auth.dto.OAuthRequest request = com.vionsys.hireai.auth.dto.OAuthRequest.builder()
                .token("mock-linkedin-token-67890")
                .role("RECRUITER")
                .email("linkedin.recruiter@example.com")
                .firstName("LinkedIn")
                .lastName("Recruiter")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("linkedin.recruiter@example.com")
                .role("RECRUITER")
                .accessToken("mock-linkedin-jwt-access-token")
                .refreshToken("mock-linkedin-jwt-refresh-token")
                .emailVerified(true)
                .requiresVerification(false)
                .build();

        when(authService.processOAuthLogin(any(), any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/oauth/linkedin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-linkedin-jwt-access-token"))
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void testLogin_UnregisteredEmail_Returns404NotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("unregistered@example.com");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        when(authService.login(any())).thenThrow(new UserNotFoundException("User not found. Please sign up first."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found. Please sign up first."));
    }

    @Test
    void testLogin_WrongPassword_Returns401Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("registered@example.com");
        request.setPassword("WrongPassword123!");
        request.setRole("CANDIDATE");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLogin_UnverifiedAccount_Returns403Forbidden() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@example.com");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        when(authService.login(any())).thenThrow(new AccountNotVerifiedException(
                "Your email address has not been verified yet. Please enter the 6-digit verification code sent to your email."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_VERIFIED"));
    }

    @Test
    void testLogin_AccountLocked_Returns429TooManyRequests() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        when(authService.login(any())).thenThrow(new AccountLockedException(
                "Account temporarily locked due to consecutive failed attempts", 60));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void testLogin_EmptyEmail_Returns400BadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLogin_EmptyPassword_Returns400BadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("valid@example.com");
        request.setPassword("");
        request.setRole("CANDIDATE");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLogin_MalformedEmail_Returns400BadRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("not-a-valid-email");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testLogin_RecruiterSuccess_Returns200Ok() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("recruiter@vionsys.com");
        request.setPassword("Password123!");
        request.setRole("RECRUITER");

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("recruiter@vionsys.com")
                .role("RECRUITER")
                .accessToken("mock-recruiter-access-token")
                .refreshToken("mock-recruiter-refresh-token")
                .build();

        when(authService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.email").value("recruiter@vionsys.com"));
    }
}
