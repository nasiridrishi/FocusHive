package com.focushive.buddy.dto;

import com.focushive.buddy.constant.MilestoneStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for buddy goal milestone information.
 * Represents a specific milestone within a goal.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BuddyMilestoneDto {

    private UUID id;

    private UUID goalId;

    @NotBlank(message = "Milestone title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Target date is required")
    private LocalDateTime targetDate;

    @NotNull(message = "Status is required")
    @Builder.Default
    private MilestoneStatus status = MilestoneStatus.NOT_STARTED;

    @Min(value = 0, message = "Order index must be non-negative")
    private Integer orderIndex;

    private LocalDateTime completedDate;

    private String completedBy;

    @Builder.Default
    private Integer progressPercentage = 0;

    private String notes;

    private Boolean isKeyMilestone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Related goal information
    private String goalTitle;
    private UUID partnershipId;

    /**
     * Checks if the milestone is overdue.
     */
    public boolean isOverdue() {
        return targetDate != null
               && LocalDateTime.now().isAfter(targetDate)
               && status != MilestoneStatus.COMPLETED
               && status != MilestoneStatus.SKIPPED;
    }

    /**
     * Calculates days remaining until target date.
     */
    public long getDaysRemaining() {
        if (targetDate != null && !isCompleted()) {
            long days = java.time.Duration.between(LocalDateTime.now(), targetDate).toDays();
            return Math.max(0, days);
        }
        return 0;
    }

    /**
     * Checks if the milestone is completed.
     */
    public boolean isCompleted() {
        return status == MilestoneStatus.COMPLETED;
    }

    /**
     * Checks if the milestone is active.
     */
    public boolean isActive() {
        return status == MilestoneStatus.IN_PROGRESS;
    }

    /**
     * Updates the progress percentage based on status.
     */
    public void updateProgressFromStatus() {
        this.progressPercentage = status.getProgressPercentage();
    }
}