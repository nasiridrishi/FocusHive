package com.focushive.chat.entity;

import com.focushive.chat.enums.MessageType;
import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a chat message in a hive.
 * Enhanced with threading, reactions, and attachment support.
 */
@Entity
@Table(name = "chat_messages",
       indexes = {
           @Index(name = "idx_chat_messages_hive", columnList = "hive_id"),
           @Index(name = "idx_chat_messages_sender", columnList = "sender_id"),
           @Index(name = "idx_chat_messages_created", columnList = "created_at"),
           @Index(name = "idx_chat_messages_parent", columnList = "parent_message_id"),
           @Index(name = "idx_chat_messages_thread", columnList = "thread_id"),
           @Index(name = "idx_chat_messages_type", columnList = "message_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ChatMessage extends BaseEntity {

    @Column(name = "hive_id", nullable = false)
    private String hiveId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false)
    @Builder.Default
    private boolean edited = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    // Threading support
    @Column(name = "parent_message_id")
    private String parentMessageId;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "reply_count")
    @Builder.Default
    private Integer replyCount = 0;

    // Reaction count (denormalized for performance)
    @Column(name = "reaction_count")
    @Builder.Default
    private Integer reactionCount = 0;

    // Attachment count (denormalized for performance)
    @Column(name = "attachment_count")
    @Builder.Default
    private Integer attachmentCount = 0;

    // Read receipts tracking
    @Column(name = "read_by_count")
    @Builder.Default
    private Integer readByCount = 0;

    // Pinned message flag
    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @Column(name = "pinned_by_user_id")
    private String pinnedByUserId;

    // Relationships
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageReaction> reactions;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageAttachment> attachments;

    // Parent message for threading
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id", insertable = false, updatable = false)
    private ChatMessage parentMessage;

    // Thread this message belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", insertable = false, updatable = false)
    private ChatThread thread;

    /**
     * Check if this is a reply message.
     */
    @Transient
    public boolean isReply() {
        return parentMessageId != null;
    }

    /**
     * Check if this message has replies.
     */
    @Transient
    public boolean hasReplies() {
        return replyCount != null && replyCount > 0;
    }

    /**
     * Check if this message has reactions.
     */
    @Transient
    public boolean hasReactions() {
        return reactionCount != null && reactionCount > 0;
    }

    /**
     * Check if this message has attachments.
     */
    @Transient
    public boolean hasAttachments() {
        return attachmentCount != null && attachmentCount > 0;
    }

    /**
     * Increment reply count.
     */
    public void incrementReplyCount() {
        this.replyCount = (this.replyCount == null ? 0 : this.replyCount) + 1;
    }

    /**
     * Increment reaction count.
     */
    public void incrementReactionCount() {
        this.reactionCount = (this.reactionCount == null ? 0 : this.reactionCount) + 1;
    }

    /**
     * Decrement reaction count.
     */
    public void decrementReactionCount() {
        this.reactionCount = Math.max(0, (this.reactionCount == null ? 0 : this.reactionCount) - 1);
    }

    /**
     * Pin the message.
     */
    public void pin(String userId) {
        this.pinned = true;
        this.pinnedAt = LocalDateTime.now();
        this.pinnedByUserId = userId;
    }

    /**
     * Unpin the message.
     */
    public void unpin() {
        this.pinned = false;
        this.pinnedAt = null;
        this.pinnedByUserId = null;
    }
}