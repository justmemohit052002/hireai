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
@Schema(description = "Add Role to Account Request Payload")
public class AddRoleRequest {

    @NotBlank(message = "Role is required")
    @Schema(description = "Role to add to the account (e.g. CANDIDATE, RECRUITER)", example = "RECRUITER")
    private String role;

    @Schema(description = "Optional company name if adding RECRUITER role", example = "Acme Technologies")
    private String companyName;
}
