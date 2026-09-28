
        package com.vionsys.hireai.notification.entity;

import java.util.UUID;

import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.notification.enums.NotificationChannel;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;
import com.vionsys.hireai.notification.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "notifications",
        indexes = {

                @Index(
                        name = "idx_notification_candidate",
                        columnList = "candidate_id"
                ),

                @Index(
                        name = "idx_notification_job",
                        columnList = "job_id"
                ),

                @Index(
                        name = "idx_notification_application",
                        columnList = "application_id"
                ),

                @Index(
                        name = "idx_notification_created_at",
                        columnList = "created_at"
                ),

                @Index(
                        name = "idx_notification_candidate_read",
                        columnList = "candidate_id, read_status"
                )
        }
)
@SQLDelete(
        sql = "UPDATE notifications SET deleted = true WHERE id = ?"
)
@SQLRestriction("deleted = false")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * We are storing IDs instead of direct JPA relationships.
     * This keeps the notification module loosely coupled with
     * Candidate, Job and Application entities.
     */
    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "read_status",
            nullable = false,
            length = 20
    )
    private NotificationReadStatus readStatus;

    /*
     * Soft delete flag.
     *
     * BaseEntity does not contain deleted,
     * so we keep it here.
     */
    @Column(nullable = false)
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {

        if (readStatus == null) {
            readStatus = NotificationReadStatus.UNREAD;
        }
    }

    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationReadStatus getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(NotificationReadStatus readStatus) {
        this.readStatus = readStatus;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
