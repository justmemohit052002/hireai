package com.vionsys.hireai.chat.mapper;

import java.util.UUID;

import com.vionsys.hireai.chat.dto.response.ChatMessageResponse;
import com.vionsys.hireai.chat.dto.response.ConversationSummaryResponse;
import com.vionsys.hireai.chat.dto.response.UserSummaryDTO;
import com.vionsys.hireai.chat.entity.DirectChatMessage;
import com.vionsys.hireai.chat.entity.DirectConversation;
import com.vionsys.hireai.user.entity.User;

public final class ChatMapper {

    private ChatMapper() {
        // Prevent instantiation
    }

    public static UserSummaryDTO toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null && user.getRole().getName() != null 
                        ? user.getRole().getName().name() 
                        : null)
                .build();
    }

    public static ChatMessageResponse toChatMessageResponse(DirectChatMessage message) {
        if (message == null) {
            return null;
        }

        String senderName = message.getSender() != null 
                ? (message.getSender().getFirstName() + " " + message.getSender().getLastName()).trim()
                : "Unknown";

        String recipientName = message.getRecipient() != null 
                ? (message.getRecipient().getFirstName() + " " + message.getRecipient().getLastName()).trim()
                : "Unknown";

        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(senderName)
                .recipientId(message.getRecipient() != null ? message.getRecipient().getId() : null)
                .recipientName(recipientName)
                .messageText(message.getMessageText())
                .messageType(message.getMessageType())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .attachmentSize(message.getAttachmentSize())
                .status(message.getStatus())
                .metadataJson(message.getMetadataJson())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ConversationSummaryResponse toConversationSummary(
            DirectConversation conversation,
            UUID currentUserId,
            Integer unreadCount,
            Boolean isOnline
    ) {
        if (conversation == null) {
            return null;
        }

        // Identify partner (the other participant)
        User partner = conversation.getRecruiter() != null && conversation.getRecruiter().getId().equals(currentUserId)
                ? conversation.getCandidate()
                : conversation.getRecruiter();

        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .partner(toUserSummary(partner))
                .jobId(conversation.getJob() != null ? conversation.getJob().getId() : null)
                .jobTitle(conversation.getJob() != null ? conversation.getJob().getTitle() : null)
                .jobApplicationId(conversation.getJobApplication() != null ? conversation.getJobApplication().getId() : null)
                .lastMessageText(conversation.getLastMessageText())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount != null ? unreadCount : 0)
                .isOnline(isOnline != null ? isOnline : false)
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
