package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entity representing daily productivity metrics for a user.
 * Tracks focus minutes, completed sessions, and calculated productivity scores.
 */
@Entity
@Table(name = "productivity_metrics",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}),
    indexes = {
        @Index(name = "idx_productivity_user_date", columnList = "user_id, date"),
        @Index(name = "idx_productivity_date", columnList = "date"),
        @Index(name = "idx_productivity_score", columnList = "productivity_score")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductivityMetric extends BaseEntity {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Focus minutes is required")
    @Min(value = 0, message = "Focus minutes cannot be negative")
    @Column(name = "focus_minutes", nullable = false)
    private Integer focusMinutes = 0;

    @NotNull(message = "Completed sessions is required")
    @Min(value = 0, message = "Completed sessions cannot be negative")
    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions = 0;

    @NotNull(message = "Productivity score is required")
    @Min(value = 0, message = "Productivity score cannot be negative")
    @Max(value = 1000, message = "Productivity score cannot exceed 1000")
    @Column(name = "productivity_score", nullable = false)
    private Integer productivityScore = 0;

    @Min(value = 0, message = "Total sessions cannot be negative")
    @Column(name = "total_sessions")
    private Integer totalSessions = 0;

    @Min(value = 0, message = "Break minutes cannot be negative")
    @Column(name = "break_minutes")
    private Integer breakMinutes = 0;

    @Min(value = 0, message = "Distractions cannot be negative")
    @Column(name = "distractions_count")
    private Integer distractionsCount = 0;

    @Min(value = 0, message = "Average session length cannot be negative")
    @Column(name = "average_session_length")
    private Integer averageSessionLength = 0;

    @Min(value = 0, message = "Peak performance hour must be between 0 and 23")
    @Max(value = 23, message = "Peak performance hour must be between 0 and 23")
    @Column(name = "peak_performance_hour")
    private Integer peakPerformanceHour;

    @Min(value = 0, message = "Goals achieved cannot be negative")
    @Column(name = "goals_achieved")
    private Integer goalsAchieved = 0;

    @Min(value = 0, message = "Streak bonus cannot be negative")
    @Column(name = "streak_bonus")
    private Integer streakBonus = 0;

    /**
     * Calculate completion rate as percentage
     */
    public double getCompletionRate() {
        if (totalSessions == null || totalSessions == 0) {
            return 0.0;
        }
        return (double) completedSessions / totalSessions * 100.0;
    }

    /**
     * Update metrics from a completed focus session
     */
    public void addSessionData(int sessionMinutes, boolean completed, int distractions) {
        if (totalSessions == null) totalSessions = 0;
        if (completedSessions == null) completedSessions = 0;
        if (focusMinutes == null) focusMinutes = 0;
        if (distractionsCount == null) distractionsCount = 0;

        totalSessions++;
        if (completed) {
            completedSessions++;
            focusMinutes += sessionMinutes;
        }
        distractionsCount += distractions;

        // Recalculate average session length
        if (completedSessions > 0) {
            averageSessionLength = focusMinutes / completedSessions;
        }
    }

    /**
     * Calculate productivity score based on various factors
     */
    public void calculateProductivityScore() {
        int baseScore = focusMinutes * 2; // 2 points per focus minute
        int sessionBonus = completedSessions * 10; // 10 points per completed session
        int distractionPenalty = distractionsCount * 5; // -5 points per distraction
        int goalBonus = goalsAchieved * 20; // 20 points per goal achieved
        int streakBonusPoints = streakBonus != null ? streakBonus : 0;

        productivityScore = Math.max(0, baseScore + sessionBonus - distractionPenalty + goalBonus + streakBonusPoints);
    }
}