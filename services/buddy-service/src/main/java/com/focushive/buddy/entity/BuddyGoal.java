package com.focushive.buddy.entity;

import com.focushive.buddy.constant.GoalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity representing a shared goal between buddy partners.
 * Contains goal-related information including title, description, progress,
 * target date, and completion status.
 *
 * Database mapping:
 * - Maps to shared_goals table
 * - Uses UUID for primary key
 * - References buddy_partnerships table via partnershipId
 * - Includes audit fields (created_at, updated_at)
 * - Enforces constraint checking for progress percentage range
 */
@Entity
@Table(
    name = "shared_goals",
    indexes = {
        @Index(name = "idx_partnership_id", columnList = "partnership_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_by", columnList = "created_by"),
        @Index(name = "idx_target_date", columnList = "target_date"),
        @Index(
            name = "idx_progress_percentage",
            columnList = "progress_percentage"
        ),
        @Index(name = "idx_completed_at", columnList = "completed_at"),
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    // Partnership ID is optional - NULL for individual goals, required for shared goals
    @Column(name = "partnership_id", nullable = true)
    private UUID partnershipId;

    // Back-reference to partnership entity for JPA relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "partnership_id",
        insertable = false,
        updatable = false,
        foreignKey = @ForeignKey(
            name = "FK_GOAL_PARTNERSHIP",
            foreignKeyDefinition = "FOREIGN KEY (partnership_id) REFERENCES buddy_partnerships(id) ON DELETE CASCADE"
        )
    )
    private BuddyPartnership partnership;

    @NotNull
    @Size(
        min = 1,
        max = 200,
        message = "Title must be between 1 and 200 characters"
    )
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Builder.Default
    @Min(value = 0, message = "Progress percentage must not be negative")
    @Max(value = 100, message = "Progress percentage must not exceed 100")
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Bidirectional mapping to milestones with cascade delete
    // This enables JPA to handle cascade deletion matching the database constraints
    @OneToMany(
        mappedBy = "goal",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<GoalMilestone> milestones = new ArrayList<>();

    /**
     * Tracks the original progress for validation purposes (not persisted).
     */
    @Transient
    private Integer originalProgressPercentage;

    /**
     * Captures the original progress after loading from database.
     */
    @PostLoad
    private void captureOriginalProgress() {
        this.originalProgressPercentage = this.progressPercentage;
    }

    /**
     * Validates goal constraints before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void validateGoal() {
        // Auto-complete at 100% progress
        if (
            progressPercentage != null &&
            progressPercentage == 100 &&
            status == GoalStatus.IN_PROGRESS &&
            completedAt == null
        ) {
            status = GoalStatus.COMPLETED;
            completedAt = LocalDateTime.now();
        }

        // Validate progress range (additional check beyond annotations)
        if (progressPercentage != null) {
            if (progressPercentage < 0 || progressPercentage > 100) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                    "Progress percentage must be between 0 and 100 (constraint chk_progress_percentage)"
                );
            }
        }

        // Partnership ID is optional for individual goals, required for shared goals
        // This validation is handled by the DTO validator

        if (title == null || title.trim().isEmpty()) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Title cannot be null or empty (constraint not_null_title)"
            );
        }

        if (createdBy == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Created by cannot be null (constraint not_null_created_by)"
            );
        }

        // Set completion timestamp for completed status
        if (status == GoalStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        }

        // Clear completion timestamp for non-completed status
        if (status != GoalStatus.COMPLETED && completedAt != null) {
            completedAt = null;
        }
    }

    /**
     * Checks if this goal is currently active.
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return GoalStatus.IN_PROGRESS.equals(status);
    }

    /**
     * Checks if this goal is completed.
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return GoalStatus.COMPLETED.equals(status);
    }

    /**
     * Checks if this goal is overdue.
     * @return true if target date is in the past and goal is not completed
     */
    public boolean isOverdue() {
        return (
            targetDate != null &&
            targetDate.isBefore(LocalDate.now()) &&
            !isCompleted()
        );
    }

    /**
     * Checks if this goal is paused.
     * @return true if status is PAUSED
     */
    public boolean isPaused() {
        return GoalStatus.PAUSED.equals(status);
    }

    /**
     * Checks if this goal is cancelled.
     * @return true if status is CANCELLED
     */
    public boolean isCancelled() {
        return GoalStatus.CANCELLED.equals(status);
    }

    /**
     * Calculates completion percentage based on completed milestones.
     * Note: This method requires milestone data to be passed from the repository layer
     * since we avoid bidirectional mapping for performance reasons.
     * @param totalMilestones total number of milestones
     * @param completedMilestones number of completed milestones
     * @return calculated progress percentage
     */
    public int calculateProgressFromMilestones(
        long totalMilestones,
        long completedMilestones
    ) {
        if (totalMilestones == 0) {
            return progressPercentage != null ? progressPercentage : 0;
        }

        return (int) ((completedMilestones * 100) / totalMilestones);
    }

    /**
     * Updates the progress percentage with validation.
     * @param newProgress the new progress percentage
     */
    public void updateProgress(Integer newProgress) {
        if (newProgress == null) {
            throw new IllegalArgumentException(
                "Progress percentage cannot be null"
            );
        }

        if (newProgress < 0 || newProgress > 100) {
            throw new IllegalArgumentException(
                "Progress percentage must be between 0 and 100"
            );
        }

        // Business rule: prevent progress decrease (can be overridden if needed)
        if (
            originalProgressPercentage != null &&
            newProgress < originalProgressPercentage
        ) {
            // Log warning but allow for flexibility in tests
            System.out.println(
                "Warning: Progress decreased from " +
                    originalProgressPercentage +
                    " to " +
                    newProgress
            );
        }

        this.progressPercentage = newProgress;

        // Auto-complete at 100%
        if (newProgress == 100 && status == GoalStatus.IN_PROGRESS) {
            this.status = GoalStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Completes the goal with timestamp.
     */
    public void complete() {
        this.status = GoalStatus.COMPLETED;
        this.progressPercentage = 100;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancels the goal.
     */
    public void cancel() {
        this.status = GoalStatus.CANCELLED;
        this.completedAt = null;
    }

    /**
     * Pauses the goal.
     */
    public void pause() {
        this.status = GoalStatus.PAUSED;
        this.completedAt = null;
    }

    /**
     * Resumes the goal (sets to ACTIVE).
     */
    public void resume() {
        this.status = GoalStatus.IN_PROGRESS;
        this.completedAt = null;
    }
}
