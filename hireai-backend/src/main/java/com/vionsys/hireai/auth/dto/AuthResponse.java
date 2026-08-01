package com.vionsys.hireai.auth.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private String role;

    private String accessToken;

    private String refreshToken;

}