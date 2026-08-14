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

import com.vionsys.hireai.application.config.AtsProperties;

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
    private final AtsMatchScoringService atsMatchScoringService;
    private final AtsProperties atsProperties;

    @Override
    public JobApplicationResponse applyToJob(UUID candidateUserId, UUID jobId, JobApplicationRequest request) {

        Candidate candidate = candidateRepository.findByUserId(candidateUserId)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate profile not found. Please create your profile before applying."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));

        if (job.getStatus() != JobStatus.OPEN) {
            throw new IllegalArgumentException("This job posting is currently closed and not accepting new applications.");
        }

        if (jobApplicationRepository.existsByJobIdAndCandidateId(jobId, candidate.getId())) {
            throw new DuplicateResourceException("You have already submitted an application for this job posting.");
        }

        // Run ATS Intelligent Match Scoring
        AtsMatchResult atsResult = atsMatchScoringService.computeAtsScore(candidate, job);
        int matchScore = atsResult.getMatchScore();
        log.info("Calculated ATS Match Score {}% for Candidate {} on Job {}",
                matchScore, candidate.getCandidateId(), job.getTitle());

        // Automated Workflow Rule: If ATS Match Score >= threshold (e.g. 70%), auto-shortlist
        ApplicationStatus initialStatus = ApplicationStatus.APPLIED;
        String autoNotes = null;
        if (matchScore >= atsProperties.getShortlistThreshold()) {
            initialStatus = ApplicationStatus.SHORTLISTED;
            autoNotes = String.format("Auto-shortlisted by AI ATS (Match Score: %d%% >= %d%% threshold)",
                    matchScore, atsProperties.getShortlistThreshold());
            log.info("Candidate {} automatically SHORTLISTED for Job {} (Score: {}%)",
                    candidate.getCandidateId(), job.getTitle(), matchScore);
        } else {
            autoNotes = String.format("ATS Match Score: %d%% (Awaiting manual recruiter review)", matchScore);
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
        return JobApplicationMapper.toResponse(saved);
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
        return JobApplicationMapper.toResponse(updated);
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
}
