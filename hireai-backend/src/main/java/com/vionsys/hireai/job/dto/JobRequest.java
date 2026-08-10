package com.vionsys.hireai.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.vionsys.hireai.job.enums.EmploymentType;
import com.vionsys.hireai.job.enums.ExperienceLevel;

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
public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "Job description is required")
    @Size(max = 5000)
    private String description;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @NotBlank(message = "Location is required")
    @Size(max = 150)
    private String location;

    @Builder.Default
    private Boolean remote = false;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salaryMin;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salaryMax;

    @Builder.Default
    private String currency = "INR";

    @NotEmpty(message = "At least one skill is required")
    private List<String> skills;

    @Size(max = 200)
    private String education;

    @NotNull(message = "Number of openings is required")
    @Min(value = 1, message = "Openings must be at least 1")
    private Integer openings;

    @Future(message = "Application deadline must be a future date")
    private LocalDate applicationDeadline;
}