package com.vionsys.hireai.candidate.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadRequest {
	 @NotNull(message = "Resume file is required")
	    private MultipartFile resume;
}
