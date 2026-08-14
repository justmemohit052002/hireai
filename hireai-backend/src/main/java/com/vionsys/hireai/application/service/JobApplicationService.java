package com.vionsys.hireai.application.service;

import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.application.dto.JobApplicationRequest;
import com.vionsys.hireai.application.dto.JobApplicationResponse;
import com.vionsys.hireai.application.dto.UpdateApplicationStatusRequest;

public interface JobApplicationService {

    /**
     * Candidate applies to an open job.
     */
    JobApplicationResponse applyToJob(UUID candidateUserId, UUID jobId, JobApplicationRequest request);

    /**
     * Candidate views all their active job applications.
     */
    List<JobApplicationResponse> getCandidateApplications(UUID candidateUserId);

    /**
     * Recruiter views all applications for a specific job posting, ranked by ATS score.
     */
    List<JobApplicationResponse> getJobApplications(UUID recruiterUserId, UUID jobId);

    /**
     * Recruiter updates candidate stage/status (e.g. SCREENING, SHORTLISTED, INTERVIEW_SCHEDULED).
     */
    JobApplicationResponse updateApplicationStatus(UUID recruiterUserId, UUID applicationId, UpdateApplicationStatusRequest request);

    /**
     * Get specific application details by ID (for candidate or recruiter).
     */
    JobApplicationResponse getApplicationById(UUID currentUserId, UUID applicationId);
}
