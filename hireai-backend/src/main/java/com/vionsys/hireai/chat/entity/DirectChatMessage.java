package com.vionsys.hireai.chat.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.chat.enums.MessageStatus;
import com.vionsys.hireai.chat.enums.MessageType;
import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "direct_chat_messages",
        indexes = {
                @Index(name = "idx_dir_msg_conv_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_dir_msg_recipient_status", columnList = "recipient_id, status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatMessage extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
