package com.focushive.notification.entity;

import com.focushive.notification.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "user_id, is_read, created_at"),
    @Index(name = "idx_notifications_type", columnList = "user_id, type"),
    @Index(name = "idx_notifications_created", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    
    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;
    
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Size(max = 500, message = "Action URL must not exceed 500 characters")
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    @Column(columnDefinition = "jsonb", name = "data")
    @Builder.Default
    private String data = "{}";
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    @Column(length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "delivery_attempts", nullable = false)
    @Builder.Default
    private Integer deliveryAttempts = 0;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Size(max = 500, message = "Failure reason must not exceed 500 characters")
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Archive notification
     */
    public void archive() {
        this.isArchived = true;
        this.archivedAt = LocalDateTime.now();
    }

    /**
     * Mark delivery as successful
     */
    public void markDelivered() {
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * Mark delivery as failed
     */
    public void markDeliveryFailed(String reason) {
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
        this.deliveryAttempts++;
    }

    public enum NotificationPriority {
        LOW, NORMAL, HIGH, URGENT
    }
}