package com.vionsys.hireai.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.vionsys.hireai.job.enums.Currency;
import com.vionsys.hireai.job.enums.EmploymentType;
import com.vionsys.hireai.job.enums.ExperienceLevel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Job Creation and Update Request Payload")
public class JobRequest {

    @Schema(description = "Title of the job opening", example = "Senior Java Backend Engineer")
    @NotBlank(message = "Job title is required")
    @Size(max = 150)
    private String title;

    @Schema(description = "Detailed job description and responsibilities", example = "We are seeking a Senior Java Engineer to lead microservices architecture, REST APIs, and event-driven distributed systems using Spring Boot and PostgreSQL.")
    @NotBlank(message = "Job description is required")
    @Size(max = 5000)
    private String description;

    @Schema(description = "Employment type", example = "FULL_TIME")
    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @Schema(description = "Required experience level", example = "SENIOR")
    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @Schema(description = "Job office location / city", example = "Bengaluru, Karnataka, India")
    @NotBlank(message = "Location is required")
    @Size(max = 150)
    private String location;

    @Schema(description = "Whether the position allows 100% remote work", example = "false")
    @Builder.Default
    private Boolean remote = false;

    @Schema(description = "Minimum annual compensation", example = "1800000")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salaryMin;

    @Schema(description = "Maximum annual compensation", example = "2800000")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salaryMax;

    @Schema(description = "Salary currency (INR, USD, EUR, GBP, AED)", example = "INR")
    @Builder.Default
    private Currency currency = Currency.INR;

    @Schema(description = "List of required technical and functional skills", example = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Docker\", \"Microservices\"]")
    @NotEmpty(message = "At least one skill is required")
    private List<String> skills;

    @Schema(description = "Minimum educational qualification", example = "B.Tech / B.E in Computer Science or equivalent")
    @Size(max = 200)
    private String education;

    @Schema(description = "Number of open positions", example = "2")
    @NotNull(message = "Number of openings is required")
    @Min(value = 1, message = "Openings must be at least 1")
    private Integer openings;

    @Schema(description = "Application deadline date (YYYY-MM-DD)", example = "2026-12-31")
    @Future(message = "Application deadline must be a future date")
    private LocalDate applicationDeadline;
}