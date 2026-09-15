package com.vionsys.hireai.auth.dto;

import java.time.LocalDateTime;

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
public class ForgotPasswordResponse {

    private boolean success;
    private String message;
    private String resetToken;
    private LocalDateTime expiresAt;
}
