package com.focushive.buddy.entity;

import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entity representing a check-in between buddy partners.
 * Contains mood, productivity, and content information for tracking progress.
 *
 * Database mapping:
 * - Maps to buddy_checkins table
 * - Uses UUID for primary key and foreign keys
 * - Includes audit fields (created_at)
 * - Enforces constraint checking for enums and ratings
 */
@Entity
@Table(name = "buddy_checkins",
       indexes = {
           @Index(name = "idx_buddy_checkins_partnership_id", columnList = "partnership_id"),
           @Index(name = "idx_buddy_checkins_user_id", columnList = "user_id"),
           @Index(name = "idx_buddy_checkins_created_at", columnList = "created_at"),
           @Index(name = "idx_buddy_checkins_partnership_user", columnList = "partnership_id, user_id")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuddyCheckin {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @NotNull
    @Column(name = "partnership_id", nullable = false)
    private UUID partnershipId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "checkin_type", nullable = false, length = 20)
    private CheckInType checkinType = CheckInType.DAILY;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "mood", length = 20)
    private MoodType mood;

    @Min(value = 1, message = "Productivity rating must be at least 1")
    @Max(value = 10, message = "Productivity rating must not exceed 10")
    @Column(name = "productivity_rating")
    private Integer productivityRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationship to partnership
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", insertable = false, updatable = false)
    private BuddyPartnership partnership;

    /**
     * Validates checkin constraints before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void validateCheckin() {
        // Set createdAt if not already set (only on persist)
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        // Set defaults and validate required fields
        if (partnershipId == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Partnership ID cannot be null");
        }
        if (userId == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "User ID cannot be null");
        }
        if (checkinType == null) {
            checkinType = CheckInType.DAILY;
        }

        // Validate productivity rating range
        if (productivityRating != null) {
            if (productivityRating < 1 || productivityRating > 10) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                    "Productivity rating must be between 1 and 10 (constraint chk_productivity_rating)");
            }
        }
    }

    /**
     * Checks if this check-in was created today.
     * @return true if created today, false otherwise
     */
    public boolean isToday() {
        if (createdAt == null) {
            return false;
        }
        return createdAt.toLocalDate().equals(LocalDate.now());
    }

    /**
     * Gets the number of days since this check-in was created.
     * @return number of days since creation
     */
    public long getDaysSince() {
        if (createdAt == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }

    /**
     * Checks if this check-in has positive mood.
     * @return true if mood is positive, false otherwise
     */
    public boolean hasPositiveMood() {
        return mood != null && mood.isPositive();
    }

    /**
     * Checks if this check-in has negative mood.
     * @return true if mood is negative, false otherwise
     */
    public boolean hasNegativeMood() {
        return mood != null && mood.isNegative();
    }

    /**
     * Checks if this check-in has high productivity rating (>= 7).
     * @return true if productivity is high, false otherwise
     */
    public boolean hasHighProductivity() {
        return productivityRating != null && productivityRating >= 7;
    }

    /**
     * Checks if this check-in has low productivity rating (<= 4).
     * @return true if productivity is low, false otherwise
     */
    public boolean hasLowProductivity() {
        return productivityRating != null && productivityRating <= 4;
    }

    /**
     * Gets the emotional score from the mood type.
     * @return emotional score 1-10, or null if no mood set
     */
    public Integer getEmotionalScore() {
        return mood != null ? mood.getEmotionalScore() : null;
    }

    /**
     * Checks if this check-in was created on the specified date.
     * @param date the date to check
     * @return true if created on the specified date
     */
    public boolean wasCreatedOn(LocalDate date) {
        return createdAt != null && createdAt.toLocalDate().equals(date);
    }

    /**
     * Checks if this check-in was created after the specified date.
     * @param date the date to check
     * @return true if created after the specified date
     */
    public boolean wasCreatedAfter(LocalDate date) {
        return createdAt != null && createdAt.toLocalDate().isAfter(date);
    }

    /**
     * Checks if this check-in was created before the specified date.
     * @param date the date to check
     * @return true if created before the specified date
     */
    public boolean wasCreatedBefore(LocalDate date) {
        return createdAt != null && createdAt.toLocalDate().isBefore(date);
    }

    /**
     * Gets a summary string for this check-in.
     * @return summary string
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(checkinType.getDisplayName());
        if (mood != null) {
            summary.append(" - ").append(mood.getDisplayName()).append(" ").append(mood.getEmoji());
        }
        if (productivityRating != null) {
            summary.append(" (Productivity: ").append(productivityRating).append("/10)");
        }
        return summary.toString();
    }
}