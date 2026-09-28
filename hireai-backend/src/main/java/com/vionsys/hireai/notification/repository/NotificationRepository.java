package com.vionsys.hireai.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.notification.entity.Notification;
import com.vionsys.hireai.notification.enums.NotificationReadStatus;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    Page<Notification> findByCandidateId(
            UUID candidateId,
            Pageable pageable
    );

    Page<Notification> findByCandidateIdAndReadStatus(
            UUID candidateId,
            NotificationReadStatus readStatus,
            Pageable pageable
    );

    long countByCandidateIdAndReadStatus(
            UUID candidateId,
            NotificationReadStatus readStatus
    );

    Optional<Notification> findByIdAndCandidateId(
            UUID id,
            UUID candidateId
    );
}