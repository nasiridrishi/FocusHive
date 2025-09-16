package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for milestone operations.
 * Used for creating, updating, and responding with milestone data.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MilestoneDto {

    private UUID id;
    private Long version;
    private UUID goalId;

    @NotBlank(message = "Milestone title is required")
    @Size(min = 1, max = 200, message = "Milestone title must be between 1 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Milestone description must not exceed 1000 characters")
    private String description;

    private LocalDate targetDate;
    private LocalDateTime completedAt;
    private UUID completedBy;
    private Boolean celebrationSent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Order of this milestone in the goal (used for sequencing)
     */
    @Min(value = 1, message = "Order must be at least 1")
    private Integer order;

    /**
     * Whether this milestone depends on previous milestones
     */
    @Builder.Default
    private Boolean hasDependencies = false;

    /**
     * List of milestone IDs that this milestone depends on
     */
    private java.util.List<UUID> dependsOnMilestones;

    /**
     * Priority level (1=low, 5=high)
     */
    @Min(value = 1, message = "Priority must be between 1 and 5")
    @Max(value = 5, message = "Priority must be between 1 and 5")
    @Builder.Default
    private Integer priority = 3;

    /**
     * Category for milestone organization
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Estimated effort in hours
     */
    @Min(value = 0, message = "Estimated effort must be non-negative")
    private Integer estimatedEffortHours;

    /**
     * Actual effort in hours (after completion)
     */
    @Min(value = 0, message = "Actual effort must be non-negative")
    private Integer actualEffortHours;

    /**
     * Completion notes from the user who completed it
     */
    @Size(max = 1000, message = "Completion notes must not exceed 1000 characters")
    private String completionNotes;

    /**
     * Computed properties
     */
    private Boolean isCompleted;
    private Boolean isOverdue;
    private Integer daysUntilTarget;
    private Integer daysSinceCreation;
    private Integer daysSinceCompletion;

    /**
     * User information for who completed the milestone
     */
    private UserInfoDto completedByUser;

    /**
     * Goal information this milestone belongs to
     */
    private GoalInfoDto goalInfo;

    /**
     * Whether the current user can edit this milestone
     */
    private Boolean canEdit;

    /**
     * Whether the current user can complete this milestone
     */
    private Boolean canComplete;

    /**
     * Whether the current user can delete this milestone
     */
    private Boolean canDelete;

    /**
     * DTO for user information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private UUID userId;
        private String username;
        private String displayName;
        private String email;
    }

    /**
     * DTO for goal information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalInfoDto {
        private UUID goalId;
        private String goalTitle;
        private String goalDescription;
        private Integer goalProgress;
        private LocalDate goalTargetDate;
    }
}