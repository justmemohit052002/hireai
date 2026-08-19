package com.vionsys.hireai.ai.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.common.base.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interview_assessments")
public class InterviewAssessment extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // "PENDING" | "SUBMITTED" | "EVALUATED"

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InterviewQuestion> questions = new ArrayList<>();
}
