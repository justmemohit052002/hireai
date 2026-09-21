package com.vionsys.hireai.application.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.application.dto.AtsMatchResult;
import com.vionsys.hireai.application.dto.JobApplicationRequest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.dto.UpdateApplicationStatusRequest;
import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.application.mapper.JobApplicationMapper;
import com.vionsys.hireai.application.repository.JobApplicationRepository;
import com.vionsys.hireai.application.service.AtsMatchScoringService;
import com.vionsys.hireai.application.service.JobApplicationService;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.exception.DuplicateResourceException;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.exception.ApplicationNotFoundException;
import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.exception.JobNotFoundException;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.repository.JobRepository;

import org.springframework.web.multipart.MultipartFile;

import com.vionsys.hireai.application.config.AtsProperties;
import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.candidate.service.ResumeService;
import com.vionsys.hireai.candidate.util.CandidateIdGenerator;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final CandidateIdGenerator candidateIdGenerator;
    private final AtsMatchScoringService atsMatchScoringService;
    private final AtsProperties atsProperties;
    private final ResumeService resumeService;
    private final com.vionsys.hireai.email.EmailService emailService;

    @Override
    public JobApplicationResponse applyToJob(UUID candidateUserId, UUID jobId, JobApplicationRequest request) {
        return applyToJob(candidateUserId, jobId, request, null);
    }

    @Override
    public JobApplicationResponse applyToJob(UUID candidateUserId, UUID jobId, JobApplicationRequest request, MultipartFile resumeFile) {

        Candidate candidate = candidateRepository.findByUserId(candidateUserId)
                .orElseGet(() -> createDefaultCandidateForUser(candidateUserId));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalArgumentException("This job posting is currently closed and not accepting new applications.");
        }

        if (jobApplicationRepository.existsByJobIdAndCandidateId(jobId, candidate.getId())) {
            throw new DuplicateResourceException("You have already submitted an application for this job posting.");
        }

        // 1. If candidate provided a new resume file during application, upload & parse it only AFTER all validations pass
        if (resumeFile != null && !resumeFile.isEmpty()) {
            log.info("Uploading and processing new resume file for candidate user {} during job application", candidateUserId);
            resumeService.uploadMyResume(candidateUserId, resumeFile);
        }

        // Run ATS Intelligent Match Scoring
        AtsMatchResult atsResult = atsMatchScoringService.computeAtsScore(candidate, job);
        int matchScore = atsResult.getMatchScore();
        log.info("Calculated ATS Match Score {}% for Candidate {} on Job {}",
                matchScore, candidate.getCandidateId(), job.getTitle());

        // Automated Workflow Rule: If ATS Match Score >= threshold (70%), shortlist for interview; otherwise reject
        ApplicationStatus initialStatus;
        String autoNotes;
        if (matchScore >= atsProperties.getShortlistThreshold()) {
            initialStatus = ApplicationStatus.SHORTLISTED;
            autoNotes = String.format("Shortlisted for interview by AI ATS (Match Score: %d%% >= %d%% threshold)",
                    matchScore, atsProperties.getShortlistThreshold());
            log.info("Candidate {} automatically SHORTLISTED for Job {} (Score: {}%)",
                    candidate.getCandidateId(), job.getTitle(), matchScore);
        } else {
            initialStatus = ApplicationStatus.REJECTED;
            autoNotes = String.format("Application Rejected: ATS Skill Match Score (%d%%) is below the required %d%% threshold",
                    matchScore, atsProperties.getShortlistThreshold());
            log.info("Candidate {} REJECTED for Job {} (Score: {}% < {}%)",
                    candidate.getCandidateId(), job.getTitle(), matchScore, atsProperties.getShortlistThreshold());
        }

        JobApplication application = JobApplication.builder()
                .job(job)
                .candidate(candidate)
                .status(initialStatus)
                .atsMatchScore(matchScore)
                .matchingSkills(String.join(", ", atsResult.getMatchingSkills()))
                .missingSkills(String.join(", ", atsResult.getMissingSkills()))
                .coverNote(request != null ? request.getCoverNote() : null)
                .recruiterNotes(autoNotes)
                .build();

        JobApplication saved = jobApplicationRepository.save(application);
        JobApplicationResponse response = JobApplicationMapper.toResponse(saved);

        // Send automated email notifications asynchronously using thread-safe DTO
        try {
            String recruiterEmail = job.getRecruiterProfile() != null && job.getRecruiterProfile().getCompanyEmail() != null
                    ? job.getRecruiterProfile().getCompanyEmail()
                    : (job.getRecruiterProfile() != null && job.getRecruiterProfile().getUser() != null
                            ? job.getRecruiterProfile().getUser().getEmail()
                            : null);

            emailService.sendApplicationReceivedToCandidate(response);
            if (recruiterEmail != null && !recruiterEmail.isBlank()) {
                emailService.sendNewApplicantAlertToRecruiter(response, recruiterEmail);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch application notification emails: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getCandidateApplications(UUID candidateUserId) {
        return jobApplicationRepository.findByCandidateUserId(candidateUserId)
                .stream()
                .map(JobApplicationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getJobApplications(UUID recruiterUserId, UUID jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        if (job.getRecruiterProfile() == null ||
                job.getRecruiterProfile().getUser() == null ||
                !job.getRecruiterProfile().getUser().getId().equals(recruiterUserId)) {
            throw new AccessDeniedException("You do not have permission to view applicants for this job.");
        }

        return jobApplicationRepository.findByJobIdOrderByScoreDesc(jobId)
                .stream()
                .map(JobApplicationMapper::toResponse)
                .toList();
    }

    @Override
    public JobApplicationResponse updateApplicationStatus(UUID recruiterUserId, UUID applicationId, UpdateApplicationStatusRequest request) {

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found"));

        if (application.getJob().getRecruiterProfile() == null ||
                application.getJob().getRecruiterProfile().getUser() == null ||
                !application.getJob().getRecruiterProfile().getUser().getId().equals(recruiterUserId)) {
            throw new AccessDeniedException("You do not have permission to update this application status.");
        }

        application.setStatus(request.getStatus());
        if (request.getRecruiterNotes() != null && !request.getRecruiterNotes().isBlank()) {
            application.setRecruiterNotes(request.getRecruiterNotes());
        }

        JobApplication updated = jobApplicationRepository.save(application);
        JobApplicationResponse response = JobApplicationMapper.toResponse(updated);

        // Send automated status update email to candidate asynchronously
        try {
            emailService.sendStatusUpdateToCandidate(response, request.getStatus(), request.getRecruiterNotes());
        } catch (Exception ex) {
            log.warn("Failed to dispatch status update email: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationById(UUID currentUserId, UUID applicationId) {

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found"));

        boolean isCandidateOwner = application.getCandidate().getUser() != null &&
                application.getCandidate().getUser().getId().equals(currentUserId);

        boolean isRecruiterOwner = application.getJob().getRecruiterProfile() != null &&
                application.getJob().getRecruiterProfile().getUser() != null &&
                application.getJob().getRecruiterProfile().getUser().getId().equals(currentUserId);

        if (!isCandidateOwner && !isRecruiterOwner) {
            throw new AccessDeniedException("You do not have permission to view this application.");
        }

        return JobApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource downloadApplicationResume(UUID currentUserId, UUID applicationId) {

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found"));

        boolean isCandidateOwner = application.getCandidate().getUser() != null &&
                application.getCandidate().getUser().getId().equals(currentUserId);

        boolean isRecruiterOwner = application.getJob().getRecruiterProfile() != null &&
                application.getJob().getRecruiterProfile().getUser() != null &&
                application.getJob().getRecruiterProfile().getUser().getId().equals(currentUserId);

        if (!isCandidateOwner && !isRecruiterOwner) {
            throw new AccessDeniedException("You do not have permission to download this application's resume.");
        }

        return resumeService.downloadResume(application.getCandidate().getId());
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
