package com.vionsys.hireai.auth.dto;

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
public class OAuthUserInfo {

    private String email;
    private String firstName;
    private String lastName;
    private String providerId;
    private String avatarUrl;
    private String provider; // "GOOGLE" or "LINKEDIN"
    private boolean emailVerified;
}
