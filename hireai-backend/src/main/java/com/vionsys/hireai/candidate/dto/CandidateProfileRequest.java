package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateProfileRequest {

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private String currentCompany;

    private String currentDesignation;

    @DecimalMin(
            value = "0.0",
            message = "Experience cannot be negative"
    )
    private BigDecimal experience;

    @DecimalMin(
            value = "0.0",
            message = "Current CTC cannot be negative"
    )
    private BigDecimal currentCtc;

    @DecimalMin(
            value = "0.0",
            message = "Expected CTC cannot be negative"
    )
    private BigDecimal expectedCtc;

    @Min(
            value = 0,
            message = "Notice period cannot be negative"
    )
    private Integer noticePeriod;

    private String location;

    private Set<UUID> skillIds;
}