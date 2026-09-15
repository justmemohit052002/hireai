package com.vionsys.hireai.chat.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.chat.entity.DirectChatMessage;
import com.vionsys.hireai.chat.enums.MessageStatus;

@Repository("directChatMessageRepository")
public interface DirectChatMessageRepository extends JpaRepository<DirectChatMessage, UUID> {

    @Query("""
        SELECT m FROM DirectChatMessage m
        LEFT JOIN FETCH m.sender
        LEFT JOIN FETCH m.recipient
        WHERE m.conversation.id = :conversationId
        ORDER BY m.createdAt DESC
    """)
    Page<DirectChatMessage> findByConversationId(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE DirectChatMessage m
        SET m.status = :newStatus, m.readAt = :readAt
        WHERE m.conversation.id = :conversationId
          AND m.recipient.id = :recipientId
          AND m.status != 'READ'
    """)
    int markMessagesAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("recipientId") UUID recipientId,
            @Param("newStatus") MessageStatus newStatus,
            @Param("readAt") LocalDateTime readAt
    );

    @Query("""
        SELECT COUNT(m) FROM DirectChatMessage m
        WHERE m.recipient.id = :userId
          AND m.status != 'READ'
    """)
    long countTotalUnreadByRecipientId(@Param("userId") UUID userId);
}
