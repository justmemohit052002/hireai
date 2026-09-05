package com.vionsys.hireai.security.evaluator;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vionsys.hireai.job.repository.JobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("jobSecurity")
@RequiredArgsConstructor
public class JobSecurityEvaluator {

    private final JobRepository jobRepository;

    /**
     * Verifies if the authenticated user is the recruiter owner of the specified job.
     */
    public boolean isJobOwner(UUID jobId, UUID userId) {
        if (jobId == null || userId == null) {
            return false;
        }

        return jobRepository.findById(jobId)
                .map(job -> job.getRecruiterProfile() != null
                        && job.getRecruiterProfile().getUser() != null
                        && userId.equals(job.getRecruiterProfile().getUser().getId()))
                .orElse(false);
    }
}
