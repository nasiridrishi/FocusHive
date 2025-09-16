package com.focushive.chat.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Entity representing a threaded conversation.
 * Allows grouping of reply messages under a parent message.
 */
@Entity
@Table(name = "chat_threads",
       indexes = {
           @Index(name = "idx_chat_threads_parent", columnList = "parent_message_id"),
           @Index(name = "idx_chat_threads_hive", columnList = "hive_id"),
           @Index(name = "idx_chat_threads_last_activity", columnList = "last_activity_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ChatThread extends BaseEntity {

    @Column(name = "hive_id", nullable = false)
    private String hiveId;

    @Column(name = "parent_message_id", nullable = false)
    private String parentMessageId;

    @Column(name = "thread_title")
    private String title;

    @Column(name = "reply_count")
    @Builder.Default
    private Integer replyCount = 0;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "last_reply_user_id")
    private String lastReplyUserId;

    @Column(name = "last_reply_username")
    private String lastReplyUsername;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean archived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id", insertable = false, updatable = false)
    private ChatMessage parentMessage;

    @PrePersist
    protected void prePersist() {
        super.prePersist();
        if (lastActivityAt == null) {
            lastActivityAt = LocalDateTime.now();
        }
        if (replyCount == null) {
            replyCount = 0;
        }
        if (archived == null) {
            archived = false;
        }
    }

    /**
     * Update the thread activity when a new reply is added.
     */
    public void updateActivity(String userId, String username) {
        this.replyCount = this.replyCount + 1;
        this.lastActivityAt = LocalDateTime.now();
        this.lastReplyUserId = userId;
        this.lastReplyUsername = username;
    }

    /**
     * Check if thread is active (has recent activity).
     */
    @Transient
    public boolean isActive() {
        return lastActivityAt != null &&
               lastActivityAt.isAfter(LocalDateTime.now().minusDays(7)) &&
               !archived;
    }
}