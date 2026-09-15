package com.vionsys.hireai.job.service;

import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;

public interface JobService {

    /**
     * Create a new job for the authenticated recruiter.
     */
    JobResponse createJob(JobRequest request);

    /**
     * Get all jobs posted by the authenticated recruiter.
     */
    List<JobResponse> getMyJobs();

    /**
     * Get all currently open jobs.
     * Used by candidates to browse available jobs.
     */
    List<JobResponse> getOpenJobs();

    /**
     * Get a job by its id.
     */
    JobResponse getJobById(UUID jobId);

    /**
     * Update an existing job.
     */
    JobResponse updateJob(
            UUID jobId,
            JobRequest request
    );

    /**
     * Close a job.
     */
    void closeJob(UUID jobId);
}