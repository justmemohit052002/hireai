package com.vionsys.hireai.candidate.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.ai.dto.resume.AiResumeParsedResult;
import com.vionsys.hireai.candidate.dto.ResumeResponse;

public interface ResumeService {

    ResumeResponse uploadResume(UUID candidateId, MultipartFile file);

    ResumeResponse uploadMyResume(UUID userId, MultipartFile file);

    ResumeResponse getResume(UUID candidateId);

    ResumeResponse getMyResume(UUID userId);

    ResumeResponse getResumeStatus(UUID candidateId);

    Resource downloadResume(UUID candidateId);

    Resource downloadMyResume(UUID userId);

    void deleteResume(UUID candidateId);

    void deleteMyResume(UUID userId);

    void processParsedResumeResult(UUID resumeId, AiResumeParsedResult parsedResult);
}
