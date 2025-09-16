package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for achievements and celebrations.
 * Used for tracking and celebrating goal-related accomplishments.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDto {

    private UUID id;
    private UUID userId;
    private UUID goalId;
    private UUID milestoneId; // if achievement is milestone-related

    @NotBlank(message = "Achievement type is required")
    private String achievementType;

    @NotBlank(message = "Achievement title is required")
    @Size(min = 1, max = 200, message = "Achievement title must be between 1 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Achievement description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Achievement category is required")
    private AchievementCategory category;

    @NotNull(message = "Achievement level is required")
    private AchievementLevel level;

    /**
     * Points awarded for this achievement
     */
    @Min(value = 0, message = "Points must be non-negative")
    @Builder.Default
    private Integer points = 0;

    /**
     * Badge or icon identifier for this achievement
     */
    private String badgeIcon;

    /**
     * Achievement earned timestamp
     */
    @Builder.Default
    private LocalDateTime earnedAt = LocalDateTime.now();

    /**
     * Whether this achievement has been celebrated/acknowledged
     */
    @Builder.Default
    private Boolean celebrated = false;

    /**
     * Celebration timestamp
     */
    private LocalDateTime celebratedAt;

    /**
     * Achievement metadata and context
     */
    private Map<String, Object> metadata;

    /**
     * Achievement rarity/uniqueness
     */
    private AchievementRarity rarity;

    /**
     * Share settings for this achievement
     */
    private ShareSettings shareSettings;

    /**
     * User information for the achiever
     */
    private UserAchievementInfoDto userInfo;

    /**
     * Goal/milestone information related to this achievement
     */
    private RelatedEntityInfoDto relatedEntityInfo;

    /**
     * Achievement progress (for multi-step achievements)
     */
    private ProgressInfoDto progressInfo;

    /**
     * Enum for achievement categories
     */
    public enum AchievementCategory {
        GOAL_COMPLETION("Goal Completion"),
        MILESTONE_ACHIEVEMENT("Milestone Achievement"),
        STREAK("Streak Achievement"),
        COLLABORATION("Collaboration Achievement"),
        IMPROVEMENT("Improvement Achievement"),
        CONSISTENCY("Consistency Achievement"),
        INNOVATION("Innovation Achievement"),
        LEADERSHIP("Leadership Achievement"),
        COMMUNITY("Community Achievement"),
        SPECIAL("Special Achievement");

        private final String displayName;

        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enum for achievement levels
     */
    public enum AchievementLevel {
        BRONZE("Bronze", 1, 10),
        SILVER("Silver", 2, 25),
        GOLD("Gold", 3, 50),
        PLATINUM("Platinum", 4, 100),
        DIAMOND("Diamond", 5, 200);

        private final String displayName;
        private final int tier;
        private final int basePoints;

        AchievementLevel(String displayName, int tier, int basePoints) {
            this.displayName = displayName;
            this.tier = tier;
            this.basePoints = basePoints;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getTier() {
            return tier;
        }

        public int getBasePoints() {
            return basePoints;
        }
    }

    /**
     * Enum for achievement rarity
     */
    public enum AchievementRarity {
        COMMON("Common", 1.0),
        UNCOMMON("Uncommon", 1.5),
        RARE("Rare", 2.0),
        EPIC("Epic", 3.0),
        LEGENDARY("Legendary", 5.0);

        private final String displayName;
        private final double multiplier;

        AchievementRarity(String displayName, double multiplier) {
            this.displayName = displayName;
            this.multiplier = multiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }

    /**
     * DTO for share settings
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareSettings {
        @Builder.Default
        private Boolean shareWithPartner = true;

        @Builder.Default
        private Boolean shareWithCommunity = false;

        @Builder.Default
        private Boolean shareOnPublicProfile = false;

        @Builder.Default
        private Boolean allowComments = true;

        private String customMessage;
    }

    /**
     * DTO for user achievement information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAchievementInfoDto {
        private UUID userId;
        private String username;
        private String displayName;
        private String email;
        private Integer totalAchievements;
        private Integer totalPoints;
        private String currentRank;
        private Integer achievementStreak;
    }

    /**
     * DTO for related entity information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedEntityInfoDto {
        private UUID entityId;
        private String entityType; // GOAL, MILESTONE, PARTNERSHIP
        private String entityTitle;
        private String entityDescription;
        private LocalDateTime entityCreatedAt;
        private LocalDateTime entityCompletedAt;
        private Map<String, Object> entityMetadata;
    }

    /**
     * DTO for achievement progress information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressInfoDto {
        private Integer currentProgress;
        private Integer targetProgress;
        private Double progressPercentage;
        private String progressDescription;
        private LocalDateTime lastProgressUpdate;
        private Boolean isMultiStep;
        private Integer currentStep;
        private Integer totalSteps;
    }

    /**
     * Helper method to calculate total points with rarity multiplier
     */
    public Integer calculateTotalPoints() {
        if (points == null || rarity == null) {
            return points != null ? points : 0;
        }
        return (int) Math.round(points * rarity.getMultiplier());
    }

    /**
     * Helper method to get display description with context
     */
    public String getDisplayDescription() {
        if (description != null && !description.trim().isEmpty()) {
            return description;
        }

        // Generate default description based on type and category
        return String.format("Achieved %s level %s for %s",
            level.getDisplayName(),
            category.getDisplayName(),
            relatedEntityInfo != null ? relatedEntityInfo.getEntityTitle() : "goal activity");
    }

    /**
     * Helper method to check if achievement is recent (within last 24 hours)
     */
    public Boolean isRecent() {
        if (earnedAt == null) {
            return false;
        }
        return earnedAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Helper method to check if achievement needs celebration
     */
    public Boolean needsCelebration() {
        return !Boolean.TRUE.equals(celebrated) &&
               level.getTier() >= AchievementLevel.SILVER.getTier();
    }
}