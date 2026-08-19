package com.vionsys.hireai.candidate.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.vionsys.hireai.candidate.enums.CandidateStatus;
import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "candidates",
        indexes = {
                @Index(
                        name = "idx_candidate_candidate_id",
                        columnList = "candidate_id"
                ),
                @Index(
                        name = "idx_candidate_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_candidate_phone",
                        columnList = "phone"
                ),
                @Index(
                        name = "idx_candidate_status",
                        columnList = "candidate_status"
                ),
                @Index(
                        name = "idx_candidate_user",
                        columnList = "user_id"
                )
        }
)
@SQLDelete(
        sql = "UPDATE candidates SET deleted = true WHERE id = ?"
)
@SQLRestriction("deleted = false")
public class Candidate extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @Column(
            name = "candidate_id",
            nullable = false,
            unique = true,
            length = 20
    )
    private String candidateId;

    @Column(
            name = "first_name",
            nullable = false,
            length = 50
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 50
    )
    private String lastName;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 100
    )
    private String email;

    @Column(
            name = "phone",
            nullable = false,
            unique = true,
            length = 15
    )
    private String phone;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "current_designation")
    private String currentDesignation;

    @Column(
            name = "experience",
            precision = 4,
            scale = 1
    )
    private BigDecimal experience;

    @Column(
            name = "current_ctc",
            precision = 10,
            scale = 2
    )
    private BigDecimal currentCtc;

    @Column(
            name = "expected_ctc",
            precision = 10,
            scale = 2
    )
    private BigDecimal expectedCtc;

    @Column(name = "notice_period")
    private Integer noticePeriod;

    @Column(name = "location")
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "candidate_status",
            nullable = false
    )
    private CandidateStatus candidateStatus;

    /*
     * Authenticated User ↔ Candidate Profile (Optional for recruiter-managed candidates)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = true,
            unique = true
    )
    private User user;

    /*
     * Soft delete
     */
    @Column(
            name = "deleted",
            nullable = false
    )
    @lombok.Builder.Default
    private boolean deleted = false;

    /*
     * Candidate ↔ Resume
     */
    @OneToOne(
            mappedBy = "candidate",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    private Resume resume;

    /*
     * Candidate ↔ Skills
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "candidate_skills",
            joinColumns = @JoinColumn(
                    name = "candidate_uuid"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "skill_uuid"
            )
    )
    @lombok.Builder.Default
    private Set<Skill> skills = new HashSet<>();
}