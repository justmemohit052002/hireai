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
@Schema(description = "Change Password Request Payload for Authenticated Users")
public class ChangePasswordRequest {

    @Schema(description = "Current account password", example = "CurrentPassword123!")
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "New account password (min 8 characters)", example = "NewSecurePassword123!")
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;

    @Schema(description = "Confirmation of new password", example = "NewSecurePassword123!")
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
