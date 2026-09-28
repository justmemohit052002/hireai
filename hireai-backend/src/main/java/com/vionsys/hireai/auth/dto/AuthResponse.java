package com.vionsys.hireai.auth.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private String role;

    private String accessToken;

    private String refreshToken;

    @Builder.Default
    private Boolean requiresVerification = false;

    @Builder.Default
    private Boolean emailVerified = false;

    private String message;
}