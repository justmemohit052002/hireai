package com.vionsys.hireai.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.dto.SendNotificationRequest;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;
import com.vionsys.hireai.notification.service.NotificationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================
    // HR / RECRUITER → SEND NOTIFICATION
    // =========================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'RECRUITER')")
    public ResponseEntity<ApiResponse<NotificationResponse>>
    sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse response =
                notificationService.sendNotification(
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.created(
                        response,
                        "Notification sent successfully"
                )
        );
    }

    // =========================================================
    // CANDIDATE → MY NOTIFICATIONS
    // =========================================================

    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<
            ApiResponse<Page<NotificationResponse>>>
    getMyNotifications(

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be >= 0")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must not exceed 50")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDir,

            @RequestParam(required = false)
            NotificationReadStatus readStatus) {

        Page<NotificationResponse> notifications =
                notificationService.getMyNotifications(
                        page,
                        size,
                        sortBy,
                        sortDir,
                        readStatus
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        notifications,
                        "Notifications fetched successfully"
                )
        );
    }

    // =========================================================
    // CANDIDATE → UNREAD COUNT
    // =========================================================

    @GetMapping("/my/unread-count")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Long>>
    getUnreadCount() {

        long count =
                notificationService.getUnreadCount();

        return ResponseEntity.ok(
                ApiResponse.success(
                        count,
                        "Unread notification count fetched successfully"
                )
        );
    }

    // =========================================================
    // CANDIDATE → MARK NOTIFICATION AS READ
    // =========================================================

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<
            ApiResponse<NotificationResponse>>
    markAsRead(
            @PathVariable UUID notificationId) {

        NotificationResponse response =
                notificationService.markAsRead(
                        notificationId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Notification marked as read"
                )
        );
    }
}