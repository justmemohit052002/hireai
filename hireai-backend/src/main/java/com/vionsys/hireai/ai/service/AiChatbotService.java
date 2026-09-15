package com.vionsys.hireai.ai.service;

import java.util.List;
import java.util.UUID;

import com.vionsys.hireai.ai.dto.chatbot.AiChatMessageResponse;
import com.vionsys.hireai.ai.dto.chatbot.AiChatTurn;

public interface AiChatbotService {

    AiChatMessageResponse sendMessage(UUID candidateUserId, String newMessage);

    List<AiChatTurn> getConversationHistory(UUID candidateUserId);

    void resetConversation(UUID candidateUserId);
}
