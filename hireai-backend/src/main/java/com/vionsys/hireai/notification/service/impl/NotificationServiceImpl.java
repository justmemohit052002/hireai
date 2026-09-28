package com.vionsys.hireai.notification.service.impl;

import java.util.UUID;

import com.vionsys.hireai.exception.CandidateNotFoundException;
import com.vionsys.hireai.exception.NotificationNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vionsys.hireai.candidate.entity.Candidate;
import com.vionsys.hireai.candidate.repository.CandidateRepository;
import com.vionsys.hireai.notification.dto.NotificationResponse;
import com.vionsys.hireai.notification.dto.SendNotificationRequest;
import com.vionsys.hireai.notification.entity.Notification;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;
import com.vionsys.hireai.notification.mapper.NotificationMapper;
import com.vionsys.hireai.notification.repository.NotificationRepository;
import com.vionsys.hireai.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final CandidateRepository candidateRepository;

    private final NotificationMapper notificationMapper;

    // =========================================================
    // HR / RECRUITER → SEND NOTIFICATION
    // =========================================================

    @Override
    public NotificationResponse sendNotification(
            SendNotificationRequest request) {

        Candidate candidate =
                candidateRepository
                        .findById(request.getCandidateId())
                        .orElseThrow(() ->
                                new CandidateNotFoundException(
                                        "Candidate not found with id: "
                                                + request.getCandidateId()
                                )
                        );

        Notification notification =
                notificationMapper.toEntity(request);

        Notification savedNotification =
                notificationRepository.save(notification);

        return notificationMapper.toResponse(
                savedNotification
        );
    }

    // =========================================================
    // CANDIDATE → MY NOTIFICATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            int page,
            int size,
            String sortBy,
            String sortDir,
            NotificationReadStatus readStatus) {

        UUID candidateId = getCurrentCandidateId();

        Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDir)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(direction, sortBy)
                );

        Page<Notification> notifications;

        if (readStatus != null) {

            notifications =
                    notificationRepository
                            .findByCandidateIdAndReadStatus(
                                    candidateId,
                                    readStatus,
                                    pageable
                            );

        } else {

            notifications =
                    notificationRepository
                            .findByCandidateId(
                                    candidateId,
                                    pageable
                            );
        }

        return notifications.map(
                notificationMapper::toResponse
        );
    }

    // =========================================================
    // CANDIDATE → UNREAD COUNT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {

        UUID candidateId =
                getCurrentCandidateId();

        return notificationRepository
                .countByCandidateIdAndReadStatus(
                        candidateId,
                        NotificationReadStatus.UNREAD
                );
    }

    // =========================================================
    // CANDIDATE → MARK AS READ
    // =========================================================

    @Override
    public NotificationResponse markAsRead(
            UUID notificationId) {

        UUID candidateId =
                getCurrentCandidateId();

        Notification notification =
                notificationRepository
                        .findByIdAndCandidateId(
                                notificationId,
                                candidateId
                        )
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        "Notification not found with id: " + notificationId
                                )
                        );

        notification.setReadStatus(
                NotificationReadStatus.READ
        );

        Notification updatedNotification =
                notificationRepository.save(
                        notification
                );

        return notificationMapper.toResponse(
                updatedNotification
        );
    }

    // =========================================================
    // GET CURRENT CANDIDATE ID FROM JWT
    // =========================================================

    private UUID getCurrentCandidateId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "User is not authenticated"
            );
        }

        String email =
                authentication.getName();

        Candidate candidate =
                candidateRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Candidate profile not found"
                                )
                        );

        return candidate.getId();
    }
}