package com.focushive.analytics.entity;

import com.focushive.analytics.enums.AchievementType;
import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing user progress towards achievements.
 * Tracks completion progress and unlock timestamps for various achievements.
 */
@Entity
@Table(name = "achievement_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_type"}),
    indexes = {
        @Index(name = "idx_achievement_user_id", columnList = "user_id"),
        @Index(name = "idx_achievement_type", columnList = "achievement_type"),
        @Index(name = "idx_achievement_unlocked", columnList = "unlocked_at"),
        @Index(name = "idx_achievement_progress", columnList = "progress")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AchievementProgress extends BaseEntity {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Achievement type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false)
    private AchievementType achievementType;

    @NotNull(message = "Progress is required")
    @Min(value = 0, message = "Progress cannot be negative")
    @Max(value = 100, message = "Progress cannot exceed 100")
    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    @Min(value = 0, message = "Current value cannot be negative")
    @Column(name = "current_value")
    private Integer currentValue = 0;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "first_progress_at")
    private LocalDateTime firstProgressAt;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Check if achievement is unlocked
     */
    public boolean isUnlocked() {
        return unlockedAt != null;
    }

    /**
     * Check if achievement is in progress (started but not unlocked)
     */
    public boolean isInProgress() {
        return progress > 0 && unlockedAt == null;
    }

    /**
     * Update progress towards achievement
     */
    public void updateProgress(int newValue) {
        if (isUnlocked()) {
            return; // Cannot update progress for unlocked achievements
        }

        if (firstProgressAt == null) {
            firstProgressAt = LocalDateTime.now();
        }

        currentValue = newValue;
        int targetValue = achievementType.getTargetValue();
        progress = Math.min(100, (newValue * 100) / targetValue);

        // Check if achievement should be unlocked
        if (progress >= 100 && unlockedAt == null) {
            unlock();
        }
    }

    /**
     * Unlock the achievement
     */
    public void unlock() {
        if (unlockedAt == null) {
            unlockedAt = LocalDateTime.now();
            progress = 100;
            currentValue = achievementType.getTargetValue();
            notificationSent = false; // Reset to allow notification
        }
    }

    /**
     * Mark notification as sent
     */
    public void markNotificationSent() {
        notificationSent = true;
    }

    /**
     * Get achievement name
     */
    public String getAchievementName() {
        return achievementType.getName();
    }

    /**
     * Get achievement description
     */
    public String getAchievementDescription() {
        return achievementType.getDescription();
    }

    /**
     * Get achievement category
     */
    public String getCategory() {
        return achievementType.getCategory();
    }

    /**
     * Get points awarded for this achievement
     */
    public int getPoints() {
        return achievementType.getPoints();
    }

    /**
     * Get remaining value needed to unlock
     */
    public int getRemainingValue() {
        if (isUnlocked()) {
            return 0;
        }
        return Math.max(0, achievementType.getTargetValue() - currentValue);
    }

    /**
     * Get estimated days to unlock based on current progress rate
     */
    public Integer getEstimatedDaysToUnlock() {
        if (isUnlocked() || firstProgressAt == null || currentValue == 0) {
            return null;
        }

        long daysInProgress = java.time.temporal.ChronoUnit.DAYS.between(
            firstProgressAt.toLocalDate(), LocalDateTime.now().toLocalDate()) + 1;

        double progressRate = (double) currentValue / daysInProgress;
        if (progressRate <= 0) {
            return null;
        }

        int remaining = getRemainingValue();
        return (int) Math.ceil(remaining / progressRate);
    }
}