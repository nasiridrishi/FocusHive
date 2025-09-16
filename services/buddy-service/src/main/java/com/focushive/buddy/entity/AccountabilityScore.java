package com.focushive.buddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing accountability scores for users and partnerships.
 * Tracks engagement metrics, check-in completion rates, and overall accountability.
 *
 * Database mapping:
 * - Maps to accountability_scores table
 * - Uses UUID for primary key and foreign keys
 * - Includes audit fields (created_at, updated_at, calculated_at)
 * - Enforces constraint checking for scores and rates
 */
@Entity
@Table(name = "accountability_scores",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_partnership", columnNames = {"user_id", "partnership_id"})
       },
       indexes = {
           @Index(name = "idx_accountability_scores_user_partnership", columnList = "user_id, partnership_id"),
           @Index(name = "idx_accountability_scores_partnership", columnList = "partnership_id")
       })
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountabilityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Version
    private Long version;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "partnership_id")
    private UUID partnershipId;

    @NotNull
    @DecimalMax(value = "1.00", message = "Score must not exceed 1.00")
    @DecimalMin(value = "0.00", message = "Score must not be negative")
    @Builder.Default
    @Column(name = "score", precision = 3, scale = 2, nullable = false)
    private BigDecimal score = BigDecimal.valueOf(0.00);

    @Builder.Default
    @Column(name = "checkins_completed")
    private Integer checkinsCompleted = 0;

    @Builder.Default
    @Column(name = "goals_achieved")
    private Integer goalsAchieved = 0;

    @DecimalMax(value = "1.00", message = "Response rate must not exceed 1.00")
    @DecimalMin(value = "0.00", message = "Response rate must not be negative")
    @Builder.Default
    @Column(name = "response_rate", precision = 3, scale = 2)
    private BigDecimal responseRate = BigDecimal.valueOf(0.00);

    @Builder.Default
    @Column(name = "streak_days")
    private Integer streakDays = 0;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationship to partnership (optional - can be user-wide score)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", insertable = false, updatable = false)
    private BuddyPartnership partnership;

    /**
     * Validates accountability score constraints before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void validateAccountabilityScore() {
        // Validate required fields
        if (userId == null) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "User ID cannot be null");
        }

        // Set default values
        if (score == null) {
            score = BigDecimal.valueOf(0.00);
        }
        if (checkinsCompleted == null) {
            checkinsCompleted = 0;
        }
        if (goalsAchieved == null) {
            goalsAchieved = 0;
        }
        if (responseRate == null) {
            responseRate = BigDecimal.valueOf(0.00);
        }
        if (streakDays == null) {
            streakDays = 0;
        }

        // Validate score ranges
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Score must be between 0.00 and 1.00 (constraint chk_score)");
        }

        if (responseRate != null) {
            if (responseRate.compareTo(BigDecimal.ZERO) < 0 || responseRate.compareTo(BigDecimal.ONE) > 0) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                    "Response rate must be between 0.00 and 1.00 (constraint chk_response_rate)");
            }
        }

        // Update calculated timestamp
        calculatedAt = LocalDateTime.now();
    }

    /**
     * Increments the number of completed check-ins.
     */
    public void incrementCheckinsCompleted() {
        this.checkinsCompleted = (this.checkinsCompleted == null ? 0 : this.checkinsCompleted) + 1;
    }

    /**
     * Increments the number of achieved goals.
     */
    public void incrementGoalsAchieved() {
        this.goalsAchieved = (this.goalsAchieved == null ? 0 : this.goalsAchieved) + 1;
    }

    /**
     * Updates the streak days count.
     * @param streakDays new streak count
     */
    public void updateStreak(Integer streakDays) {
        this.streakDays = streakDays != null ? streakDays : 0;
    }

    /**
     * Updates the response rate.
     * @param responseRate new response rate (0.00-1.00)
     */
    public void updateResponseRate(BigDecimal responseRate) {
        if (responseRate != null && responseRate.compareTo(BigDecimal.ZERO) >= 0 && responseRate.compareTo(BigDecimal.ONE) <= 0) {
            this.responseRate = responseRate;
        }
    }

    /**
     * Calculates and updates the overall accountability score.
     * Score is based on check-in completion rate, goal achievement, response rate, and streak bonus.
     */
    public void calculateScore() {
        BigDecimal calculatedScore = BigDecimal.ZERO;

        // Check-in completion contributes 40%
        if (checkinsCompleted != null && checkinsCompleted > 0) {
            // Assume expected check-ins based on some baseline (e.g., daily check-ins)
            // For now, use a simple ratio where 7+ check-ins = 100% of this component
            BigDecimal checkinComponent = BigDecimal.valueOf(Math.min(checkinsCompleted / 7.0, 1.0))
                .multiply(BigDecimal.valueOf(0.4));
            calculatedScore = calculatedScore.add(checkinComponent);
        }

        // Goal achievement contributes 25%
        if (goalsAchieved != null && goalsAchieved > 0) {
            // Each goal contributes up to 25%, capped at full score
            BigDecimal goalComponent = BigDecimal.valueOf(Math.min(goalsAchieved / 2.0, 1.0))
                .multiply(BigDecimal.valueOf(0.25));
            calculatedScore = calculatedScore.add(goalComponent);
        }

        // Response rate contributes 20%
        if (responseRate != null) {
            BigDecimal responseComponent = responseRate.multiply(BigDecimal.valueOf(0.20));
            calculatedScore = calculatedScore.add(responseComponent);
        }

        // Streak bonus contributes 15%
        if (streakDays != null && streakDays > 0) {
            // Streak bonus: 1-3 days = 25%, 4-7 days = 50%, 8-14 days = 75%, 15+ days = 100%
            double streakMultiplier = Math.min(streakDays / 15.0, 1.0);
            BigDecimal streakComponent = BigDecimal.valueOf(streakMultiplier)
                .multiply(BigDecimal.valueOf(0.15));
            calculatedScore = calculatedScore.add(streakComponent);
        }

        // Ensure score doesn't exceed 1.00
        this.score = calculatedScore.min(BigDecimal.ONE);
        this.calculatedAt = LocalDateTime.now();
    }

    /**
     * Checks if the user has a high accountability score (>= 0.75).
     * @return true if score is high, false otherwise
     */
    public boolean hasHighAccountability() {
        return score != null && score.compareTo(BigDecimal.valueOf(0.75)) >= 0;
    }

    /**
     * Checks if the user has a low accountability score (<= 0.40).
     * @return true if score is low, false otherwise
     */
    public boolean hasLowAccountability() {
        return score != null && score.compareTo(BigDecimal.valueOf(0.40)) <= 0;
    }

    /**
     * Checks if the user is on a streak (3+ days).
     * @return true if on a streak, false otherwise
     */
    public boolean isOnStreak() {
        return streakDays != null && streakDays >= 3;
    }

    /**
     * Checks if the user is on a long streak (7+ days).
     * @return true if on a long streak, false otherwise
     */
    public boolean isOnLongStreak() {
        return streakDays != null && streakDays >= 7;
    }

    /**
     * Gets a descriptive accountability level based on score.
     * @return accountability level description
     */
    public String getAccountabilityLevel() {
        if (score == null) {
            return "Unknown";
        }

        if (score.compareTo(BigDecimal.valueOf(0.85)) >= 0) {
            return "Excellent";
        } else if (score.compareTo(BigDecimal.valueOf(0.70)) >= 0) {
            return "Good";
        } else if (score.compareTo(BigDecimal.valueOf(0.50)) >= 0) {
            return "Fair";
        } else if (score.compareTo(BigDecimal.valueOf(0.30)) >= 0) {
            return "Needs Improvement";
        } else {
            return "Poor";
        }
    }

    /**
     * Gets the score as a percentage string.
     * @return score as percentage (e.g., "75%")
     */
    public String getScorePercentage() {
        if (score == null) {
            return "0%";
        }
        return String.format("%.0f%%", score.multiply(BigDecimal.valueOf(100)).doubleValue());
    }
}