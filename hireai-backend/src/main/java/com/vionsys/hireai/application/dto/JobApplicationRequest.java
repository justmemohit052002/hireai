package com.vionsys.hireai.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body when candidate applies to a job")
public class JobApplicationRequest {

    @Size(max = 2000, message = "Cover note cannot exceed 2000 characters")
    @Schema(description = "Optional cover note or application message", example = "I am excited to apply for this Senior Java position with 5+ years of Spring Boot experience.")
    private String coverNote;
}
