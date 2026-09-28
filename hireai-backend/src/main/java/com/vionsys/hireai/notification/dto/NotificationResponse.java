package com.vionsys.hireai.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.notification.enums.NotificationChannel;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;
import com.vionsys.hireai.notification.enums.NotificationType;

public class NotificationResponse {

    private UUID id;

    private UUID candidateId;

    private UUID jobId;

    private UUID applicationId;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationChannel channel;

    private NotificationReadStatus readStatus;

    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

