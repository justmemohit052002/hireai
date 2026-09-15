package com.vionsys.hireai.ai.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vionsys.hireai.ai.entity.ChatConversation;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    Optional<ChatConversation> findFirstByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    @Query("SELECT c FROM ChatConversation c WHERE c.candidate.user.id = :userId ORDER BY c.createdAt DESC LIMIT 1")
    Optional<ChatConversation> findLatestByCandidateUserId(@Param("userId") UUID userId);
}
