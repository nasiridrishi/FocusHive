package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entity representing user streak information.
 * Tracks current and longest streaks for focus session completion.
 */
@Entity
@Table(name = "user_streaks",
    indexes = {
        @Index(name = "idx_user_streaks_user_id", columnList = "user_id"),
        @Index(name = "idx_user_streaks_current_streak", columnList = "current_streak"),
        @Index(name = "idx_user_streaks_longest_streak", columnList = "longest_streak")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserStreak extends BaseEntity {

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @NotNull(message = "Current streak is required")
    @Min(value = 0, message = "Current streak cannot be negative")
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @NotNull(message = "Longest streak is required")
    @Min(value = 0, message = "Longest streak cannot be negative")
    @Column(name = "longest_streak", nullable = false)
    private Integer longestStreak = 0;

    @NotNull(message = "Last active date is required")
    @Column(name = "last_active_date", nullable = false)
    private LocalDate lastActiveDate;

    @NotNull(message = "Streak start date is required")
    @Column(name = "streak_start_date", nullable = false)
    private LocalDate streakStartDate;

    @Min(value = 0, message = "Total active days cannot be negative")
    @Column(name = "total_active_days")
    private Integer totalActiveDays = 0;

    @Min(value = 0, message = "Streak freezes used cannot be negative")
    @Column(name = "streak_freezes_used")
    private Integer streakFreezesUsed = 0;

    @Min(value = 0, message = "Available streak freezes cannot be negative")
    @Column(name = "available_streak_freezes")
    private Integer availableStreakFreezes = 2; // Default 2 freezes per month

    /**
     * Update streak based on activity today
     */
    public void updateStreak(boolean activeToday) {
        LocalDate today = LocalDate.now();

        if (activeToday) {
            if (lastActiveDate == null || lastActiveDate.equals(today.minusDays(1))) {
                // Continue or start streak
                if (currentStreak == 0) {
                    streakStartDate = today;
                }
                currentStreak++;

                // Update longest streak if current exceeds it
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }

                totalActiveDays++;
                lastActiveDate = today;
            } else if (lastActiveDate.equals(today)) {
                // Already updated today, no change
                return;
            } else {
                // Gap detected, reset streak unless freeze is used
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastActiveDate, today);

                if (daysBetween == 1 || canUseStreakFreeze()) {
                    // Use freeze or no gap
                    if (daysBetween > 1 && availableStreakFreezes > 0) {
                        availableStreakFreezes--;
                        streakFreezesUsed++;
                    }
                    currentStreak++;
                    if (currentStreak > longestStreak) {
                        longestStreak = currentStreak;
                    }
                } else {
                    // Reset streak
                    currentStreak = 1;
                    streakStartDate = today;
                }

                totalActiveDays++;
                lastActiveDate = today;
            }
        }
    }

    /**
     * Check if user can use a streak freeze
     */
    public boolean canUseStreakFreeze() {
        return availableStreakFreezes > 0;
    }

    /**
     * Reset monthly streak freezes (to be called monthly)
     */
    public void resetMonthlyFreezes() {
        availableStreakFreezes = 2;
        streakFreezesUsed = 0;
    }

    /**
     * Get streak percentage for this month
     */
    public double getMonthlyStreakPercentage() {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);

        if (streakStartDate == null || streakStartDate.isAfter(now)) {
            return 0.0;
        }

        LocalDate effectiveStart = streakStartDate.isBefore(monthStart) ? monthStart : streakStartDate;
        long daysInStreak = java.time.temporal.ChronoUnit.DAYS.between(effectiveStart, now) + 1;
        long monthDays = monthStart.lengthOfMonth();

        return Math.min(100.0, (double) daysInStreak / monthDays * 100.0);
    }

    /**
     * Check if streak is at risk (no activity yesterday)
     */
    public boolean isStreakAtRisk() {
        if (lastActiveDate == null) return true;
        return !lastActiveDate.equals(LocalDate.now()) && !lastActiveDate.equals(LocalDate.now().minusDays(1));
    }
}