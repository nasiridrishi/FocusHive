package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for user streak information.
 * Contains current streak status, history, and related analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakInfoResponse {

    private String userId;

    // Current streak information
    private Integer currentStreak;
    private Integer longestStreak;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastActiveDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate streakStartDate;

    // Streak status
    private Boolean streakAtRisk;
    private String streakStatus; // "ACTIVE", "AT_RISK", "BROKEN", "NEW"
    private Integer daysToNextMilestone;
    private Integer nextMilestone;

    // Streak management
    private Integer availableFreeze;
    private Integer freezesUsed;
    private Boolean canUseFreeze;

    // Historical data
    private Integer totalActiveDays;
    private Double monthlyStreakPercentage;
    private List<StreakPeriod> streakHistory;

    // Milestones and achievements
    private List<StreakMilestone> milestones;
    private List<String> recentAchievements;

    // Comparative data
    private StreakComparison comparison;

    // Motivation and insights
    private List<String> streakTips;
    private String encouragementMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakPeriod {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private Integer length;
        private String status; // "COMPLETED", "BROKEN", "CURRENT"
        private Boolean wasBest;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakMilestone {
        private Integer days;
        private String name;
        private String description;
        private Boolean achieved;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate achievedDate;
        private Boolean isNext;
        private Integer daysToReach;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakComparison {
        private Double averageStreakLength;
        private Integer platformBestStreak;
        private String percentileRank;
        private Integer usersWithSimilarStreak;
        private String comparisonMessage;
    }
}