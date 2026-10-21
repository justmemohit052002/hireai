package com.vionsys.hireai.job.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.exception.JobNotFoundException;
import com.vionsys.hireai.exception.RecruiterProfileNotFoundException;
import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.job.mapper.JobMapper;
import com.vionsys.hireai.job.repository.JobRepository;
import com.vionsys.hireai.job.service.JobService;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;
import com.vionsys.hireai.recruiter.repository.RecruiterProfileRepository;
import com.vionsys.hireai.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final com.vionsys.hireai.application.repository.JobApplicationRepository jobApplicationRepository;
    private final com.vionsys.hireai.notification.service.NotificationService notificationService;
    private final com.vionsys.hireai.candidate.repository.CandidateRepository candidateRepository;
    private final com.vionsys.hireai.email.EmailService emailService;


    // =========================================================
    // CREATE JOB
    // =========================================================

    @Override
    public JobResponse createJob(
            JobRequest request) {

        RecruiterProfile recruiterProfile =
                getCurrentRecruiterProfile();

        Job job =
                JobMapper.toEntity(request);

        job.setRecruiterProfile(
                recruiterProfile
        );

        job.setStatus(
                JobStatus.OPEN
        );

        Job savedJob =
                jobRepository.save(job);

        // Send real-time notifications for job creation
        try {
            // 1. Confirmation to recruiter
            if (recruiterProfile.getUser() != null) {
                notificationService.createAndSendNotification(
                        recruiterProfile.getUser(),
                        "Job Posted Successfully",
                        "Your job posting '" + savedJob.getTitle() + "' is now live and accepting applications.",
                        com.vionsys.hireai.notification.enums.NotificationType.SYSTEM,
                        "/recruiter/jobs"
                );
            }

            // 2. Smart Job Recommendations to candidates
            candidateRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 10))
                    .getContent()
                    .stream()
                    .filter(c -> c.getUser() != null)
                    .forEach(c -> {
                        notificationService.createAndSendNotification(
                                c.getUser(),
                                "Smart Job Recommendation",
                                "New opening for " + savedJob.getTitle() + " at " + recruiterProfile.getCompanyName() + ". Check it out and apply today!",
                                com.vionsys.hireai.notification.enums.NotificationType.AI_MATCH,
                                "/candidate/jobs"
                        );
                    });
        } catch (Exception ex) {
            // Notification failure shouldn't fail job creation
        }

        // Send email confirmation to recruiter
        try {
            String recruiterEmail = recruiterProfile.getUser() != null ? recruiterProfile.getUser().getEmail() : recruiterProfile.getCompanyEmail();
            String recruiterName = recruiterProfile.getUser() != null ? recruiterProfile.getUser().getFirstName() : recruiterProfile.getCompanyName();
            if (recruiterEmail != null && !recruiterEmail.isBlank()) {
                emailService.sendJobPostedConfirmation(recruiterEmail, recruiterName, savedJob.getTitle(), savedJob.getId());
            }
        } catch (Exception ex) {
            // Non-critical email failure shouldn't fail job creation
        }

        return JobMapper.toResponse(
                savedJob,
                0L
        );
    }


    // =========================================================
    // RECRUITER - MY JOBS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs() {

        RecruiterProfile recruiterProfile =
                getCurrentRecruiterProfile();

        return jobRepository
                .findByRecruiterProfileId(
                        recruiterProfile.getId()
                )
                .stream()
                .map(job -> {
                    long count = jobApplicationRepository.countByJobId(job.getId());
                    return JobMapper.toResponse(job, count);
                })
                .toList();
    }


    // =========================================================
    // CANDIDATE - OPEN JOBS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getOpenJobs() {

        return jobRepository
                .findByStatus(JobStatus.OPEN)
                .stream()
                .map(job -> {
                    long count = jobApplicationRepository.countByJobId(job.getId());
                    return JobMapper.toResponse(job, count);
                })
                .toList();
    }


    // =========================================================
    // GET JOB BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(
            UUID jobId) {

        RecruiterProfile recruiterProfile =
                getCurrentRecruiterProfile();

        Job job =
                jobRepository
                        .findByIdAndRecruiterProfileId(
                                jobId,
                                recruiterProfile.getId()
                        )
                        .orElseThrow(() ->
                                new JobNotFoundException(
                                        "Job not found"
                                )
                        );

        long count = jobApplicationRepository.countByJobId(job.getId());
        return JobMapper.toResponse(
                job,
                count
        );
    }


    // =========================================================
    // UPDATE JOB
    // =========================================================

    @Override
    public JobResponse updateJob(
            UUID jobId,
            JobRequest request) {

        RecruiterProfile recruiterProfile =
                getCurrentRecruiterProfile();

        Job job =
                jobRepository
                        .findByIdAndRecruiterProfileId(
                                jobId,
                                recruiterProfile.getId()
                        )
                        .orElseThrow(() ->
                                new JobNotFoundException(
                                        "Job not found"
                                )
                        );

        job.setTitle(
                request.getTitle()
        );

        job.setDescription(
                request.getDescription()
        );

        job.setEmploymentType(
                request.getEmploymentType()
        );

        job.setExperienceLevel(
                request.getExperienceLevel()
        );

        job.setLocation(
                request.getLocation()
        );

        job.setRemote(
                request.getRemote()
        );

        job.setSalaryMin(
                request.getSalaryMin()
        );

        job.setSalaryMax(
                request.getSalaryMax()
        );

        job.setCurrency(
                request.getCurrency() != null ? request.getCurrency() : com.vionsys.hireai.job.enums.Currency.INR
        );

        job.setSkills(
                request.getSkills()
        );

        job.setPreferredSkills(
                request.getPreferredSkills()
        );

        job.setEducation(
                request.getEducation()
        );

        job.setOpenings(
                request.getOpenings()
        );

        job.setApplicationDeadline(
                request.getApplicationDeadline()
        );

        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }

        return JobMapper.toResponse(
                job
        );
    }


    // =========================================================
    // CLOSE JOB
    // =========================================================

    @Override
    public void closeJob(
            UUID jobId) {

        RecruiterProfile recruiterProfile =
                getCurrentRecruiterProfile();

        Job job =
                jobRepository
                        .findByIdAndRecruiterProfileId(
                                jobId,
                                recruiterProfile.getId()
                        )
                        .orElseThrow(() ->
                                new JobNotFoundException(
                                        "Job not found"
                                )
                        );

        job.setStatus(
                JobStatus.CLOSED
        );
    }


    // =========================================================
    // CURRENT RECRUITER
    // =========================================================

    private RecruiterProfile getCurrentRecruiterProfile() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        return recruiterProfileRepository
                .findByUserId(
                        userDetails.getId()
                )
                .orElseThrow(() ->
                        new RecruiterProfileNotFoundException(
                                "Recruiter profile not found"
                        )
                );
    }
}