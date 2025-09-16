package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing daily goals set by users.
 * Tracks target focus minutes and progress towards achieving them.
 */
@Entity
@Table(name = "daily_goals",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}),
    indexes = {
        @Index(name = "idx_daily_goals_user_date", columnList = "user_id, date"),
        @Index(name = "idx_daily_goals_date", columnList = "date"),
        @Index(name = "idx_daily_goals_achieved", columnList = "achieved")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DailyGoal extends BaseEntity {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Target minutes is required")
    @Min(value = 1, message = "Target minutes must be at least 1")
    @Max(value = 1440, message = "Target minutes cannot exceed 24 hours")
    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes;

    @NotNull(message = "Completed minutes is required")
    @Min(value = 0, message = "Completed minutes cannot be negative")
    @Column(name = "completed_minutes", nullable = false)
    private Integer completedMinutes = 0;

    @Column(name = "achieved")
    private Boolean achieved = false;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @Size(max = 200, message = "Goal description cannot exceed 200 characters")
    @Column(name = "description", length = 200)
    private String description;

    @Size(max = 50, message = "Priority cannot exceed 50 characters")
    @Column(name = "priority", length = 50)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "streak_contribution")
    private Boolean streakContribution = false;

    /**
     * Get completion percentage
     */
    public double getCompletionPercentage() {
        if (targetMinutes == null || targetMinutes == 0) {
            return 0.0;
        }
        return Math.min(100.0, (double) completedMinutes / targetMinutes * 100.0);
    }

    /**
     * Get remaining minutes to achieve goal
     */
    public int getRemainingMinutes() {
        return Math.max(0, targetMinutes - completedMinutes);
    }

    /**
     * Check if goal is overachieved
     */
    public boolean isOverachieved() {
        return completedMinutes > targetMinutes;
    }

    /**
     * Update progress towards goal
     */
    public void updateProgress(int additionalMinutes) {
        if (additionalMinutes < 0) {
            throw new IllegalArgumentException("Additional minutes cannot be negative");
        }

        completedMinutes += additionalMinutes;

        // Check if goal is achieved
        if (!achieved && completedMinutes >= targetMinutes) {
            achieved = true;
            achievedAt = LocalDateTime.now();
            streakContribution = true;
        }
    }

    /**
     * Set goal as achieved manually
     */
    public void markAsAchieved() {
        if (!achieved) {
            achieved = true;
            achievedAt = LocalDateTime.now();
            streakContribution = true;
        }
    }

    /**
     * Check if goal is for today
     */
    public boolean isForToday() {
        return date.equals(LocalDate.now());
    }

    /**
     * Check if goal is overdue (past date and not achieved)
     */
    public boolean isOverdue() {
        return date.isBefore(LocalDate.now()) && !achieved;
    }

    /**
     * Get goal status as string
     */
    public String getStatus() {
        if (achieved) {
            return isOverachieved() ? "OVERACHIEVED" : "ACHIEVED";
        } else if (isOverdue()) {
            return "OVERDUE";
        } else if (isForToday()) {
            return "IN_PROGRESS";
        } else {
            return "PENDING";
        }
    }

    /**
     * Get appropriate color for UI display based on progress
     */
    public String getProgressColor() {
        double percentage = getCompletionPercentage();
        if (achieved) {
            return "#4CAF50"; // Green
        } else if (percentage >= 75) {
            return "#FF9800"; // Orange
        } else if (percentage >= 50) {
            return "#FFC107"; // Amber
        } else if (percentage >= 25) {
            return "#FF5722"; // Deep Orange
        } else {
            return "#F44336"; // Red
        }
    }

    /**
     * Mark reminder as sent
     */
    public void markReminderSent() {
        reminderSent = true;
    }

    /**
     * Reset daily for new day (completed minutes, achieved status, etc.)
     */
    public void resetForNewDay(LocalDate newDate) {
        this.date = newDate;
        this.completedMinutes = 0;
        this.achieved = false;
        this.achievedAt = null;
        this.reminderSent = false;
        this.streakContribution = false;
    }
}