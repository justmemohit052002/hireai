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

        User recruiterUser = User.builder().id(recruiterUserId).build();
        RecruiterProfile recruiterProfile = RecruiterProfile.builder().user(recruiterUser).build();
        Job job = Job.builder().id(jobId).recruiterProfile(recruiterProfile).build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertTrue(jobSecurityEvaluator.isJobOwner(jobId, recruiterUserId));
        assertFalse(jobSecurityEvaluator.isJobOwner(jobId, UUID.randomUUID()));
    }

    @Test
    void testApplicationSecurityEvaluator_AccessAndManage() {
        UUID appId = UUID.randomUUID();
        UUID candidateUserId = UUID.randomUUID();
        UUID recruiterUserId = UUID.randomUUID();
        UUID strangerUserId = UUID.randomUUID();

        User candidateUser = User.builder().id(candidateUserId).build();
        Candidate candidate = Candidate.builder().user(candidateUser).build();

        User recruiterUser = User.builder().id(recruiterUserId).build();
        RecruiterProfile recruiterProfile = RecruiterProfile.builder().user(recruiterUser).build();
        Job job = Job.builder().recruiterProfile(recruiterProfile).build();

        JobApplication application = JobApplication.builder()
                .id(appId)
                .candidate(candidate)
                .job(job)
                .build();

        when(jobApplicationRepository.findById(appId)).thenReturn(Optional.of(application));

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

        User user = User.builder().id(userId).build();
        Candidate candidate = Candidate.builder().id(candidateId).user(user).build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        assertTrue(candidateSecurityEvaluator.isCandidateOwner(candidateId, userId));
        assertFalse(candidateSecurityEvaluator.isCandidateOwner(candidateId, UUID.randomUUID()));

        assertTrue(candidateSecurityEvaluator.isSelf(userId, userId));
        assertFalse(candidateSecurityEvaluator.isSelf(userId, UUID.randomUUID()));
    }
}
