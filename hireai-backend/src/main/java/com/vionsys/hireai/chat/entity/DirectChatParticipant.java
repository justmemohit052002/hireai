package com.vionsys.hireai.chat.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vionsys.hireai.common.base.BaseEntity;
import com.vionsys.hireai.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "direct_chat_participants",
        indexes = {
                @Index(name = "idx_dir_part_user_conv", columnList = "user_id, conversation_id", unique = true),
                @Index(name = "idx_dir_part_user_unread", columnList = "user_id, unread_count")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DirectChatParticipant extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private Integer unreadCount = 0;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;
}
