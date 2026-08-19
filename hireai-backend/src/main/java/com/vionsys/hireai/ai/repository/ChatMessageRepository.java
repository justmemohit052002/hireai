package com.vionsys.hireai.ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vionsys.hireai.ai.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
