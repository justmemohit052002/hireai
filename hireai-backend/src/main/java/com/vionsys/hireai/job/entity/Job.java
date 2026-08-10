package com.vionsys.hireai.job.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.job.enums.EmploymentType;
import com.vionsys.hireai.job.enums.ExperienceLevel;
import com.vionsys.hireai.job.enums.JobStatus;
import com.vionsys.hireai.recruiter.entity.RecruiterProfile;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_profile_id", nullable = false)
    private RecruiterProfile recruiterProfile;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExperienceLevel experienceLevel;

    @Column(nullable = false, length = 150)
    private String location;

    @Default
    @Column(nullable = false)
    private Boolean remote = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Builder.Default
    @Column(length = 10, nullable = false)
    private String currency = "INR";

    @ElementCollection
    @CollectionTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", nullable = false)
    private List<String> skills;

    @Column(length = 200)
    private String education;

    @Column(nullable = false)
    private Integer openings;

    private LocalDate applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.OPEN;
}