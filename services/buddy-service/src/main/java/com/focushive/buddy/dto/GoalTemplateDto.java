package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for goal templates.
 * Used for creating, updating, and responding with goal template data.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoalTemplateDto {

    private UUID id;
    private Long version;

    @NotBlank(message = "Template name is required")
    @Size(min = 1, max = 200, message = "Template name must be between 1 and 200 characters")
    private String name;

    @NotBlank(message = "Template title is required")
    @Size(min = 1, max = 200, message = "Template title must be between 1 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Template description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Template category is required")
    @Size(min = 1, max = 100, message = "Category must be between 1 and 100 characters")
    private String category;

    /**
     * List of predefined tags for goals created from this template
     */
    private List<String> tags;

    /**
     * Default duration in days for goals created from this template
     */
    @Min(value = 1, message = "Default duration must be at least 1 day")
    @Max(value = 365, message = "Default duration must not exceed 365 days")
    private Integer defaultDurationDays;

    /**
     * Difficulty level (1=easy, 5=very hard)
     */
    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    @Builder.Default
    private Integer difficulty = 3;

    /**
     * Estimated completion time in hours
     */
    @Min(value = 1, message = "Estimated time must be at least 1 hour")
    private Integer estimatedTimeHours;

    /**
     * Template type
     */
    @NotNull(message = "Template type is required")
    private TemplateType templateType;

    /**
     * Whether this template is active and available for use
     */
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Whether this template is featured/promoted
     */
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * Creator of the template
     */
    private UUID createdBy;

    /**
     * Template creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last modification timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Predefined milestones for this template
     */
    private List<TemplateMilestoneDto> milestones;

    /**
     * Template usage statistics
     */
    private TemplateStatisticsDto statistics;

    /**
     * Template rating and feedback
     */
    private TemplateRatingDto rating;

    /**
     * Template metadata for customization
     */
    private TemplateMetadataDto metadata;

    /**
     * Whether the current user can edit this template
     */
    private Boolean canEdit;

    /**
     * Whether the current user can delete this template
     */
    private Boolean canDelete;

    /**
     * Enum for template types
     */
    public enum TemplateType {
        INDIVIDUAL("Individual goal template"),
        SHARED("Shared goal template"),
        BEGINNER("Beginner-friendly template"),
        ADVANCED("Advanced template"),
        QUICK_WIN("Quick win template"),
        LONG_TERM("Long-term goal template");

        private final String displayName;

        TemplateType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * DTO for template milestones
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateMilestoneDto {

        @NotBlank(message = "Milestone title is required")
        @Size(min = 1, max = 200, message = "Milestone title must be between 1 and 200 characters")
        private String title;

        @Size(max = 1000, message = "Milestone description must not exceed 1000 characters")
        private String description;

        /**
         * Order of this milestone in the template
         */
        @Min(value = 1, message = "Order must be at least 1")
        private Integer order;

        /**
         * Suggested days from goal start to complete this milestone
         */
        @Min(value = 0, message = "Suggested days offset must be non-negative")
        private Integer suggestedDaysOffset;

        /**
         * Whether this milestone is required or optional
         */
        @Builder.Default
        private Boolean isRequired = true;

        /**
         * Category for this milestone
         */
        private String category;

        /**
         * Estimated effort in hours
         */
        @Min(value = 0, message = "Estimated effort must be non-negative")
        private Integer estimatedEffortHours;

        /**
         * Whether this milestone has dependencies on other milestones
         */
        @Builder.Default
        private Boolean hasDependencies = false;
    }

    /**
     * DTO for template statistics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateStatisticsDto {
        private Integer usageCount;
        private Integer completionCount;
        private Double successRate; // percentage of goals completed
        private Double averageCompletionTime; // days
        private Integer totalUsersUsed;
        private LocalDateTime lastUsed;
        private Double averageRating;
        private Integer totalRatings;
    }

    /**
     * DTO for template rating
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateRatingDto {
        private Double averageRating; // 1-5 stars
        private Integer totalRatings;
        private Integer fiveStarCount;
        private Integer fourStarCount;
        private Integer threeStarCount;
        private Integer twoStarCount;
        private Integer oneStarCount;
        private List<String> recentFeedback;
        private String overallSentiment; // POSITIVE, NEUTRAL, NEGATIVE
    }

    /**
     * DTO for template metadata
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateMetadataDto {
        private String author;
        private String version;
        private List<String> applicableContexts; // WORK, STUDY, PERSONAL, etc.
        private String targetAudience; // BEGINNER, INTERMEDIATE, ADVANCED
        private List<String> prerequisites;
        private List<String> recommendedResources;
        private String templateInstructions;
        private Map<String, Object> customFields;
    }
}