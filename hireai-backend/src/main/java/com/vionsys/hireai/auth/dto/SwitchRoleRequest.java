package com.vionsys.hireai.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Switch Active Role Request Payload")
public class SwitchRoleRequest {

    @NotBlank(message = "Role is required")
    @Schema(description = "Desired active role (e.g. CANDIDATE, RECRUITER, ROLE_CANDIDATE, ROLE_RECRUITER)", example = "CANDIDATE")
    private String role;
}
