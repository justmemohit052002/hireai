package com.vionsys.hireai.user.service;

import java.util.UUID;

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

}