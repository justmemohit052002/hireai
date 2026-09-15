package com.vionsys.hireai.chat.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.chat.enums.MessageStatus;
import com.vionsys.hireai.chat.enums.MessageType;

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
public class ChatMessageResponse {

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderName;
    private UUID recipientId;
    private String recipientName;
    private String messageText;
    private MessageType messageType;
    private String attachmentUrl;
    private String attachmentName;
    private Long attachmentSize;
    private MessageStatus status;
    private String metadataJson;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
