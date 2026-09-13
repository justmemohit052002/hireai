package com.vionsys.hireai.candidate.mapper;

import org.springframework.stereotype.Component;

import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.entity.Resume;

@Component
public class ResumeMapper {

    public ResumeResponse toResponse(Resume resume) {
        if (resume == null) {
            return null;
        }

        return ResumeResponse.builder()
                .id(resume.getId())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .resumeStatus(resume.getResumeStatus())
                .uploadedAt(resume.getUploadedAt())
                .aiJobId(resume.getAiJobId())
                .parsedDomain(resume.getParsedDomain())
                .parsedRole(resume.getParsedRole())
                .parsedExperience(resume.getParsedExperience())
                .parsedDataJson(resume.getParsedDataJson())
                .build();
    }
}
