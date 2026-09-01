package com.vionsys.hireai.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Candidate Registration Request Payload")
public class RegisterRequest {

    @Schema(description = "Candidate first name", example = "Alex")
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Schema(description = "Candidate last name", example = "Murphy")
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Schema(description = "Candidate email address", example = "alex.murphy@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Candidate account password", example = "Password@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20,
            message = "Password must be between 8 and 20 characters")
    private String password;

    @Schema(description = "Optional 10-digit Indian phone number", example = "9876543210")
    @Pattern(
            regexp = "^$|^[6-9]\\d{9}$",
            message = "Invalid phone number"
    )
    private String phoneNumber;

}