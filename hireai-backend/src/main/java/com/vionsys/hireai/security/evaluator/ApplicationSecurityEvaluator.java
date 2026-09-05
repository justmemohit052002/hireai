package com.vionsys.hireai.security.evaluator;

import java.util.UUID;

import org.springframework.stereotype.Component;

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
    public boolean canAccessApplication(UUID applicationId, UUID userId) {
        if (applicationId == null || userId == null) {
            return false;
        }

        return jobApplicationRepository.findById(applicationId)
                .map(app -> {
                    boolean isApplicant = app.getCandidate() != null
                            && app.getCandidate().getUser() != null
                            && userId.equals(app.getCandidate().getUser().getId());

                    boolean isJobOwner = app.getJob() != null
                            && app.getJob().getRecruiterProfile() != null
                            && app.getJob().getRecruiterProfile().getUser() != null
                            && userId.equals(app.getJob().getRecruiterProfile().getUser().getId());

                    return isApplicant || isJobOwner;
                })
                .orElse(false);
    }

    /**
     * Checks if the user is authorized to manage/update the application (must be the recruiter who posted the job).
     */
    public boolean canManageApplication(UUID applicationId, UUID userId) {
        if (applicationId == null || userId == null) {
            return false;
        }

        return jobApplicationRepository.findById(applicationId)
                .map(app -> app.getJob() != null
                        && app.getJob().getRecruiterProfile() != null
                        && app.getJob().getRecruiterProfile().getUser() != null
                        && userId.equals(app.getJob().getRecruiterProfile().getUser().getId()))
                .orElse(false);
    }
}
