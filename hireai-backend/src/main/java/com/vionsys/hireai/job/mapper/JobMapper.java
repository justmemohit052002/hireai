package com.vionsys.hireai.job.mapper;

import java.util.ArrayList;

import com.vionsys.hireai.job.dto.JobRequest;
import com.vionsys.hireai.job.dto.JobResponse;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.job.enums.Currency;


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
                .currency(request.getCurrency() != null ? request.getCurrency() : Currency.INR)
                .skills(request.getSkills())
                .education(request.getEducation())
                .openings(request.getOpenings())
                .applicationDeadline(request.getApplicationDeadline())
                .build();
    }

    public static JobResponse toResponse(Job job) {
        return toResponse(job, 0L);
    }

    public static JobResponse toResponse(Job job, Long applicantsCount) {

        if (job == null) {
            return null;
        }

        return JobResponse.builder()
                .id(job.getId())
                .recruiterProfileId(job.getRecruiterProfile() != null ? job.getRecruiterProfile().getId() : null)
                .companyName(job.getRecruiterProfile() != null ? job.getRecruiterProfile().getCompanyName() : null)
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
                .applicantsCount(applicantsCount != null ? applicantsCount : 0L)
                .aiParsedCount(applicantsCount != null ? applicantsCount : 0L)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

}