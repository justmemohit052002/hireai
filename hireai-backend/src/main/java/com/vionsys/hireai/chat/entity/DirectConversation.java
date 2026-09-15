package com.vionsys.hireai.chat.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.application.entity.JobApplication;
import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.job.entity.Job;
import com.vionsys.hireai.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "direct_conversations",
        indexes = {
                @Index(name = "idx_dir_conv_recruiter_candidate", columnList = "recruiter_id, candidate_id"),
                @Index(name = "idx_dir_conv_job_app", columnList = "job_application_id"),
                @Index(name = "idx_dir_conv_last_message_at", columnList = "last_message_at")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DirectConversation extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id")
    private JobApplication jobApplication;

    @Column(name = "last_message_text", length = 1000)
    private String lastMessageText;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
