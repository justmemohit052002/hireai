package com.vionsys.hireai.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.application.enums.ApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Job application detail response with ATS match analytics")
public class JobApplicationResponse {

    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private UUID candidateId;
    private String candidateBusinessId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private ApplicationStatus status;
    private Integer atsMatchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String coverNote;
    private String recruiterNotes;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
