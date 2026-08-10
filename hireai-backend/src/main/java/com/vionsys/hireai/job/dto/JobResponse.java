package com.vionsys.hireai.job.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.job.enums.EmploymentType;
import com.vionsys.hireai.job.enums.ExperienceLevel;
import com.vionsys.hireai.job.enums.JobStatus;

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
public class JobResponse {

    private UUID id;

    private UUID recruiterProfileId;

    private String companyName;

    private String title;

    private String description;

    private EmploymentType employmentType;

    private ExperienceLevel experienceLevel;

    private String location;

    private Boolean remote;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String currency;

    private List<String> skills;

    private String education;

    private Integer openings;

    private LocalDate applicationDeadline;

    private JobStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}