package com.vionsys.hireai.candidate.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vionsys.hireai.ai.client.AiEngineClient;
import com.vionsys.hireai.ai.config.AiEngineProperties;
import com.vionsys.hireai.ai.dto.resume.AiJobAcceptedResponse;
import com.vionsys.hireai.ai.dto.resume.AiJobStatusResponse;
import com.vionsys.hireai.ai.dto.resume.AiResumeParseRequest;
import com.vionsys.hireai.ai.dto.resume.AiResumeParsedResult;
import com.vionsys.hireai.candidate.dto.ResumeResponse;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.entity.Resume;
import com.vionsys.hireai.candidate.entity.Skill;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.exception.ResumeNotFoundException;
import com.vionsys.hireai.candidate.mapper.ResumeMapper;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.repository.SkillRepository;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.candidate.service.ResumeTextExtractorService;
import com.vionsys.hireai.candidate.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResumeServiceImpl implements ResumeService {

    @Value("${resume.max-file-size:10485760}")
    private long maxFileSize;

    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;
    private final SkillRepository skillRepository;
    private final ResumeMapper resumeMapper;
    private final FileStorageService fileStorageService;
    private final ResumeTextExtractorService textExtractorService;
    private final AiEngineClient aiEngineClient;
    private final AiEngineProperties aiEngineProperties;
    private final ObjectMapper objectMapper;

    @Override
    public ResumeResponse uploadResume(UUID candidateId, MultipartFile file) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with id: " + candidateId));

        return handleResumeUpload(candidate, file);
    }

    @Override
    public ResumeResponse uploadMyResume(UUID userId, MultipartFile file) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate profile not found for authenticated user."));

        return handleResumeUpload(candidate, file);
    }

    private ResumeResponse handleResumeUpload(Candidate candidate, MultipartFile file) {
        validateFile(file);

        // 1. Extract plain text from PDF/DOCX file using Apache Tika
        String extractedText = textExtractorService.extractText(file);
        log.info("Extracted {} characters of text from resume for candidate {}",
                extractedText.length(), candidate.getCandidateId());

        try {
            // 2. Store physical file on disk
            String filePath = fileStorageService.store(file);
            String storedFileName = Paths.get(filePath).getFileName().toString();

            // 3. Find existing resume or create new
            Resume resume = resumeRepository.findByCandidateId(candidate.getId())
                    .orElse(new Resume());

            // If an older physical file exists, delete it safely
            if (resume.getFilePath() != null && !resume.getFilePath().equals(filePath)) {
                try {
                    fileStorageService.delete(resume.getFilePath());
                } catch (Exception ex) {
                    log.warn("Failed to delete older resume file: {}", ex.getMessage());
                }
            }

            resume.setCandidate(candidate);
            resume.setOriginalFileName(file.getOriginalFilename());
            resume.setStoredFileName(storedFileName);
            resume.setFileType(file.getContentType());
            resume.setFileSize(file.getSize());
            resume.setFilePath(filePath);
            resume.setRawText(extractedText);
            resume.setUploadedAt(LocalDateTime.now());
            resume.setResumeStatus(ResumeStatus.PARSING);
            resume.setDeleted(false);

            // 4. Submit to AI Engine for async parsing
            AiResumeParseRequest parseRequest = AiResumeParseRequest.builder()
                    .candidateId(candidate.getCandidateId())
                    .resumeText(extractedText)
                    .build();

            Optional<AiJobAcceptedResponse> acceptedResponse = aiEngineClient.submitResumeForParsing(parseRequest);

            if (acceptedResponse.isPresent() && acceptedResponse.get().getJobId() != null) {
                String jobId = acceptedResponse.get().getJobId();
                resume.setAiJobId(jobId);
                resume.setResumeStatus(ResumeStatus.PARSING);
                log.info("AI Resume parsing job {} initiated for candidate {}", jobId, candidate.getCandidateId());

                Resume savedResume = resumeRepository.save(resume);

                // 5. Trigger async polling task in background
                triggerAsyncPolling(savedResume.getId(), jobId);

                return resumeMapper.toResponse(savedResume);
            } else {
                log.warn("AI Engine parsing job submission failed or offline. Resume saved with status UPLOADED.");
                resume.setResumeStatus(ResumeStatus.UPLOADED);
                Resume savedResume = resumeRepository.save(resume);
                return resumeMapper.toResponse(savedResume);
            }

        } catch (IOException ex) {
            throw new FileStorageException("Failed to store resume file: " + ex.getMessage(), ex);
        }
    }

    private void triggerAsyncPolling(UUID resumeId, String jobId) {
        CompletableFuture.runAsync(() -> {
            int attempts = 0;
            int maxAttempts = aiEngineProperties.getPollingMaxAttempts();
            long intervalMs = aiEngineProperties.getPollingIntervalMs();

            while (attempts < maxAttempts) {
                try {
                    Thread.sleep(intervalMs);
                    attempts++;

                    Optional<AiJobStatusResponse> statusOpt = aiEngineClient.checkResumeParseStatus(jobId);
                    if (statusOpt.isPresent()) {
                        AiJobStatusResponse statusResponse = statusOpt.get();
                        String jobStatus = statusResponse.getStatus();

                        if ("complete".equalsIgnoreCase(jobStatus) && statusResponse.getResult() != null) {
                            log.info("AI Resume parsing job {} COMPLETED successfully on attempt {}", jobId, attempts);
                            processParsedResumeResult(resumeId, statusResponse.getResult());
                            break;
                        } else if ("failed".equalsIgnoreCase(jobStatus)) {
                            log.warn("AI Resume parsing job {} FAILED on attempt {}", jobId, attempts);
                            markResumeParseFailed(resumeId);
                            break;
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    log.error("Error during AI parsing polling for job {}: {}", jobId, ex.getMessage());
                }
            }
        });
    }

    @Override
    public void processParsedResumeResult(UUID resumeId, AiResumeParsedResult parsedResult) {
        Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
        if (resumeOpt.isEmpty()) {
            return;
        }

        Resume resume = resumeOpt.get();
        resume.setResumeStatus(ResumeStatus.PARSED);
        resume.setParsedDomain(parsedResult.getDomain());
        resume.setParsedRole(parsedResult.getCurrentRole());
        if (parsedResult.getYearsExperience() != null) {
            resume.setParsedExperience(BigDecimal.valueOf(parsedResult.getYearsExperience()));
        }

        try {
            resume.setParsedDataJson(objectMapper.writeValueAsString(parsedResult));
        } catch (Exception ex) {
            log.warn("Could not serialize parsedDataJson: {}", ex.getMessage());
        }

        // Auto-enrich Candidate profile with skills if present
        Candidate candidate = resume.getCandidate();
        if (candidate != null && parsedResult.getSkills() != null && !parsedResult.getSkills().isEmpty()) {
            Set<Skill> existingSkills = candidate.getSkills() != null
                    ? new HashSet<>(candidate.getSkills())
                    : new HashSet<>();

            for (String skillName : parsedResult.getSkills()) {
                if (skillName == null || skillName.isBlank()) continue;
                String trimmedName = skillName.trim();
                Skill skill = skillRepository.findByNameIgnoreCase(trimmedName)
                        .orElseGet(() -> skillRepository.save(Skill.builder().name(trimmedName).build()));
                existingSkills.add(skill);
            }
            candidate.setSkills(existingSkills);

            if (candidate.getCurrentDesignation() == null && parsedResult.getCurrentRole() != null) {
                candidate.setCurrentDesignation(parsedResult.getCurrentRole());
            }
            if (candidate.getExperience() == null && parsedResult.getYearsExperience() != null) {
                candidate.setExperience(BigDecimal.valueOf(parsedResult.getYearsExperience()));
            }
            candidateRepository.save(candidate);
        }

        resumeRepository.save(resume);
        log.info("Successfully updated resume {} with AI parsed result", resumeId);
    }

    private void markResumeParseFailed(UUID resumeId) {
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            resume.setResumeStatus(ResumeStatus.PARSE_FAILED);
            resumeRepository.save(resume);
        });
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Resume file cannot be empty.");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("Resume file size exceeds maximum limit of " + (maxFileSize / (1024 * 1024)) + " MB.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new FileStorageException("Resume file name is missing.");
        }

        String lowerName = fileName.toLowerCase();
        boolean validExt = lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc");

        if (!validExt) {
            throw new FileStorageException("Only PDF, DOCX, and DOC resume files are allowed.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResume(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMyResume(UUID userId) {
        Resume resume = resumeRepository.findByCandidateUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for candidate."));
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeStatus(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadResume(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));

        try {
            return fileStorageService.loadAsResource(resume.getFilePath());
        } catch (IOException ex) {
            throw new FileStorageException("Could not read resume file: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadMyResume(UUID userId) {
        Resume resume = resumeRepository.findByCandidateUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for authenticated candidate."));

        try {
            return fileStorageService.loadAsResource(resume.getFilePath());
        } catch (IOException ex) {
            throw new FileStorageException("Could not read resume file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteResume(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));

        softDeleteResume(resume);
    }

    @Override
    public void deleteMyResume(UUID userId) {
        Resume resume = resumeRepository.findByCandidateUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for authenticated candidate."));

        softDeleteResume(resume);
    }

    private void softDeleteResume(Resume resume) {
        if (resume.getFilePath() != null) {
            try {
                fileStorageService.delete(resume.getFilePath());
            } catch (Exception ex) {
                log.warn("Failed to delete physical resume file during soft-delete: {}", ex.getMessage());
            }
        }
        resume.setDeleted(true);
        resume.setResumeStatus(ResumeStatus.DELETED);
        resumeRepository.save(resume);
    }
}
