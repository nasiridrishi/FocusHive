package com.focushive.notification.entity;

import com.focushive.notification.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Entity representing user notification preferences.
 * Allows users to customize how they receive different types of notifications.
 */
@Entity
@Table(name = "notification_preferences", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notification_type"}),
       indexes = {
           @Index(name = "idx_notification_preferences_user", columnList = "user_id"),
           @Index(name = "idx_notification_preferences_type", columnList = "notification_type")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    /**
     * ID of the user these preferences belong to
     */
    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of notification these preferences apply to
     */
    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    /**
     * Whether in-app notifications are enabled for this type
     */
    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    /**
     * Whether email notifications are enabled for this type
     */
    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    /**
     * Whether push notifications are enabled for this type
     */
    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    /**
     * Start time for quiet hours (no notifications sent)
     */
    @Column(name = "quiet_start_time")
    private LocalTime quietStartTime;

    /**
     * End time for quiet hours (no notifications sent)
     */
    @Column(name = "quiet_end_time")
    private LocalTime quietEndTime;

    /**
     * Frequency setting for this notification type
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;

    /**
     * Checks if the current time is within the user's quiet hours.
     * 
     * @param currentTime the time to check
     * @return true if within quiet hours, false otherwise
     */
    public boolean isInQuietHours(LocalTime currentTime) {
        if (quietStartTime == null || quietEndTime == null || currentTime == null) {
            return false;
        }

        // Handle case where quiet hours cross midnight (e.g., 22:00 to 08:00)
        if (quietStartTime.isAfter(quietEndTime)) {
            return currentTime.isAfter(quietStartTime) || currentTime.isBefore(quietEndTime);
        } else {
            // Normal case (e.g., 08:00 to 17:00)
            return currentTime.isAfter(quietStartTime) && currentTime.isBefore(quietEndTime);
        }
    }

    /**
     * Checks if notifications should be sent for this preference.
     * 
     * @return true if notifications are enabled, false otherwise
     */
    public boolean isNotificationEnabled() {
        return frequency != NotificationFrequency.OFF && 
               (inAppEnabled || emailEnabled || pushEnabled);
    }

    /**
     * Checks if any delivery channel is enabled.
     * 
     * @return true if at least one channel is enabled, false otherwise
     */
    public boolean hasEnabledChannels() {
        return inAppEnabled || emailEnabled || pushEnabled;
    }
}