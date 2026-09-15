package com.vionsys.hireai.security.evaluator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.user.entity.User;

@ExtendWith(MockitoExtension.class)
class SecurityEvaluatorTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private JobSecurityEvaluator jobSecurityEvaluator;

    @InjectMocks
    private ApplicationSecurityEvaluator applicationSecurityEvaluator;

    @InjectMocks
    private CandidateSecurityEvaluator candidateSecurityEvaluator;

    @Test
    void testJobSecurityEvaluator_OwnerMatches() {
        UUID jobId = UUID.randomUUID();
        UUID recruiterUserId = UUID.randomUUID();
        UUID strangerUserId = UUID.randomUUID();

        when(jobRepository.existsByIdAndRecruiterUserId(jobId, recruiterUserId)).thenReturn(true);
        when(jobRepository.existsByIdAndRecruiterUserId(jobId, strangerUserId)).thenReturn(false);

        assertTrue(jobSecurityEvaluator.isJobOwner(jobId, recruiterUserId));
        assertFalse(jobSecurityEvaluator.isJobOwner(jobId, strangerUserId));
    }

    @Test
    void testApplicationSecurityEvaluator_AccessAndManage() {
        UUID appId = UUID.randomUUID();
        UUID candidateUserId = UUID.randomUUID();
        UUID recruiterUserId = UUID.randomUUID();
        UUID strangerUserId = UUID.randomUUID();

        when(jobApplicationRepository.canAccessApplication(appId, candidateUserId)).thenReturn(true);
        when(jobApplicationRepository.canAccessApplication(appId, recruiterUserId)).thenReturn(true);
        when(jobApplicationRepository.canAccessApplication(appId, strangerUserId)).thenReturn(false);

        when(jobApplicationRepository.canManageApplication(appId, recruiterUserId)).thenReturn(true);
        when(jobApplicationRepository.canManageApplication(appId, candidateUserId)).thenReturn(false);
        when(jobApplicationRepository.canManageApplication(appId, strangerUserId)).thenReturn(false);

        // Candidate who applied can access
        assertTrue(applicationSecurityEvaluator.canAccessApplication(appId, candidateUserId));
        // Recruiter who owns the job can access
        assertTrue(applicationSecurityEvaluator.canAccessApplication(appId, recruiterUserId));
        // Stranger cannot access
        assertFalse(applicationSecurityEvaluator.canAccessApplication(appId, strangerUserId));

        // Only recruiter can manage status
        assertTrue(applicationSecurityEvaluator.canManageApplication(appId, recruiterUserId));
        assertFalse(applicationSecurityEvaluator.canManageApplication(appId, candidateUserId));
        assertFalse(applicationSecurityEvaluator.canManageApplication(appId, strangerUserId));
    }

    @Test
    void testCandidateSecurityEvaluator() {
        UUID candidateId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID strangerUserId = UUID.randomUUID();

        when(candidateRepository.existsByIdAndUserId(candidateId, userId)).thenReturn(true);
        when(candidateRepository.existsByIdAndUserId(candidateId, strangerUserId)).thenReturn(false);

        assertTrue(candidateSecurityEvaluator.isCandidateOwner(candidateId, userId));
        assertFalse(candidateSecurityEvaluator.isCandidateOwner(candidateId, strangerUserId));

        assertTrue(candidateSecurityEvaluator.isSelf(userId, userId));
        assertFalse(candidateSecurityEvaluator.isSelf(userId, UUID.randomUUID()));
    }
}
