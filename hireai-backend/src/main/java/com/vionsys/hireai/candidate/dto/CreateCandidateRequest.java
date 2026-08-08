package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCandidateRequest {

	@NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid phone number"
    )
    private String phone;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private String currentCompany;

    private String currentDesignation;

    @PositiveOrZero(message = "Experience cannot be negative")
    private BigDecimal experience;

    @PositiveOrZero(message = "Current CTC cannot be negative")
    private BigDecimal currentCtc;

    @PositiveOrZero(message = "Expected CTC cannot be negative")
    private BigDecimal expectedCtc;

    @PositiveOrZero(message = "Notice period cannot be negative")
    private Integer noticePeriod;

    @NotBlank(message = "Location is required")
    private String location;

}
