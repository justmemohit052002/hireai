package com.vionsys.hireai.user.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.user.dto.ChangePasswordRequest;
import com.vionsys.hireai.user.dto.UpdateUserRequest;
import com.vionsys.hireai.user.dto.UserResponse;

public interface UserService {

    /**
     * Returns the profile of the currently authenticated user.
     */
    UserResponse getCurrentUser();

    /**
     * Returns a user by ID.
     */
    UserResponse getUserById(UUID userId);

    /**
     * Updates the profile of the currently authenticated user.
     */
    UserResponse updateCurrentUser(UpdateUserRequest request);

    /**
     * Changes password for the currently authenticated user and revokes active refresh tokens.
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Sends a 6-digit OTP verification code to the authenticated OAuth user's registered email.
     */
    void requestSetPasswordOtp();

    /**
     * Validates OTP and configures the initial password for the authenticated OAuth user.
     */
    void confirmSetPassword(com.vionsys.hireai.user.dto.SetPasswordRequest request);

    /**
     * Uploads and updates profile avatar for the currently authenticated user.
     */
    UserResponse uploadAvatar(MultipartFile file);

    /**
     * Removes the avatar for the currently authenticated user.
     */
    UserResponse deleteAvatar();

    /**
     * Retrieves the profile avatar image resource for a user.
     */
    Resource getAvatarResource(UUID userId);

    /**
     * Retrieves the physical stored avatar photo path for a user.
     */
    String getAvatarPath(UUID userId);

    /**
     * Sends a 6-digit OTP verification code to the authenticated OAuth user's registered email for account deletion.
     */
    void requestDeleteAccountOtp();

    /**
     * Permanently deletes and soft-deletes the authenticated user account, revokes sessions, and cleans up data.
     */
    void deleteAccount(com.vionsys.hireai.user.dto.DeleteAccountRequest request);

}