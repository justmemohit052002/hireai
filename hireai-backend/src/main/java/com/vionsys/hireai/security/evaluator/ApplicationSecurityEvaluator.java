package com.vionsys.hireai.security.evaluator;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.application.repository.JobApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("applicationSecurity")
@RequiredArgsConstructor
public class ApplicationSecurityEvaluator {

    private final JobApplicationRepository jobApplicationRepository;

    /**
     * Checks if the user is authorized to view an application (either the applicant candidate or the job recruiter).
     */
    @Transactional(readOnly = true)
    public boolean canAccessApplication(UUID applicationId, UUID userId) {
        if (applicationId == null || userId == null) {
            return false;
        }

        return jobApplicationRepository.canAccessApplication(applicationId, userId);
    }

    /**
     * Checks if the user is authorized to manage/update the application (must be the recruiter who posted the job).
     */
    @Transactional(readOnly = true)
    public boolean canManageApplication(UUID applicationId, UUID userId) {
        if (applicationId == null || userId == null) {
            return false;
        }

        return jobApplicationRepository.canManageApplication(applicationId, userId);
    }
}
