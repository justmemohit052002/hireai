package com.vionsys.hireai.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.application.entity.JobApplication;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @Query("SELECT a FROM JobApplication a WHERE a.candidate.user.id = :userId ORDER BY a.createdAt DESC")
    List<JobApplication> findByCandidateUserId(@Param("userId") UUID userId);

    @Query("SELECT a FROM JobApplication a WHERE a.job.id = :jobId ORDER BY a.atsMatchScore DESC, a.createdAt DESC")
    List<JobApplication> findByJobIdOrderByScoreDesc(@Param("jobId") UUID jobId);

    @Query("SELECT a FROM JobApplication a WHERE a.id = :applicationId AND a.job.recruiterProfile.user.id = :recruiterUserId")
    Optional<JobApplication> findByIdAndRecruiterUserId(
            @Param("applicationId") UUID applicationId,
            @Param("recruiterUserId") UUID recruiterUserId
    );
}
