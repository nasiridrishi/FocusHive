package com.focushive.chat.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a reaction to a chat message.
 * Allows users to react to messages with emojis.
 */
@Entity
@Table(name = "message_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}),
       indexes = {
           @Index(name = "idx_message_reactions_message", columnList = "message_id"),
           @Index(name = "idx_message_reactions_user", columnList = "user_id"),
           @Index(name = "idx_message_reactions_emoji", columnList = "emoji")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class MessageReaction extends BaseEntity {

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "emoji", nullable = false, length = 10)
    private String emoji;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private ChatMessage message;
}