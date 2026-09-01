package com.vionsys.hireai.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Recruiter Registration Request Payload")
public class RecruiterRegisterRequest {

    @Schema(description = "First name of the recruiter", example = "Sarah")
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Schema(description = "Last name of the recruiter", example = "Connor")
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Schema(description = "Work email address", example = "sarah.connor@cyberdyne.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Account password (8-20 characters)", example = "Password@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String password;

    @Schema(description = "Company or employer name", example = "Cyberdyne Systems Inc.")
    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String companyName;

}
