package com.vionsys.hireai.application.dto;

import com.vionsys.hireai.application.enums.ApplicationStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request body when recruiter updates application stage/status")
public class UpdateApplicationStatusRequest {

    @NotNull(message = "Application status is required")
    @Schema(description = "New stage/status in recruitment pipeline", example = "SHORTLISTED")
    private ApplicationStatus status;

    @Size(max = 2000, message = "Recruiter notes cannot exceed 2000 characters")
    @Schema(description = "Optional interview feedback or recruiter notes", example = "Strong background in distributed microservices. Scheduled technical round.")
    private String recruiterNotes;
}
