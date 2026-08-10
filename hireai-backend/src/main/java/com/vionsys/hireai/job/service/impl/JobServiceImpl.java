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

    @Override
    public JobResponse createJob(JobRequest request) {

        RecruiterProfile recruiterProfile = getCurrentRecruiterProfile();

        Job job = JobMapper.toEntity(request);

        job.setRecruiterProfile(recruiterProfile);
        job.setStatus(JobStatus.OPEN);

        Job savedJob = jobRepository.save(job);

        return JobMapper.toResponse(savedJob);
    }





    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs() {

        RecruiterProfile recruiterProfile = getCurrentRecruiterProfile();

        return jobRepository.findByRecruiterProfileId(
                        recruiterProfile.getId())
                .stream()
                .map(JobMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {

        RecruiterProfile recruiterProfile = getCurrentRecruiterProfile();

        Job job = jobRepository.findByIdAndRecruiterProfileId(
                        jobId,
                        recruiterProfile.getId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        return JobMapper.toResponse(job);
    }

    @Override
    public JobResponse updateJob(
            UUID jobId,
            JobRequest request) {

        RecruiterProfile recruiterProfile = getCurrentRecruiterProfile();

        Job job = jobRepository.findByIdAndRecruiterProfileId(
                        jobId,
                        recruiterProfile.getId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setEmploymentType(request.getEmploymentType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setLocation(request.getLocation());
        job.setRemote(request.getRemote());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setCurrency(request.getCurrency());
        job.setSkills(request.getSkills());
        job.setEducation(request.getEducation());
        job.setOpenings(request.getOpenings());
        job.setApplicationDeadline(request.getApplicationDeadline());

        return JobMapper.toResponse(job);
    }

    @Override
    public void closeJob(UUID jobId) {

        RecruiterProfile recruiterProfile = getCurrentRecruiterProfile();

        Job job = jobRepository.findByIdAndRecruiterProfileId(
                        jobId,
                        recruiterProfile.getId())
                .orElseThrow(() ->
                        new JobNotFoundException("Job not found"));

        job.setStatus(JobStatus.CLOSED);
    }



    private RecruiterProfile getCurrentRecruiterProfile() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return recruiterProfileRepository.findByUserId(userDetails.getId())
                .orElseThrow(() ->
                        new RecruiterProfileNotFoundException(
                                "Recruiter profile not found"));
    }
}
