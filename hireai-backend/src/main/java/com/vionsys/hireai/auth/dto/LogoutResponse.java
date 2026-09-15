package com.vionsys.hireai.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload after user logout")
public class LogoutResponse {

    @Schema(description = "Indicates whether the logout was successful", example = "true")
    private boolean success;

    @Schema(description = "Informative status message", example = "Successfully logged out. Refresh token has been revoked.")
    private String message;
}
