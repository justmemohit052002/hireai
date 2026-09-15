package com.vionsys.hireai.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vionsys.hireai.chat.entity.DirectChatParticipant;

@Repository("directChatParticipantRepository")
public interface DirectChatParticipantRepository extends JpaRepository<DirectChatParticipant, UUID> {

    Optional<DirectChatParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<DirectChatParticipant> findByConversationId(UUID conversationId);

    @Modifying
    @Query("""
        UPDATE DirectChatParticipant p
        SET p.unreadCount = p.unreadCount + 1
        WHERE p.conversation.id = :conversationId
          AND p.user.id = :recipientId
    """)
    void incrementUnreadCount(@Param("conversationId") UUID conversationId, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query("""
        UPDATE DirectChatParticipant p
        SET p.unreadCount = 0, p.lastReadAt = CURRENT_TIMESTAMP
        WHERE p.conversation.id = :conversationId
          AND p.user.id = :userId
    """)
    void resetUnreadCount(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
