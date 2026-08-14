package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

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
public class CandidateRequest {

    /*
     * Existing User account associated with this candidate.
     *
     * This is required because Candidate.user is mandatory.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(
            max = 50,
            message = "First name must not exceed 50 characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(
            max = 50,
            message = "Last name must not exceed 50 characters"
    )
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(
            max = 100,
            message = "Email must not exceed 100 characters"
    )
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must contain exactly 10 digits"
    )
    private String phone;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private String currentCompany;

    private String currentDesignation;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Experience cannot be negative"
    )
    private BigDecimal experience;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Current CTC cannot be negative"
    )
    private BigDecimal currentCtc;

    @DecimalMin(
            value = "0.0",
            inclusive = true,
            message = "Expected CTC cannot be negative"
    )
    private BigDecimal expectedCtc;

    @NotNull(message = "Notice period is required")
    private Integer noticePeriod;

    private String location;

    private Set<UUID> skillIds;
}