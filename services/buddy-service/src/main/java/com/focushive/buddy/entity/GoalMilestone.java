package com.focushive.buddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a milestone within a shared goal.
 * Contains milestone-related information including title, description,
 * target date, completion status, and celebration tracking.
 *
 * Database mapping:
 * - Maps to goal_milestones table
 * - Uses UUID for primary key
 * - References shared_goals table via goalId
 * - Includes audit fields (created_at, updated_at)
 * - Tracks completion and celebration status
 */
@Entity
@Table(name = "goal_milestones",
       indexes = {
           @Index(name = "idx_goal_id", columnList = "goal_id"),
           @Index(name = "idx_target_date", columnList = "target_date"),
           @Index(name = "idx_completed_at", columnList = "completed_at"),
           @Index(name = "idx_completed_by", columnList = "completed_by"),
           @Index(name = "idx_celebration_sent", columnList = "celebration_sent")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @NotNull
    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    // Back-reference to goal entity for JPA relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", insertable = false, updatable = false,
               foreignKey = @ForeignKey(name = "FK_MILESTONE_GOAL",
                                       foreignKeyDefinition = "FOREIGN KEY (goal_id) REFERENCES shared_goals(id) ON DELETE CASCADE"))
    private BuddyGoal goal;

    @NotNull
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Builder.Default
    @Column(name = "celebration_sent", nullable = false)
    private Boolean celebrationSent = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Note: Avoiding bidirectional mapping to prevent circular dependency issues
    // The relationship is maintained via goalId field only

    /**
     * Validates milestone constraints before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void validateMilestone() {
        // Validate required fields
        if (goalId == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Goal ID cannot be null (constraint not_null_goal_id)");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Title cannot be null or empty (constraint not_null_title)");
        }

        // Ensure celebration sent is never null
        if (celebrationSent == null) {
            celebrationSent = false;
        }

        // If completed, ensure completed timestamp and user are set
        if (completedAt != null && completedBy == null) {
            // Allow completion without specifying user for flexibility
            // completedBy can be set separately if needed
        }

        // If not completed, clear completion fields
        if (completedAt == null) {
            completedBy = null;
        }
    }

    /**
     * Checks if this milestone is completed.
     * @return true if completedAt is not null
     */
    public boolean isCompleted() {
        return completedAt != null;
    }

    /**
     * Checks if this milestone is overdue.
     * @return true if target date is in the past and milestone is not completed
     */
    public boolean isOverdue() {
        return targetDate != null &&
               targetDate.isBefore(LocalDate.now()) &&
               !isCompleted();
    }

    /**
     * Completes the milestone with the specified user and timestamp.
     * @param userId the user who completed the milestone
     */
    public void complete(UUID userId) {
        this.completedAt = LocalDateTime.now();
        this.completedBy = userId;
    }

    /**
     * Completes the milestone with current timestamp.
     */
    public void complete() {
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Marks the milestone as incomplete.
     */
    public void markIncomplete() {
        this.completedAt = null;
        this.completedBy = null;
        this.celebrationSent = false;
    }

    /**
     * Marks celebration as sent.
     */
    public void markCelebrationSent() {
        this.celebrationSent = true;
    }

    /**
     * Checks if celebration has been sent for this milestone.
     * @return true if celebration was sent
     */
    public boolean isCelebrationSent() {
        return Boolean.TRUE.equals(celebrationSent);
    }

    /**
     * Gets the number of days until the target date.
     * @return days until target date, negative if overdue, null if no target date
     */
    public Long getDaysUntilTarget() {
        if (targetDate == null) {
            return null;
        }

        LocalDate now = LocalDate.now();
        return now.until(targetDate, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * Gets the number of days since completion.
     * @return days since completion, null if not completed
     */
    public Long getDaysSinceCompletion() {
        if (completedAt == null) {
            return null;
        }

        LocalDate completionDate = completedAt.toLocalDate();
        LocalDate now = LocalDate.now();
        return completionDate.until(now, java.time.temporal.ChronoUnit.DAYS);
    }
}