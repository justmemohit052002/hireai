package com.vionsys.hireai.candidate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.candidate.enums.ResumeStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private UUID id;

    private String originalFileName;

    private String fileType;

    private Long fileSize;

    private ResumeStatus resumeStatus;

    private LocalDateTime uploadedAt;

    private String aiJobId;

    private String parsedDomain;

    private String parsedRole;

    private BigDecimal parsedExperience;

    private String parsedDataJson;
}
