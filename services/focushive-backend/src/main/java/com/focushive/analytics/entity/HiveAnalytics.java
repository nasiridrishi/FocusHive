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
 * Entity representing daily analytics aggregated at the hive level.
 * Tracks collective productivity metrics for all members of a hive.
 */
@Entity
@Table(name = "hive_analytics",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hive_id", "date"}),
    indexes = {
        @Index(name = "idx_hive_analytics_hive_date", columnList = "hive_id, date"),
        @Index(name = "idx_hive_analytics_date", columnList = "date"),
        @Index(name = "idx_hive_analytics_active_users", columnList = "active_users")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HiveAnalytics extends BaseEntity {

    @NotNull(message = "Hive ID is required")
    @Column(name = "hive_id", nullable = false)
    private String hiveId;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Active users count is required")
    @Min(value = 0, message = "Active users cannot be negative")
    @Column(name = "active_users", nullable = false)
    private Integer activeUsers = 0;

    @NotNull(message = "Total focus time is required")
    @Min(value = 0, message = "Total focus time cannot be negative")
    @Column(name = "total_focus_time", nullable = false)
    private Integer totalFocusTime = 0;

    @NotNull(message = "Total sessions is required")
    @Min(value = 0, message = "Total sessions cannot be negative")
    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions = 0;

    @NotNull(message = "Completed sessions is required")
    @Min(value = 0, message = "Completed sessions cannot be negative")
    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions = 0;

    @Min(value = 0, message = "Average productivity score cannot be negative")
    @Max(value = 1000, message = "Average productivity score cannot exceed 1000")
    @Column(name = "average_productivity_score")
    private Integer averageProductivityScore = 0;

    @Min(value = 0, message = "Peak concurrent users cannot be negative")
    @Column(name = "peak_concurrent_users")
    private Integer peakConcurrentUsers = 0;

    @Min(value = 0, message = "Total break time cannot be negative")
    @Column(name = "total_break_time")
    private Integer totalBreakTime = 0;

    @Min(value = 0, message = "Total distractions cannot be negative")
    @Column(name = "total_distractions")
    private Integer totalDistractions = 0;

    @Min(value = 0, message = "Goals achieved cannot be negative")
    @Column(name = "total_goals_achieved")
    private Integer totalGoalsAchieved = 0;

    @Min(value = 0, message = "Most productive hour must be between 0 and 23")
    @Max(value = 23, message = "Most productive hour must be between 0 and 23")
    @Column(name = "most_productive_hour")
    private Integer mostProductiveHour;

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
     * Calculate average session length in minutes
     */
    public double getAverageSessionLength() {
        if (completedSessions == null || completedSessions == 0) {
            return 0.0;
        }
        return (double) totalFocusTime / completedSessions;
    }

    /**
     * Update analytics with new session data
     */
    public void addSessionData(int sessionMinutes, boolean completed, int distractions, int productivityScore) {
        if (totalSessions == null) totalSessions = 0;
        if (completedSessions == null) completedSessions = 0;
        if (totalFocusTime == null) totalFocusTime = 0;
        if (totalDistractions == null) totalDistractions = 0;
        if (averageProductivityScore == null) averageProductivityScore = 0;

        totalSessions++;
        if (completed) {
            completedSessions++;
            totalFocusTime += sessionMinutes;
        }
        totalDistractions += distractions;

        // Update running average of productivity score
        if (totalSessions > 0) {
            averageProductivityScore = (averageProductivityScore * (totalSessions - 1) + productivityScore) / totalSessions;
        }
    }

    /**
     * Add a new active user for the day (if not already counted)
     */
    public void incrementActiveUsers() {
        if (activeUsers == null) activeUsers = 0;
        activeUsers++;
    }

    /**
     * Update peak concurrent users if current count is higher
     */
    public void updatePeakConcurrentUsers(int currentConcurrent) {
        if (peakConcurrentUsers == null || currentConcurrent > peakConcurrentUsers) {
            peakConcurrentUsers = currentConcurrent;
        }
    }
}