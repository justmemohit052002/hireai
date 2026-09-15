package com.vionsys.hireai.application.entity;

import java.util.UUID;

import com.vionsys.hireai.application.enums.ApplicationStatus;
import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.job.entity.Job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "job_applications",
        indexes = {
                @Index(
                        name = "idx_app_job_candidate",
                        columnList = "job_id, candidate_id",
                        unique = true
                ),
                @Index(
                        name = "idx_app_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_app_ats_score",
                        columnList = "ats_match_score"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "ats_match_score", nullable = false)
    private Integer atsMatchScore;

    @Column(name = "matching_skills", length = 1000)
    private String matchingSkills;

    @Column(name = "missing_skills", length = 1000)
    private String missingSkills;

    @Column(name = "cover_note", length = 2000)
    private String coverNote;

    @Column(name = "recruiter_notes", length = 2000)
    private String recruiterNotes;

    @Column(name = "interview_score")
    private Integer interviewScore;

    @Column(name = "chatbot_score")
    private Integer chatbotScore;

    @Column(name = "final_ai_score")
    private Integer finalAiScore;

    @Column(name = "ai_classification", length = 50)
    private String aiClassification; // "shortlist" | "hold" | "reject"

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;
}
