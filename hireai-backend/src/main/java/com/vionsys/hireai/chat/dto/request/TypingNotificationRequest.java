package com.vionsys.hireai.chat.dto.request;

import java.util.UUID;

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
public class TypingNotificationRequest {

    @NotNull
    private UUID conversationId;

    @NotNull
    private UUID recipientId;

    private boolean isTyping;
}
