package com.focushive.buddy.dto;

import com.focushive.buddy.validation.ValidSharedGoal;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating new goals (individual or shared).
 * Contains all necessary information to create a goal with optional milestones.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ValidSharedGoal
public class GoalCreationDto {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private LocalDate targetDate;

    // Partnership ID is only required for shared goals, optional for individual goals
    private UUID partnershipId;

    @NotNull(message = "Created by user ID is required")
    private UUID createdBy;

    /**
     * Initial progress percentage (default 0)
     */
    @Min(value = 0, message = "Initial progress must be at least 0")
    @Max(value = 100, message = "Initial progress must not exceed 100")
    @Builder.Default
    private Integer initialProgress = 0;

    /**
     * Goal type: INDIVIDUAL or SHARED
     */
    @NotNull(message = "Goal type is required")
    private GoalType goalType;

    /**
     * Optional template ID if using a predefined template
     */
    private UUID templateId;

    /**
     * List of initial milestones to create with the goal
     */
    private List<MilestoneCreationDto> initialMilestones;

    /**
     * Priority level (1=low, 5=high)
     */
    @Min(value = 1, message = "Priority must be between 1 and 5")
    @Max(value = 5, message = "Priority must be between 1 and 5")
    @Builder.Default
    private Integer priority = 3;

    /**
     * Category for goal organization
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * Tags for goal organization and searching
     */
    private List<String> tags;

    /**
     * Whether this goal should send progress reminders
     */
    @Builder.Default
    private Boolean enableReminders = true;

    /**
     * Goal difficulty level (1=easy, 5=very hard)
     */
    @Min(value = 1, message = "Difficulty must be between 1 and 5")
    @Max(value = 5, message = "Difficulty must be between 1 and 5")
    @Builder.Default
    private Integer difficulty = 3;

    /**
     * Enum for goal types
     */
    public enum GoalType {
        INDIVIDUAL("Individual goal"),
        SHARED("Shared goal with buddy");

        private final String displayName;

        GoalType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * DTO for creating milestones within the goal creation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneCreationDto {

        @NotBlank(message = "Milestone title is required")
        @Size(min = 1, max = 200, message = "Milestone title must be between 1 and 200 characters")
        private String title;

        @Size(max = 1000, message = "Milestone description must not exceed 1000 characters")
        private String description;

        private LocalDate targetDate;

        /**
         * Order of this milestone (used for sequencing)
         */
        @Min(value = 1, message = "Order must be at least 1")
        private Integer order;

        /**
         * Whether this milestone depends on previous milestones
         */
        @Builder.Default
        private Boolean hasDependencies = false;
    }
}