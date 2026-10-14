package com.vionsys.hireai.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Delete Account Request Payload")
public class DeleteAccountRequest {

    @Schema(description = "Current account password (required for password-enabled users)")
    private String currentPassword;

    @Schema(description = "6-digit OTP verification code (required for OAuth-only users)")
    private String otp;

    @Schema(description = "Explicit confirmation phrase, must be 'DELETE'", example = "DELETE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Confirmation text is required and must be 'DELETE'")
    private String confirmationText;
}
