package com.focushive.buddy.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for updating goal progress.
 * Contains progress information and optional notes.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdateDto {

    @NotNull(message = "Goal ID is required")
    private UUID goalId;

    @NotNull(message = "Progress percentage is required")
    @Min(value = 0, message = "Progress percentage must be between 0 and 100")
    @Max(value = 100, message = "Progress percentage must be between 0 and 100")
    private Integer progressPercentage;

    @NotNull(message = "Updated by user ID is required")
    private UUID updatedBy;

    /**
     * Optional notes about the progress update
     */
    @Size(max = 1000, message = "Progress notes must not exceed 1000 characters")
    private String progressNotes;

    /**
     * Type of progress update
     */
    @NotNull(message = "Update type is required")
    private ProgressUpdateType updateType;

    /**
     * Optional milestone ID if this update is related to a specific milestone
     */
    private UUID relatedMilestoneId;

    /**
     * Timestamp of the progress update (defaults to current time)
     */
    @Builder.Default
    private LocalDateTime updateTimestamp = LocalDateTime.now();

    /**
     * Whether this update should trigger notifications
     */
    @Builder.Default
    private Boolean triggerNotifications = true;

    /**
     * Confidence level in the progress update (1=low, 5=high)
     */
    @Min(value = 1, message = "Confidence level must be between 1 and 5")
    @Max(value = 5, message = "Confidence level must be between 1 and 5")
    @Builder.Default
    private Integer confidenceLevel = 5;

    /**
     * Estimated effort spent since last update (in hours)
     */
    @Min(value = 0, message = "Effort hours must be non-negative")
    private Double effortHours;

    /**
     * Mood/feeling about the progress (optional for emotional tracking)
     */
    private String mood;

    /**
     * Challenges faced during this progress period
     */
    @Size(max = 500, message = "Challenges must not exceed 500 characters")
    private String challengesFaced;

    /**
     * Successes achieved during this progress period
     */
    @Size(max = 500, message = "Successes must not exceed 500 characters")
    private String successesAchieved;

    /**
     * Next steps planned
     */
    @Size(max = 500, message = "Next steps must not exceed 500 characters")
    private String nextSteps;

    /**
     * Whether this update marks goal completion
     */
    @Builder.Default
    private Boolean marksCompletion = false;

    /**
     * Enum for progress update types
     */
    public enum ProgressUpdateType {
        MANUAL("Manual update"),
        MILESTONE_COMPLETION("Milestone completion"),
        AUTOMATIC("Automatic calculation"),
        BULK_UPDATE("Bulk progress update"),
        CORRECTION("Progress correction"),
        ROLLBACK("Progress rollback");

        private final String displayName;

        ProgressUpdateType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}