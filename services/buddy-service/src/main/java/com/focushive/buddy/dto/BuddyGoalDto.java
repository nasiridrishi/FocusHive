package com.focushive.buddy.dto;

import com.focushive.buddy.constant.GoalCategory;
import com.focushive.buddy.constant.GoalStatus;
import com.focushive.buddy.constant.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for buddy goal information.
 * Used for creating and updating goals in buddy partnerships.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BuddyGoalDto {

    private UUID id;

    @NotBlank(message = "User ID is required")
    private String userId;

    private UUID partnershipId;

    @NotBlank(message = "Goal title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Goal type is required")
    private GoalType type;

    @NotNull(message = "Goal category is required")
    private GoalCategory category;

    @NotNull(message = "Goal status is required")
    @Builder.Default
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @NotNull(message = "Target date is required")
    private LocalDateTime targetDate;

    private LocalDateTime startDate;

    private LocalDateTime completedDate;

    @Builder.Default
    private Boolean isPublic = false;

    @Builder.Default
    private Boolean isShared = false;

    private Integer progressPercentage;

    private List<String> tags;

    private List<MilestoneDto> milestones;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Accountability metrics
    private Integer checkinsCompleted;
    private Integer checkinsRequired;
    private Double complianceRate;

    // Partnership-specific fields
    private String buddyUserId;
    private String buddyName;
    private Boolean requiresBuddyApproval;

    /**
     * Validates that the goal dates are logical.
     */
    public boolean isValidDateRange() {
        if (startDate != null && targetDate != null) {
            return startDate.isBefore(targetDate);
        }
        return true;
    }

    /**
     * Calculates days remaining until target date.
     */
    public long getDaysRemaining() {
        if (targetDate != null) {
            return java.time.Duration.between(LocalDateTime.now(), targetDate).toDays();
        }
        return 0;
    }

    /**
     * Checks if the goal is overdue.
     */
    public boolean isOverdue() {
        return targetDate != null && LocalDateTime.now().isAfter(targetDate)
               && status != GoalStatus.COMPLETED;
    }
}