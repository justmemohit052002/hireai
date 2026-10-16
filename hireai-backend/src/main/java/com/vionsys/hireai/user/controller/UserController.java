package com.vionsys.hireai.user.controller;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.candidate.storage.ProfilePhotoStorageService;
import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.user.dto.ChangePasswordRequest;
import com.vionsys.hireai.user.dto.DeleteAccountRequest;
import com.vionsys.hireai.user.dto.SetPasswordRequest;
import com.vionsys.hireai.user.dto.UpdateUserRequest;
import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/users", "/api/v1/users", "/api/users"})
@RequiredArgsConstructor
@Tag(name = "User Self-Service", description = "Endpoints for authenticated user details, password change, account deletion, and avatar management")
public class UserController {

    private final UserService userService;
    private final ProfilePhotoStorageService photoStorageService;

    @Operation(summary = "Get Current Authenticated User", description = "Fetch user account profile and role details for the logged-in session")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get User by ID", description = "Fetch user information by UUID (Admin or account owner only)")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUB_ADMIN') or (principal != null and #userId.equals(principal.id))")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Current User", description = "Modify first name, last name, or phone number of the authenticated user")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateCurrentUser(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change Password", description = "Securely update the authenticated user's password and revoke active sessions")
    @PutMapping({"/me/change-password", "/change-password"})
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @Operation(summary = "Request Set Password OTP", description = "Sends a 6-digit OTP verification code to the authenticated OAuth user's email")
    @PostMapping({"/me/set-password/request", "/set-password/request"})
    public ResponseEntity<ApiResponse<Void>> requestSetPasswordOtp() {
        userService.requestSetPasswordOtp();
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code sent to your registered email."));
    }

    @Operation(summary = "Confirm Set Password", description = "Verify OTP and configure an account password for an OAuth user")
    @PostMapping({"/me/set-password/confirm", "/set-password/confirm"})
    public ResponseEntity<ApiResponse<Void>> confirmSetPassword(
            @Valid @RequestBody SetPasswordRequest request) {
        userService.confirmSetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password configured successfully. You can now log in with email and password."));
    }

    @Operation(summary = "Request Delete Account OTP", description = "Sends a 6-digit OTP verification code to the authenticated OAuth user's registered email for account deletion")
    @PostMapping({"/me/delete-account/request", "/delete-account/request"})
    public ResponseEntity<ApiResponse<Void>> requestDeleteAccountOtp() {
        userService.requestDeleteAccountOtp();
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code sent to your registered email."));
    }

    @Operation(summary = "Delete Account", description = "Permanently delete and soft-delete the authenticated user account and anonymize personal data")
    @DeleteMapping({"/me/delete-account", "/me", "/delete-account"})
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Your account has been deleted successfully."));
    }

    @Operation(summary = "Delete Account (POST fallback)", description = "Fallback POST endpoint to delete account if HTTP client or proxy does not support DELETE with body")
    @PostMapping({"/me/delete-account", "/delete-account"})
    public ResponseEntity<ApiResponse<Void>> deleteAccountPost(
            @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Your account has been deleted successfully."));
    }

    @Operation(summary = "Upload Profile Avatar", description = "Upload profile picture (.png, .jpg, .jpeg, .webp)")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadAvatar(
            @Parameter(description = "Image file (.png, .jpg, .jpeg, .webp)", required = true)
            @RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadAvatar(file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Profile Avatar", description = "Remove avatar image for the authenticated user")
    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> deleteAvatar() {
        UserResponse response = userService.deleteAvatar();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Current User Avatar Image Binary", description = "Display or download avatar image of current authenticated user")
    @GetMapping({"/me/avatar", "/avatar"})
    public ResponseEntity<Resource> getCurrentUserAvatar() {
        UserResponse currentUser = userService.getCurrentUser();
        Resource resource = userService.getAvatarResource(currentUser.getId());
        String photoPath = userService.getAvatarPath(currentUser.getId());
        MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @Operation(summary = "Get User Avatar Image Binary", description = "Display or download public profile avatar image by user ID")
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getUserAvatar(
            @PathVariable UUID userId) {
        Resource resource = userService.getAvatarResource(userId);
        String photoPath = userService.getAvatarPath(userId);
        MediaType mediaType = photoStorageService.determineMediaType(photoPath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}