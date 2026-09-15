package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Candidate Profile Creation and Update Request Payload")
public class CandidateProfileRequest {

    @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/alex-murphy-dev")
    private String linkedinUrl;

    @Schema(description = "GitHub profile URL", example = "https://github.com/alexmurphy")
    private String githubUrl;

    @Schema(description = "Personal portfolio URL", example = "https://alexmurphy.dev")
    private String portfolioUrl;

    @Schema(description = "Current employer or company", example = "Acme Technologies")
    private String currentCompany;

    @Schema(description = "Current job title / designation", example = "Senior Software Engineer")
    private String currentDesignation;

    @Schema(description = "Years of total professional experience", example = "5.5")
    @DecimalMin(
            value = "0.0",
            message = "Experience cannot be negative"
    )
    private BigDecimal experience;

    @Schema(description = "Current annual CTC in currency (e.g. INR)", example = "1500000")
    @DecimalMin(
            value = "0.0",
            message = "Current CTC cannot be negative"
    )
    private BigDecimal currentCtc;

    @Schema(description = "Expected annual CTC in currency (e.g. INR)", example = "2200000")
    @DecimalMin(
            value = "0.0",
            message = "Expected CTC cannot be negative"
    )
    private BigDecimal expectedCtc;

    @Schema(description = "Notice period in days", example = "30")
    @Min(
            value = 0,
            message = "Notice period cannot be negative"
    )
    private Integer noticePeriod;

    @Schema(description = "Current residential location / city", example = "Pune, Maharashtra, India")
    private String location;

    @Schema(description = "Skill UUID set (if selecting from predefined skills)", example = "[]")
    private Set<UUID> skillIds;

    @Schema(description = "List of skill names", example = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Microservices\", \"Kafka\"]")
    private List<String> skills;
}