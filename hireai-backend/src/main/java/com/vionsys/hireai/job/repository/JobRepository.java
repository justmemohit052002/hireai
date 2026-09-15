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

    /**
     * Check if a job is owned by the recruiter with the given user ID.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(j) > 0 FROM Job j WHERE j.id = :jobId AND j.recruiterProfile.user.id = :userId")
    boolean existsByIdAndRecruiterUserId(@org.springframework.data.repository.query.Param("jobId") UUID jobId, @org.springframework.data.repository.query.Param("userId") UUID userId);
}