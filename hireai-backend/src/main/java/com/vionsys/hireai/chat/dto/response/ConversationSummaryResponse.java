package com.vionsys.hireai.chat.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class ConversationSummaryResponse {

    private UUID id;
    private UserSummaryDTO partner;
    private UUID jobId;
    private String jobTitle;
    private UUID jobApplicationId;
    private String lastMessageText;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;
    private Boolean isOnline;
    private LocalDateTime createdAt;
}
