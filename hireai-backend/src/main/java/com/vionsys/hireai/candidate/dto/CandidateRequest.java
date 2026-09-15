package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
@Schema(description = "Talent Directory Candidate Request Payload")
public class CandidateRequest {

    /*
     * Existing User account associated with this candidate.
     *
     * This is required because Candidate.user is mandatory.
     */
    @Schema(description = "UUID of existing registered user", example = "a0000000-0000-0000-0000-000000000001")
    @NotNull(message = "User ID is required")
    private UUID userId;

    @Schema(description = "First name", example = "Alex")
    @NotBlank(message = "First name is required")
    @Size(
            max = 50,
            message = "First name must not exceed 50 characters"
    )
    private String firstName;

    @Schema(description = "Last name", example = "Murphy")
    @NotBlank(message = "Last name is required")
    @Size(
            max = 50,
            message = "Last name must not exceed 50 characters"
    )
    private String lastName;

    @Schema(description = "Email address", example = "alex.murphy@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(
            max = 100,
            message = "Email must not exceed 100 characters"
    )
    private String email;

    @Schema(description = "Phone number with optional country code", example = "+91 9876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9\\s\\-\\(\\)]{7,25}$",
            message = "Invalid phone number format"
    )
    private String phone;

    @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/alex-murphy-dev")
    private String linkedinUrl;

    @Schema(description = "GitHub profile URL", example = "https://github.com/alexmurphy")
    private String githubUrl;

    @Schema(description = "Portfolio URL", example = "https://alexmurphy.dev")
    private String portfolioUrl;

    @Schema(description = "Current employer", example = "Acme Technologies")
    private String currentCompany;

    @Schema(description = "Current job title", example = "Senior Software Engineer")
    private String currentDesignation;

    @Schema(description = "Years of experience", example = "5.5")
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Experience cannot be negative"
    )
    private BigDecimal experience;

    @Schema(description = "Current CTC", example = "1500000")
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Current CTC cannot be negative"
    )
    private BigDecimal currentCtc;

    @Schema(description = "Expected CTC", example = "2200000")
    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Expected CTC cannot be negative"
    )
    private BigDecimal expectedCtc;

    @Schema(description = "Notice period in days", example = "30")
    @NotNull(message = "Notice period is required")
    private Integer noticePeriod;

    @Schema(description = "City / location", example = "Pune, Maharashtra, India")
    private String location;

    @Schema(description = "Professional biography / summary", example = "Senior Full-Stack Engineer with 5+ years of experience...")
    private String bio;

    @Schema(description = "Set of skill UUIDs", example = "[]")
    private Set<UUID> skillIds;
}