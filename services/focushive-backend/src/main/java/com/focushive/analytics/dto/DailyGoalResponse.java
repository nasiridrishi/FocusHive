package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for daily goal information.
 * Contains goal details, progress, and related analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyGoalResponse {

    private String userId;

    // Goal details
    private String goalId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private Integer targetMinutes;
    private Integer completedMinutes;
    private Boolean achieved;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime achievedAt;

    private String description;
    private String priority;

    // Progress information
    private Double completionPercentage;
    private Integer remainingMinutes;
    private Boolean overachieved;
    private String status; // "PENDING", "IN_PROGRESS", "ACHIEVED", "OVERACHIEVED", "OVERDUE"
    private String progressColor;

    // Streak and achievement data
    private Boolean contributesToStreak;
    private Integer currentGoalStreak;
    private List<String> newAchievements;

    // Time tracking
    private List<GoalProgressUpdate> progressUpdates;
    private EstimatedCompletion estimatedCompletion;

    // Insights and motivation
    private String motivationalMessage;
    private List<String> tips;
    private GoalComparison comparison;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProgressUpdate {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

        private Integer minutesAdded;
        private Integer totalCompleted;
        private Double progressPercentage;
        private String source; // "FOCUS_SESSION", "MANUAL_UPDATE", "BREAK_SESSION"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstimatedCompletion {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime estimatedTime;

        private Integer minutesNeeded;
        private Double averageSessionLength;
        private Integer sessionsNeeded;
        private String confidence; // "HIGH", "MEDIUM", "LOW"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalComparison {
        private Integer averageTargetForUser;
        private Integer averageCompletedForUser;
        private Double userAchievementRate;
        private Integer platformAverageTarget;
        private Double platformAchievementRate;
        private String performanceVsAverage; // "ABOVE", "AVERAGE", "BELOW"
    }
}