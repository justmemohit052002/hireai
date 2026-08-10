package com.vionsys.hireai.job.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.JobStatus;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Get all jobs posted by a recruiter.
     */
    List<Job> findByRecruiterProfileId(UUID recruiterProfileId);

    /**
     * Get all jobs posted by a recruiter with a specific status.
     */
    List<Job> findByRecruiterProfileIdAndStatus(
            UUID recruiterProfileId,
            JobStatus status);

    /**
     * Get a job by id and recruiter.
     * Used to ensure recruiters can only manage their own jobs.
     */
    Optional<Job> findByIdAndRecruiterProfileId(
            UUID jobId,
            UUID recruiterProfileId);

    /**
     * Get all open jobs.
     */
    List<Job> findByStatus(JobStatus status);
}