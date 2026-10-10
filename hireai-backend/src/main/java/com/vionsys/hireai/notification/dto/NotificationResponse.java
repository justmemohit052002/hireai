package com.vionsys.hireai.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.notification.enums.NotificationType;

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
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private NotificationType type;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
}
