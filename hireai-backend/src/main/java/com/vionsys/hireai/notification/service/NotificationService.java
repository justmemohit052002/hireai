package com.vionsys.hireai.notification.service;

import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.enums.NotificationType;
import com.vionsys.hireai.user.entity.User;

public interface NotificationService {

    NotificationResponse createAndSendNotification(
            User recipient,
            String title,
            String message,
            NotificationType type,
            String link
    );

    List<NotificationResponse> getUserNotifications(UUID userId, int limit);

    long getUnreadCount(UUID userId);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    void deleteNotification(UUID userId, UUID notificationId);

    void clearAllNotifications(UUID userId);
}
