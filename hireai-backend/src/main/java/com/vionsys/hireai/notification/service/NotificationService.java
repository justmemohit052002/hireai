package com.vionsys.hireai.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.dto.SendNotificationRequest;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;

public interface NotificationService {

    NotificationResponse sendNotification(
            SendNotificationRequest request
    );

    Page<NotificationResponse> getMyNotifications(
            int page,
            int size,
            String sortBy,
            String sortDir,
            NotificationReadStatus readStatus
    );

    long getUnreadCount();

    NotificationResponse markAsRead(
            UUID notificationId
    );
}