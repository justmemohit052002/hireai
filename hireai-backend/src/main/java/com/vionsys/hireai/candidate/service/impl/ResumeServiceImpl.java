package com.vionsys.hireai.candidate.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.candidate.exception.FileStorageException;
import com.vionsys.hireai.candidate.exception.ResumeNotFoundException;
import com.vionsys.hireai.candidate.mapper.ResumeMapper;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.candidate.repository.SkillRepository;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.candidate.service.ResumeTextExtractorService;
import com.vionsys.hireai.candidate.storage.FileStorageService;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final CandidateIdGenerator candidateIdGenerator;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final com.vionsys.hireai.application.repository.JobApplicationRepository jobApplicationRepository;
    private final com.vionsys.hireai.application.service.AtsMatchScoringService atsMatchScoringService;
    private final com.vionsys.hireai.application.config.AtsProperties atsProperties;

    @Override
    public ResumeResponse uploadResume(UUID candidateId, MultipartFile file) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with id: " + candidateId));

        return handleResumeUpload(candidate, file);
    }

    @Override
    public ResumeResponse uploadMyResume(UUID userId, MultipartFile file) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultCandidateForUser(userId));

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

            // If an older physical file exists, delete it safely AFTER the transaction successfully commits
            final String oldFilePath = resume.getFilePath();
            if (oldFilePath != null && !oldFilePath.equals(filePath)) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                fileStorageService.delete(oldFilePath);
                                log.info("Deleted older resume file after transaction commit: {}", oldFilePath);
                            } catch (Exception ex) {
                                log.warn("Failed to delete older resume file: {}", ex.getMessage());
                            }
                        }
                    });
                } else {
                    try {
                        fileStorageService.delete(oldFilePath);
                    } catch (Exception ex) {
                        log.warn("Failed to delete older resume file: {}", ex.getMessage());
                    }
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
                            transactionTemplate.executeWithoutResult(status -> {
                                processParsedResumeResult(resumeId, statusResponse.getResult());
                            });
                            break;
                        } else if ("failed".equalsIgnoreCase(jobStatus)) {
                            log.warn("AI Resume parsing job {} FAILED on attempt {}", jobId, attempts);
                            transactionTemplate.executeWithoutResult(status -> {
                                markResumeParseFailed(resumeId);
                            });
                            break;
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    log.error("Error during AI parsing polling for job {}: {}", jobId, ex.getMessage(), ex);
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
                if (skillName == null || skillName.isBlank())
                    continue;
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

            // Auto-recalculate ATS score for all candidate's job applications
            try {
                List<com.vionsys.hireai.application.entity.JobApplication> applications = jobApplicationRepository
                        .findByCandidateId(candidate.getId());
                for (com.vionsys.hireai.application.entity.JobApplication app : applications) {
                    com.vionsys.hireai.application.dto.AtsMatchResult atsResult = atsMatchScoringService
                            .computeAtsScore(candidate, app.getJob());
                    int newScore = atsResult.getMatchScore();
                    app.setAtsMatchScore(newScore);
                    app.setMatchingSkills(String.join(", ", atsResult.getMatchingSkills()));
                    app.setMissingSkills(String.join(", ", atsResult.getMissingSkills()));

                    if (newScore >= atsProperties.getShortlistThreshold()) {
                        app.setStatus(com.vionsys.hireai.application.enums.ApplicationStatus.SHORTLISTED);
                        app.setRecruiterNotes(
                                String.format("Shortlisted for interview by AI ATS (Match Score: %d%% >= %d%% threshold)",
                                        newScore, atsProperties.getShortlistThreshold()));
                    } else {
                        app.setStatus(com.vionsys.hireai.application.enums.ApplicationStatus.REJECTED);
                        app.setRecruiterNotes(
                                String.format("Application Rejected: ATS Skill Match Score (%d%%) is below the required %d%% threshold",
                                        newScore, atsProperties.getShortlistThreshold()));
                    }
                    jobApplicationRepository.save(app);
                    log.info("Auto-updated Application {} with freshly calculated ATS Match Score: {}%", app.getId(),
                            newScore);
                }
            } catch (Exception ex) {
                log.warn("Failed to auto-update ATS score on existing applications: {}", ex.getMessage());
            }
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
            throw new FileStorageException(
                    "Resume file size exceeds maximum limit of " + (maxFileSize / (1024 * 1024)) + " MB.");
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
                .orElseThrow(
                        () -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getMyResume(UUID userId) {
        return resumeRepository.findByCandidateUserIdAndDeletedFalse(userId)
                .map(resumeMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeStatus(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(
                        () -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));
        return resumeMapper.toResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadResume(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(
                        () -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));

        try {
            return fileStorageService.loadAsResource(resume.getFilePath());
        } catch (Exception ex) {
            log.warn("Primary file path {} failed to load for candidate {}: {}. Attempting stored file name fallback...",
                    resume.getFilePath(), candidateId, ex.getMessage());
            if (resume.getStoredFileName() != null && !resume.getStoredFileName().isBlank()) {
                try {
                    return fileStorageService.loadAsResource(resume.getStoredFileName());
                } catch (Exception ignored) {
                }
            }
            throw new FileStorageException("Resume file could not be found or is not readable.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadMyResume(UUID userId) {
        Resume resume = resumeRepository.findByCandidateUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for authenticated candidate."));

        try {
            return fileStorageService.loadAsResource(resume.getFilePath());
        } catch (Exception ex) {
            log.warn("Primary file path {} failed to load for user {}: {}. Attempting stored file name fallback...",
                    resume.getFilePath(), userId, ex.getMessage());
            if (resume.getStoredFileName() != null && !resume.getStoredFileName().isBlank()) {
                try {
                    return fileStorageService.loadAsResource(resume.getStoredFileName());
                } catch (Exception ignored) {
                }
            }
            throw new FileStorageException("Resume file could not be found or is not readable.", ex);
        }
    }

    @Override
    public void deleteResume(UUID candidateId) {
        Resume resume = resumeRepository.findByCandidateIdAndDeletedFalse(candidateId)
                .orElseThrow(
                        () -> new ResumeNotFoundException("Resume not found for candidate with id: " + candidateId));

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

    private Candidate createDefaultCandidateForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate user account not found: " + userId));

        Candidate candidate = Candidate.builder()
                .user(user)
                .candidateId(candidateIdGenerator.generateCandidateId())
                .firstName(user.getFirstName() != null ? user.getFirstName() : "Candidate")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .candidateStatus(CandidateStatus.ACTIVE)
                .deleted(false)
                .build();

        return candidateRepository.save(candidate);
    }
}
