package com.vionsys.hireai.candidate.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.candidate.enums.ResumeStatus;
import com.vionsys.hireai.common.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "resumes")
public class Resume extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "original_file_name",
            nullable = false
    )
    private String originalFileName;

    @Column(
            name = "stored_file_name",
            nullable = false,
            unique = true
    )
    private String storedFileName;

    @Column(
            name = "file_type",
            nullable = false,
            length = 50
    )
    private String fileType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private Long fileSize;

    @Column(
            name = "file_path",
            nullable = false
    )
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "resume_status",
            nullable = false
    )
    private ResumeStatus resumeStatus;

    @Column(
            name = "uploaded_at",
            nullable = false
    )
    private LocalDateTime uploadedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "candidate_id",
            nullable = false,
            unique = true
    )
    private Candidate candidate;
}