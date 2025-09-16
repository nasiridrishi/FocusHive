package com.focushive.buddy.entity;

import com.focushive.buddy.constant.PartnershipStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Entity representing a buddy partnership between two users.
 * Contains all partnership-related information including status, timeline,
 * compatibility scores, and health metrics.
 *
 * Database mapping:
 * - Maps to buddy_partnerships table
 * - Uses UUID for primary key and user identifiers
 * - Includes audit fields (created_at, updated_at)
 * - Enforces constraint checking for scores and user uniqueness
 */
@Entity
@Table(
    name = "buddy_partnerships",
    indexes = {
        @Index(name = "idx_user1_user2", columnList = "user1_id, user2_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_health_score", columnList = "health_score"),
        @Index(name = "idx_started_at", columnList = "started_at"),
        @Index(
            name = "idx_last_interaction",
            columnList = "last_interaction_at"
        ),
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyPartnership {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @NotNull
    @Column(name = "user1_id", nullable = false)
    private UUID user1Id;

    @NotNull
    @Column(name = "user2_id", nullable = false)
    private UUID user2Id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private PartnershipStatus status = PartnershipStatus.PENDING;

    /**
     * Tracks the original status for validation purposes (not persisted).
     */
    @Transient
    private PartnershipStatus originalStatus;

    @Column(name = "started_at")
    private ZonedDateTime startedAt;

    @Column(name = "ended_at")
    private ZonedDateTime endedAt;

    @Size(max = 200)
    @Column(name = "end_reason", length = 200)
    private String endReason;

    @Column(name = "agreement_text", columnDefinition = "TEXT")
    private String agreementText;

    @Builder.Default
    @Column(name = "duration_days")
    private Integer durationDays = 30;

    @DecimalMax(
        value = "1.00",
        message = "Compatibility score must not exceed 1.00"
    )
    @DecimalMin(
        value = "0.00",
        message = "Compatibility score must not be negative"
    )
    @Column(name = "compatibility_score", precision = 3, scale = 2)
    private BigDecimal compatibilityScore;

    @DecimalMax(value = "1.00", message = "Health score must not exceed 1.00")
    @DecimalMin(value = "0.00", message = "Health score must not be negative")
    @Builder.Default
    @Column(name = "health_score", precision = 3, scale = 2)
    private BigDecimal healthScore = BigDecimal.valueOf(1.00);

    @Column(name = "last_interaction_at")
    private ZonedDateTime lastInteractionAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Bidirectional mapping to goals with cascade delete
    // This enables JPA to handle cascade deletion matching the database constraints
    @OneToMany(
        mappedBy = "partnership",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<BuddyGoal> goals = new ArrayList<>();

    // Bidirectional mapping to checkins with cascade delete
    // This enables JPA to handle cascade deletion matching the database constraints
    @OneToMany(
        mappedBy = "partnership",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<BuddyCheckin> checkins = new ArrayList<>();

    /**
     * Captures the original status after loading from database.
     */
    @PostLoad
    private void captureOriginalStatus() {
        this.originalStatus = this.status;
    }

    /**
     * Validates partnership constraints before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void validatePartnership() {
        // Validate status transitions - prevent reactivation of ended partnerships
        if (
            originalStatus == PartnershipStatus.ENDED &&
            status == PartnershipStatus.ACTIVE
        ) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Cannot reactivate ended partnership (constraint chk_no_reactivation)"
            );
        }
        // Validate required fields
        if (user1Id == null || user2Id == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "User IDs cannot be null (constraint not_null_users)"
            );
        }

        // Prevent self-partnership - create custom exception to mimic DataIntegrityViolationException
        if (user1Id != null && user2Id != null && user1Id.equals(user2Id)) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Self-partnership not allowed (constraint chk_different_users)"
            );
        }

        // Validate status transitions
        // This is handled by the enum's canTransitionTo method but we validate here too
        if (status != null) {
            switch (status) {
                case ACTIVE:
                    if (startedAt == null) {
                        startedAt = ZonedDateTime.now();
                    }
                    break;
                case ENDED:
                    if (endedAt == null) {
                        endedAt = ZonedDateTime.now();
                    }
                    break;
                case PENDING:
                case PAUSED:
                    // No special handling needed
                    break;
            }
        }

        // Validate score ranges - create custom exceptions to mimic database constraints
        if (compatibilityScore != null) {
            if (
                compatibilityScore.compareTo(BigDecimal.ZERO) < 0 ||
                compatibilityScore.compareTo(BigDecimal.ONE) > 0
            ) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                    "Compatibility score must be between 0.0 and 1.0 (constraint chk_compatibility_score)"
                );
            }
        }

        if (healthScore != null) {
            if (
                healthScore.compareTo(BigDecimal.ZERO) < 0 ||
                healthScore.compareTo(BigDecimal.ONE) > 0
            ) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                    "Health score must be between 0.0 and 1.0 (constraint chk_health_score)"
                );
            }
        }

        // User ordering normalization is handled at the repository level for queries
    }

    /**
     * Normalizes user IDs to ensure consistent ordering for uniqueness constraints.
     * Always puts the lexicographically smaller ID as user1Id.
     */
    private void normalizeUserIds() {
        if (
            user1Id != null && user2Id != null && user1Id.toString().compareTo(user2Id.toString()) > 0
        ) {
            UUID temp = user1Id;
            user1Id = user2Id;
            user2Id = temp;
        }
    }

    /**
     * Checks if this partnership involves the specified user.
     * @param userId the user ID to check
     * @return true if the user is part of this partnership
     */
    public boolean involvesUser(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    /**
     * Gets the partner ID for the specified user.
     * @param userId the user ID
     * @return the partner's user ID, or null if the user is not part of this partnership
     */
    public UUID getPartnerIdFor(UUID userId) {
        if (user1Id.equals(userId)) {
            return user2Id;
        } else if (user2Id.equals(userId)) {
            return user1Id;
        }
        return null;
    }

    /**
     * Checks if this partnership is currently active.
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return PartnershipStatus.ACTIVE.equals(status);
    }

    /**
     * Checks if this partnership is pending.
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return PartnershipStatus.PENDING.equals(status);
    }

    /**
     * Checks if this partnership has ended.
     * @return true if status is ENDED
     */
    public boolean isEnded() {
        return PartnershipStatus.ENDED.equals(status);
    }

    /**
     * Checks if this partnership is paused.
     * @return true if status is PAUSED
     */
    public boolean isPaused() {
        return PartnershipStatus.PAUSED.equals(status);
    }

    /**
     * Calculates the duration of this partnership in days.
     * @return the number of days between start and end dates, or 0 if not available
     */
    public long calculateDuration() {
        if (startedAt != null && endedAt != null) {
            return ChronoUnit.DAYS.between(
                startedAt.toLocalDate(),
                endedAt.toLocalDate()
            );
        } else if (startedAt != null) {
            return ChronoUnit.DAYS.between(
                startedAt.toLocalDate(),
                ZonedDateTime.now().toLocalDate()
            );
        }
        return 0L;
    }

    /**
     * Checks if this status can transition to the target status.
     * @param newStatus the target status
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(PartnershipStatus newStatus) {
        if (status == null || newStatus == null) {
            return false;
        }
        return status.canTransitionTo(newStatus);
    }

    /**
     * Updates the last interaction timestamp to now.
     */
    public void updateLastInteraction() {
        this.lastInteractionAt = ZonedDateTime.now();
    }

    /**
     * Activates the partnership by setting status to ACTIVE and started timestamp.
     */
    public void activate() {
        if (!canTransitionTo(PartnershipStatus.ACTIVE)) {
            throw new IllegalStateException(
                "Cannot transition from " + status + " to ACTIVE"
            );
        }
        this.status = PartnershipStatus.ACTIVE;
        if (this.startedAt == null) {
            this.startedAt = ZonedDateTime.now();
        }
        updateLastInteraction();
    }

    /**
     * Ends the partnership with the specified reason.
     * @param reason the reason for ending the partnership
     */
    public void end(String reason) {
        if (!canTransitionTo(PartnershipStatus.ENDED)) {
            throw new IllegalStateException(
                "Cannot transition from " + status + " to ENDED"
            );
        }
        this.status = PartnershipStatus.ENDED;
        this.endedAt = ZonedDateTime.now();
        this.endReason = reason;
    }

    /**
     * Pauses the partnership.
     */
    public void pause() {
        if (!canTransitionTo(PartnershipStatus.PAUSED)) {
            throw new IllegalStateException(
                "Cannot transition from " + status + " to PAUSED"
            );
        }
        this.status = PartnershipStatus.PAUSED;
    }
}
