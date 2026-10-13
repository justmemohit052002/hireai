package com.vionsys.hireai.notification.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vionsys.hireai.common.dto.ApiResponse;
import com.vionsys.hireai.exception.UserNotFoundException;
import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.enums.NotificationType;
import com.vionsys.hireai.notification.service.NotificationService;
import com.vionsys.hireai.security.CustomUserDetails;
import com.vionsys.hireai.security.annotation.CurrentUser;
import com.vionsys.hireai.user.entity.User;
import com.vionsys.hireai.user.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Real-time user notification management APIs")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Get user notifications list")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @CurrentUser CustomUserDetails currentUser,
            @RequestParam(defaultValue = "30") int limit
    ) {
        List<NotificationResponse> list = notificationService.getUserNotifications(currentUser.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(list, "Notifications retrieved successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notifications count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @CurrentUser CustomUserDetails currentUser
    ) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count), "Unread count retrieved"));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID id
    ) {
        NotificationResponse updated = notificationService.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Notification marked as read"));
    }

    @PatchMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @CurrentUser CustomUserDetails currentUser
    ) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @CurrentUser CustomUserDetails currentUser,
            @PathVariable UUID id
    ) {
        notificationService.deleteNotification(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification deleted"));
    }

    @DeleteMapping("/clear-all")
    @Operation(summary = "Clear all user notifications")
    public ResponseEntity<ApiResponse<Void>> clearAll(
            @CurrentUser CustomUserDetails currentUser
    ) {
        notificationService.clearAllNotifications(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications cleared"));
    }

    @PostMapping("/test")
    @Operation(summary = "Send a test notification to the current user (for real-time testing)")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendTestNotification(
            @CurrentUser CustomUserDetails currentUser,
            @RequestParam(defaultValue = "HireAI Alert") String title,
            @RequestParam(defaultValue = "Real-time notification test successful!") String message
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String link = (user.getRole() != null && user.getRole().getName() != null &&
                user.getRole().getName().name().contains("RECRUITER"))
                ? "/recruiter/dashboard"
                : "/candidate/jobs";

        NotificationResponse response = notificationService.createAndSendNotification(
                user,
                title,
                message,
                NotificationType.SYSTEM,
                link
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Test notification sent successfully"));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate specific real-time notifications for testing all platform scenarios")
    public ResponseEntity<ApiResponse<NotificationResponse>> simulateNotification(
            @CurrentUser CustomUserDetails currentUser,
            @RequestParam(defaultValue = "AI_MATCH") String scenario
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isRecruiter = user.getRole() != null && user.getRole().getName() != null &&
                user.getRole().getName().name().contains("RECRUITER");

        String title;
        String message;
        NotificationType type;
        String link;

        switch (scenario.toUpperCase()) {
            case "NEW_APPLICATION" -> {
                title = "New Candidate Application";
                message = "Candidate Jane Doe applied for Senior Full Stack Engineer (Match: 78%).";
                type = NotificationType.NEW_APPLICATION;
                link = "/recruiter/jobs";
            }
            case "AI_MATCH" -> {
                title = "High-Match Candidate Shortlisted (88%)";
                message = "Candidate Jane Doe scored 88% on Full Stack Engineer (>= 70% threshold) - Automatically Shortlisted!";
                type = NotificationType.AI_MATCH;
                link = isRecruiter ? "/recruiter/jobs" : "/candidate/applications";
            }
            case "INTERVIEW_REMINDER" -> {
                title = "Upcoming Interview in 15 Minutes";
                message = "Your technical interview for Senior Frontend Engineer starts in 15 minutes on Google Meet.";
                type = NotificationType.INTERVIEW;
                link = isRecruiter ? "/recruiter/inbox" : "/candidate/inbox";
            }
            case "STATUS_UPDATE", "OFFER_RELEASED" -> {
                title = "Application Status: Offer Released";
                message = "Congratulations! An official job offer has been extended for your application.";
                type = NotificationType.APPLICATION_STATUS;
                link = "/candidate/applications";
            }
            case "INTERVIEW_INVITE" -> {
                title = "Interview Invitation: Senior React Engineer";
                message = "Jordan Kim invited you for a Technical Screen on Friday, 3:00 PM via Google Meet.";
                type = NotificationType.INTERVIEW;
                link = "/candidate/inbox";
            }
            case "JOB_CAPACITY" -> {
                title = "Job Capacity Alert: Full Stack Developer";
                message = "Your active posting has reached 50 applicants! Review shortlisted candidates before closing.";
                type = NotificationType.SYSTEM;
                link = "/recruiter/jobs";
            }
            case "RESUME_FEEDBACK" -> {
                title = "Resume Parsed and ATS Feedback Ready";
                message = "We extracted 14 skills from your resume. Your ATS profile has been updated with matching recommendations.";
                type = NotificationType.AI_MATCH;
                link = "/candidate/profile";
            }
            case "JOB_RECOMMENDATION" -> {
                title = "Smart Job Recommendation";
                message = "New opening for Staff React Architect at OpenAI matches 95% of your skill profile.";
                type = NotificationType.AI_MATCH;
                link = "/candidate/jobs";
            }
            case "PROFILE_COMPLETION" -> {
                title = "Profile Completion Reminder";
                message = "Complete your profile by uploading your portfolio and experience to increase recruiter visibility!";
                type = NotificationType.SYSTEM;
                link = isRecruiter ? "/recruiter/profile" : "/candidate/profile";
            }
            case "SECURITY_ALERT" -> {
                title = "Security Alert: New Sign-in";
                message = "New sign-in detected on your HireAI account from Chrome on Windows (127.0.0.1).";
                type = NotificationType.SYSTEM;
                link = isRecruiter ? "/recruiter/settings" : "/candidate/settings";
            }
            default -> {
                title = "HireAI Real-Time Alert";
                message = "Real-time notification test successful!";
                type = NotificationType.SYSTEM;
                link = isRecruiter ? "/recruiter/dashboard" : "/candidate/jobs";
            }
        }

        NotificationResponse response = notificationService.createAndSendNotification(
                user,
                title,
                message,
                type,
                link
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Simulated notification dispatched successfully"));
    }
}

