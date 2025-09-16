package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.focushive.analytics.enums.AchievementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user achievement progress.
 * Contains comprehensive achievement tracking and progress information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementProgressResponse {

    private String userId;

    // Summary statistics
    private Integer totalAchievements;
    private Integer unlockedCount;
    private Integer inProgressCount;
    private Integer totalPoints;
    private Double completionPercentage;

    // Achievement details
    private List<AchievementDetail> achievements;

    // Recent activity
    private List<AchievementDetail> recentUnlocks;
    private List<AchievementDetail> nearCompletion;

    // Category breakdown
    private Map<String, CategoryStats> categoryStats;

    // Milestone tracking
    private List<MilestoneProgress> milestones;

    // Leaderboard position
    private LeaderboardPosition leaderboard;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementDetail {
        private AchievementType type;
        private String name;
        private String description;
        private String category;
        private Integer progress;
        private Integer currentValue;
        private Integer targetValue;
        private Boolean unlocked;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime unlockedAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime firstProgressAt;

        private Integer points;
        private String difficulty; // "EASY", "MEDIUM", "HARD", "LEGENDARY"
        private String rarity; // "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"
        private Integer estimatedDaysToUnlock;
        private String progressStatus; // "NOT_STARTED", "IN_PROGRESS", "NEAR_COMPLETION", "UNLOCKED"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String categoryName;
        private Integer totalAchievements;
        private Integer unlockedCount;
        private Integer totalPoints;
        private Double completionRate;
        private List<String> nextToUnlock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneProgress {
        private String milestoneName;
        private String description;
        private Integer currentValue;
        private Integer targetValue;
        private Integer progress;
        private String category;
        private Boolean achieved;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime achievedAt;
        private Integer pointsAwarded;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardPosition {
        private Integer globalRank;
        private Integer totalUsers;
        private String percentile;
        private Integer pointsToNextRank;
        private List<TopAchiever> topAchievers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopAchiever {
        private String userId;
        private String username;
        private Integer totalPoints;
        private Integer achievementCount;
        private Integer rank;
    }
}