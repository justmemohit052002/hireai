package com.vionsys.hireai.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Set Password Request Payload for OAuth Users")
public class SetPasswordRequest {

    @Schema(description = "6-digit OTP verification code sent to registered email", example = "123456")
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be exactly 6 digits")
    private String otp;

    @Schema(description = "New account password (min 8 characters)", example = "SecurePass123!")
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    @Schema(description = "Confirmation of new password", example = "SecurePass123!")
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
