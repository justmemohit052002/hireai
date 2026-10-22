package com.vionsys.hireai.candidate.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.candidate.repository.ResumeRepository;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Startup auditor that verifies database-referenced files exist on disk in the unified storage directory.
 * Emits a warning if database entries are present but the directory is empty or missing referenced files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageStartupAuditor implements ApplicationRunner {

    private final StorageProperties storageProperties;
    private final ResumeRepository resumeRepository;
    private final CandidateRepository candidateRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            auditStorageIntegrity();
        } catch (Exception ex) {
            log.warn("Storage startup audit skipped due to inspection exception: {}", ex.getMessage());
        }
    }

    private void auditStorageIntegrity() {
        AtomicInteger totalReferenced = new AtomicInteger(0);
        AtomicInteger missingCount = new AtomicInteger(0);

        resumeRepository.findAll().forEach(resume -> {
            if (resume.getFilePath() != null && !resume.getFilePath().isBlank()) {
                totalReferenced.incrementAndGet();
                if (!checkFileExists(resume.getFilePath(), storageProperties.getResumeUploadPath())) {
                    missingCount.incrementAndGet();
                }
            }
        });

        candidateRepository.findAll().forEach(candidate -> {
            if (candidate.getProfilePhotoPath() != null && !candidate.getProfilePhotoPath().isBlank()) {
                totalReferenced.incrementAndGet();
                if (!checkFileExists(candidate.getProfilePhotoPath(), storageProperties.getPhotoUploadPath())) {
                    missingCount.incrementAndGet();
                }
            }
        });

        recruiterProfileRepository.findAll().forEach(recruiter -> {
            if (recruiter.getProfilePhotoPath() != null && !recruiter.getProfilePhotoPath().isBlank()) {
                totalReferenced.incrementAndGet();
                if (!checkFileExists(recruiter.getProfilePhotoPath(), storageProperties.getPhotoUploadPath())) {
                    missingCount.incrementAndGet();
                }
            }
        });

        if (totalReferenced.get() > 0 && missingCount.get() > 0) {
            log.warn("Storage audit: Database references {} stored file(s), but {} referenced file(s) are missing from storage directory '{}'. " +
                    "Existing files may reside in an alternate working directory (e.g. repo root 'uploads' vs module 'hireai-backend/uploads'). " +
                    "Verify file locations or configure APP_UPLOAD_DIR.",
                    totalReferenced.get(), missingCount.get(), storageProperties.getBaseUploadPath());
        } else if (totalReferenced.get() > 0) {
            log.info("Storage audit passed: all {} database-referenced files verified in storage directory '{}'.",
                    totalReferenced.get(), storageProperties.getBaseUploadPath());
        } else {
            log.info("Storage audit: No files currently referenced in database.");
        }
    }

    private boolean checkFileExists(String pathString, Path expectedDir) {
        try {
            Path p = Paths.get(pathString);
            if (Files.exists(p)) {
                return true;
            }
            Path resolved = expectedDir.resolve(p.getFileName());
            return Files.exists(resolved);
        } catch (Exception ex) {
            return false;
        }
    }
}
