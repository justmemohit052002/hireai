package com.vionsys.hireai.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.vionsys.hireai.chat.dto.request.CreateConversationRequest;
import com.vionsys.hireai.chat.dto.request.SendMessageRequest;
import com.vionsys.hireai.chat.dto.response.ChatMessageResponse;
import com.vionsys.hireai.chat.dto.response.ConversationSummaryResponse;
import com.vionsys.hireai.chat.dto.response.UnreadCountResponse;
import com.vionsys.hireai.common.dto.PagedResponse;

public interface ChatService {

    ConversationSummaryResponse getOrCreateConversation(UUID currentUserId, CreateConversationRequest request);

    List<ConversationSummaryResponse> getUserConversations(UUID currentUserId);

    ConversationSummaryResponse getConversationById(UUID currentUserId, UUID conversationId);

    PagedResponse<ChatMessageResponse> getConversationMessages(UUID currentUserId, UUID conversationId, Pageable pageable);

    ChatMessageResponse sendMessage(UUID senderId, SendMessageRequest request);

    void markConversationAsRead(UUID currentUserId, UUID conversationId);

    UnreadCountResponse getTotalUnreadCount(UUID currentUserId);
}
