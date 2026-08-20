package com.vionsys.hireai.ai.dto.chatbot;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageRequest {
    private String candidateId;
    private List<AiChatTurn> conversationHistory;
    private String newMessage;
}
