package com.vionsys.hireai.chat.dto.request;

import java.util.UUID;

import com.vionsys.hireai.chat.enums.MessageType;

import jakarta.validation.constraints.NotNull;
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
public class SendMessageRequest {

    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    private String messageText;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private String attachmentUrl;

    private String attachmentName;

    private Long attachmentSize;

    private String metadataJson;
}
