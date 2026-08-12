package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.vionsys.hireai.candidate.enums.CandidateStatus;

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
public class CandidateResponse {

    private UUID id;

    private String candidateId;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private String currentCompany;

    private String currentDesignation;

    private BigDecimal experience;

    private BigDecimal currentCtc;

    private BigDecimal expectedCtc;

    private Integer noticePeriod;

    private String location;

    private CandidateStatus candidateStatus;

    private UUID resumeId;

    private Set<UUID> skillIds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}