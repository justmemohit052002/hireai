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
    private final com.vionsys.hireai.notification.service.NotificationService notificationService;

    public JobApplicationServiceImpl(
            JobApplicationRepository jobApplicationRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            UserRepository userRepository,
            CandidateIdGenerator candidateIdGenerator,
            AtsMatchScoringService atsMatchScoringService,
            AtsProperties atsProperties,
            ResumeService resumeService,
            com.vionsys.hireai.email.EmailService emailService,
            com.vionsys.hireai.notification.service.NotificationService notificationService
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.userRepository = userRepository;
        this.candidateIdGenerator = candidateIdGenerator;
        this.atsMatchScoringService = atsMatchScoringService;
        this.atsProperties = atsProperties;
        this.resumeService = resumeService;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

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

        // Automated Workflow Rule: If ATS Match Score >= threshold (70%), shortlist for interview; otherwise keep as APPLIED (under review)
        ApplicationStatus initialStatus;
        String autoNotes;
        if (matchScore >= atsProperties.getShortlistThreshold()) {
            initialStatus = ApplicationStatus.SHORTLISTED;
            autoNotes = String.format("Shortlisted for interview by AI ATS (Match Score: %d%% >= %d%% threshold)",
                    matchScore, atsProperties.getShortlistThreshold());
            log.info("Candidate {} automatically SHORTLISTED for Job {} (Score: {}%)",
                    candidate.getCandidateId(), job.getTitle(), matchScore);
        } else {
            initialStatus = ApplicationStatus.APPLIED;
            autoNotes = String.format("Application Received: ATS Match Score %d%%. Under review by recruiter.",
                    matchScore);
            log.info("Candidate {} application received for Job {} (Score: {}%)",
                    candidate.getCandidateId(), job.getTitle(), matchScore);
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

        // Send real-time in-app notifications
        try {
            String companyName = (job.getRecruiterProfile() != null && job.getRecruiterProfile().getCompanyName() != null)
                    ? job.getRecruiterProfile().getCompanyName()
                    : "HireAI";

            boolean isAutoShortlisted = matchScore >= atsProperties.getShortlistThreshold();

            // 1. Candidate notification
            if (candidate.getUser() != null) {
                String candidateTitle = isAutoShortlisted
                        ? "Application Auto-Shortlisted"
                        : "Application Submitted Successfully";
                String candidateMsg = isAutoShortlisted
                        ? "Congratulations! Your profile is a " + matchScore + "% match for " + job.getTitle() + " at " + companyName + " and was automatically shortlisted!"
                        : "You applied for " + job.getTitle() + " at " + companyName + " (ATS Match: " + matchScore + "%).";

                notificationService.createAndSendNotification(
                        candidate.getUser(),
                        candidateTitle,
                        candidateMsg,
                        isAutoShortlisted
                                ? com.vionsys.hireai.notification.enums.NotificationType.AI_MATCH
                                : com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS,
                        "/candidate/applications"
                );
            }

            // 2. Recruiter notification with shortlist highlighting
            if (job.getRecruiterProfile() != null && job.getRecruiterProfile().getUser() != null) {
                String recruiterTitle = isAutoShortlisted
                        ? "High-Match Candidate Auto-Shortlisted (" + matchScore + "%)"
                        : "New Applicant Received";
                String recruiterMsg = isAutoShortlisted
                        ? "Candidate " + candidate.getFirstName() + " " + candidate.getLastName() + " scored " + matchScore + "% on " + job.getTitle() + " (≥ " + atsProperties.getShortlistThreshold() + "% threshold) - Automatically Shortlisted!"
                        : candidate.getFirstName() + " " + candidate.getLastName() + " applied for " + job.getTitle() + " (Match: " + matchScore + "%).";

                notificationService.createAndSendNotification(
                        job.getRecruiterProfile().getUser(),
                        recruiterTitle,
                        recruiterMsg,
                        isAutoShortlisted
                                ? com.vionsys.hireai.notification.enums.NotificationType.AI_MATCH
                                : com.vionsys.hireai.notification.enums.NotificationType.NEW_APPLICATION,
                        "/recruiter/jobs"
                );

                // 3. Job application capacity milestone check
                long applicantCount = jobApplicationRepository.countByJobId(job.getId());
                if (applicantCount == 10 || applicantCount == 25 || applicantCount == 50 || applicantCount == 100) {
                    notificationService.createAndSendNotification(
                            job.getRecruiterProfile().getUser(),
                            "Job Capacity Milestone",
                            "Your job posting '" + job.getTitle() + "' has reached " + applicantCount + " applicants. Review candidates or update listing status.",
                            com.vionsys.hireai.notification.enums.NotificationType.SYSTEM,
                            "/recruiter/jobs"
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch real-time in-app application notifications: {}", ex.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public List<JobApplicationResponse> getCandidateApplications(UUID candidateUserId) {
        List<JobApplication> applications = jobApplicationRepository.findByCandidateUserId(candidateUserId);
        boolean changed = false;
        for (JobApplication app : applications) {
            // Auto-heal any legacy applications that were prematurely auto-rejected by ATS score rule
            if (app.getStatus() == ApplicationStatus.REJECTED &&
                    app.getRecruiterNotes() != null &&
                    app.getRecruiterNotes().contains("ATS Skill Match Score")) {
                app.setStatus(ApplicationStatus.APPLIED);
                app.setRecruiterNotes(String.format("Application Received: ATS Match Score %d%%. Under review by recruiter.",
                        app.getAtsMatchScore() != null ? app.getAtsMatchScore() : 0));
                changed = true;
            }
        }
        if (changed) {
            jobApplicationRepository.saveAll(applications);
        }
        return applications.stream()
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
    @Transactional
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

        // Send real-time notification to candidate
        try {
            if (application.getCandidate() != null && application.getCandidate().getUser() != null) {
                String title;
                String msg;
                com.vionsys.hireai.notification.enums.NotificationType notifType;

                switch (request.getStatus()) {
                    case SHORTLISTED -> {
                        title = "Congratulations! You're Shortlisted";
                        msg = "Your application for " + application.getJob().getTitle() + " has advanced to the Shortlisted stage.";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS;
                    }
                    case INTERVIEW_SCHEDULED -> {
                        title = "Interview Scheduled";
                        msg = "An interview has been scheduled for " + application.getJob().getTitle() + ". Please review meeting details in your inbox.";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.INTERVIEW;
                    }
                    case OFFERED -> {
                        title = "Job Offer Extended";
                        msg = "Congratulations! A job offer has been released for " + application.getJob().getTitle() + ". View your application details.";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS;
                    }
                    case REJECTED -> {
                        title = "Application Update: " + application.getJob().getTitle();
                        msg = "Thank you for applying. The hiring team has updated your application status.";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS;
                    }
                    case SCREENING -> {
                        title = "Application Under Review";
                        msg = "The hiring team is currently reviewing your resume for " + application.getJob().getTitle() + ".";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS;
                    }
                    default -> {
                        title = "Application Status Updated";
                        msg = "Your application for " + application.getJob().getTitle() + " has moved to: " + request.getStatus() + ".";
                        notifType = com.vionsys.hireai.notification.enums.NotificationType.APPLICATION_STATUS;
                    }
                }

                notificationService.createAndSendNotification(
                        application.getCandidate().getUser(),
                        title,
                        msg,
                        notifType,
                        "/candidate/applications"
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch real-time in-app notification on status update: {}", ex.getMessage());
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

    @Override
    @Transactional
    public JobApplicationResponse withdrawApplication(UUID candidateUserId, UUID applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException("Job application not found"));

        if (application.getCandidate() == null ||
                application.getCandidate().getUser() == null ||
                !application.getCandidate().getUser().getId().equals(candidateUserId)) {
            throw new AccessDeniedException("You do not have permission to withdraw this application.");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setRecruiterNotes("Application withdrawn by candidate.");
        JobApplication updated = jobApplicationRepository.save(application);
        log.info("Candidate user {} withdrew application {}", candidateUserId, applicationId);
        return JobApplicationMapper.toResponse(updated);
    }
}
