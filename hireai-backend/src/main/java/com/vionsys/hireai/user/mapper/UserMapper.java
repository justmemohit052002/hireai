package com.vionsys.hireai.user.mapper;

import com.vionsys.hireai.user.dto.UserResponse;
import com.vionsys.hireai.user.entity.User;

public final class UserMapper {

    private UserMapper() {
        // Prevent instantiation
    }

    public static UserResponse toUserResponse(User user) {

        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName().name())
                .build();
    }
}