package com.vionsys.hireai.job.mapper;

import java.util.ArrayList;

import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;
import com.vionsys.hireai.job.entity.Job;


public final class JobMapper {

    private JobMapper() {
    }

    public static Job toEntity(JobRequest request) {

        if (request == null) {
            return null;
        }

        return Job.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .employmentType(request.getEmploymentType())
                .experienceLevel(request.getExperienceLevel())
                .location(request.getLocation())
                .remote(request.getRemote())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .currency(request.getCurrency())
                .skills(request.getSkills())
                .education(request.getEducation())
                .openings(request.getOpenings())
                .applicationDeadline(request.getApplicationDeadline())
                .build();
    }

    public static JobResponse toResponse(Job job) {

        if (job == null) {
            return null;
        }

        return JobResponse.builder()
                .id(job.getId())
                .recruiterProfileId(job.getRecruiterProfile().getId())
                .companyName(job.getRecruiterProfile().getCompanyName())
                .title(job.getTitle())
                .description(job.getDescription())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .location(job.getLocation())
                .remote(job.getRemote())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .skills(job.getSkills() != null
                        ? new ArrayList<>(job.getSkills())
                        : null)
                .education(job.getEducation())
                .openings(job.getOpenings())
                .applicationDeadline(job.getApplicationDeadline())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

}