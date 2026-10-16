package com.vionsys.hireai.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.security.jwt.JwtAccessDeniedHandler;
import com.vionsys.hireai.security.jwt.JwtAuthenticationEntryPoint;
import com.vionsys.hireai.security.jwt.JwtAuthenticationFilter;
import com.vionsys.hireai.security.jwt.JwtService;
import com.vionsys.hireai.user.dto.ChangePasswordRequest;
import com.vionsys.hireai.user.dto.DeleteAccountRequest;
import com.vionsys.hireai.user.dto.SetPasswordRequest;
import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ProfilePhotoStorageService photoStorageService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Test
    @DisplayName("GET /users/me - Returns 200 and current user profile with avatarUrl")
    void getCurrentUser_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("CANDIDATE")
                .avatarUrl("http://localhost:8080/api/v1/users/" + userId + "/avatar")
                .build();

        when(userService.getCurrentUser()).thenReturn(response);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost:8080/api/v1/users/" + userId + "/avatar"));
    }

    @Test
    @DisplayName("PUT /users/me/change-password - Returns 200 on valid password payload")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPass@123")
                .newPassword("NewPass@456")
                .confirmPassword("NewPass@456")
                .build();

        doNothing().when(userService).changePassword(any(ChangePasswordRequest.class));

        mockMvc.perform(put("/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("PUT /users/me/change-password - Returns 400 when fields are blank or invalid")
    void changePassword_ValidationFailure() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("")
                .newPassword("short")
                .confirmPassword("")
                .build();

        mockMvc.perform(put("/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/me/set-password/request - Returns 200 on OTP dispatch")
    void requestSetPasswordOtp_Success() throws Exception {
        doNothing().when(userService).requestSetPasswordOtp();

        mockMvc.perform(post("/users/me/set-password/request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent to your registered email."));
    }

    @Test
    @DisplayName("POST /users/me/set-password/confirm - Returns 200 on valid OTP and passwords")
    void confirmSetPassword_Success() throws Exception {
        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("123456")
                .newPassword("SecurePass123")
                .confirmPassword("SecurePass123")
                .build();

        doNothing().when(userService).confirmSetPassword(any(SetPasswordRequest.class));

        mockMvc.perform(post("/users/me/set-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password configured successfully. You can now log in with email and password."));
    }

    @Test
    @DisplayName("POST /users/me/set-password/confirm - Returns 400 on invalid payload (short OTP, blank password)")
    void confirmSetPassword_ValidationFailure() throws Exception {
        SetPasswordRequest request = SetPasswordRequest.builder()
                .otp("12")
                .newPassword("short")
                .confirmPassword("")
                .build();

        mockMvc.perform(post("/users/me/set-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users/me/avatar - Uploads multipart file and returns 200 with new avatarUrl")
    void uploadAvatar_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", MediaType.IMAGE_PNG_VALUE, "fake-image-content".getBytes());

        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .avatarUrl("http://localhost:8080/api/v1/users/" + userId + "/avatar")
                .build();

        when(userService.uploadAvatar(any())).thenReturn(response);

        mockMvc.perform(multipart("/users/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost:8080/api/v1/users/" + userId + "/avatar"));
    }

    @Test
    @DisplayName("DELETE /users/me/avatar - Clears avatar and returns 200 with null avatarUrl")
    void deleteAvatar_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .avatarUrl(null)
                .build();

        when(userService.deleteAvatar()).thenReturn(response);

        mockMvc.perform(delete("/users/me/avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    @DisplayName("GET /users/{userId}/avatar - Serves binary image stream with correct headers")
    void getUserAvatar_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] imageBytes = "fake-png-binary".getBytes();
        ByteArrayResource resource = new ByteArrayResource(imageBytes);

        when(userService.getAvatarResource(userId)).thenReturn(resource);
        when(userService.getAvatarPath(userId)).thenReturn("profile-photos/" + userId + "/avatar.png");
        when(photoStorageService.determineMediaType(any())).thenReturn(MediaType.IMAGE_PNG);

        mockMvc.perform(get("/users/{userId}/avatar", userId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("GET /users/me/avatar - Serves binary image stream for current user")
    void getCurrentUserAvatar_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] imageBytes = "fake-png-binary".getBytes();
        ByteArrayResource resource = new ByteArrayResource(imageBytes);
        UserResponse response = UserResponse.builder().id(userId).email("test@example.com").build();

        when(userService.getCurrentUser()).thenReturn(response);
        when(userService.getAvatarResource(userId)).thenReturn(resource);
        when(userService.getAvatarPath(userId)).thenReturn("profile-photos/" + userId + "/avatar.png");
        when(photoStorageService.determineMediaType(any())).thenReturn(MediaType.IMAGE_PNG);

        mockMvc.perform(get("/users/me/avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("POST /users/me/delete-account/request - Sends 6-digit verification code")
    void requestDeleteAccountOtp_Success() throws Exception {
        doNothing().when(userService).requestDeleteAccountOtp();

        mockMvc.perform(post("/users/me/delete-account/request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification code sent to your registered email."));
    }

    @Test
    @DisplayName("DELETE /users/me/delete-account - Deletes account with valid confirmation")
    void deleteAccount_Success() throws Exception {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .currentPassword("ValidPass123")
                .confirmationText("DELETE")
                .build();

        doNothing().when(userService).deleteAccount(any(DeleteAccountRequest.class));

        mockMvc.perform(delete("/users/me/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your account has been deleted successfully."));
    }

    @Test
    @DisplayName("POST /users/me/delete-account - Fallback POST deletes account")
    void deleteAccountPost_Success() throws Exception {
        DeleteAccountRequest request = DeleteAccountRequest.builder()
                .otp("123456")
                .confirmationText("DELETE")
                .build();

        doNothing().when(userService).deleteAccount(any(DeleteAccountRequest.class));

        mockMvc.perform(post("/users/me/delete-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Your account has been deleted successfully."));
    }

    @Test
    @DisplayName("GET /users/{userId} - Returns user response for authorized caller")
    void getUserById_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = UserResponse.builder()
                .id(userId)
                .email("target@example.com")
                .firstName("Target")
                .lastName("User")
                .role("CANDIDATE")
                .build();

        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target@example.com"))
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }
}
