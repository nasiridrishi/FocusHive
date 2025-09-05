package com.focushive.forum.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"}),
        @UniqueConstraint(columnNames = {"user_id", "category_id"})
    },
    indexes = {
        @Index(name = "idx_forum_subscription_user", columnList = "user_id"),
        @Index(name = "idx_forum_subscription_post", columnList = "post_id"),
        @Index(name = "idx_forum_subscription_category", columnList = "category_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumSubscription extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private ForumPost post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ForumCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    @Builder.Default
    private NotificationType notificationType = NotificationType.ALL;
    
    @Column(name = "email_notifications", nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;
    
    @Column(name = "in_app_notifications", nullable = false)
    @Builder.Default
    private Boolean inAppNotifications = true;
    
    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;
    
    @Column(name = "muted_until")
    private LocalDateTime mutedUntil;
    
    public enum NotificationType {
        ALL,           // All activity
        REPLIES_ONLY,  // Only direct replies
        MENTIONS_ONLY, // Only mentions
        NONE          // No notifications (watching only)
    }
    
    @PrePersist
    @PreUpdate
    private void validate() {
        // Ensure either post or category is set, but not both
        if ((post == null && category == null) || (post != null && category != null)) {
            throw new IllegalArgumentException("Subscription must be for either a post or a category, not both");
        }
    }
    
    public boolean isForPost() {
        return post != null;
    }
    
    public boolean isForCategory() {
        return category != null;
    }
    
    public boolean isActive() {
        if (isMuted) {
            if (mutedUntil == null) {
                return false; // Permanently muted
            }
            return LocalDateTime.now().isAfter(mutedUntil);
        }
        return true;
    }
    
    public boolean shouldNotify(NotificationEvent event) {
        if (!isActive()) {
            return false;
        }
        
        switch (notificationType) {
            case NONE:
                return false;
            case ALL:
                return true;
            case REPLIES_ONLY:
                return event == NotificationEvent.REPLY;
            case MENTIONS_ONLY:
                return event == NotificationEvent.MENTION;
            default:
                return false;
        }
    }
    
    public enum NotificationEvent {
        REPLY,
        MENTION,
        VOTE,
        EDIT,
        ACCEPTED_ANSWER
    }
    
    public void muteTemporarily(int hours) {
        this.isMuted = true;
        this.mutedUntil = LocalDateTime.now().plusHours(hours);
    }
    
    public void mutePermanently() {
        this.isMuted = true;
        this.mutedUntil = null;
    }
    
    public void unmute() {
        this.isMuted = false;
        this.mutedUntil = null;
    }
}