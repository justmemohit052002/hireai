package com.vionsys.hireai.candidate.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.candidate.enums.ResumeStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeResponse {

	private UUID id;

    private String originalFileName;

    private String fileType;

    private Long fileSize;

    private ResumeStatus resumeStatus;

    private LocalDateTime uploadedAt;
}
