package com.focushive.analytics.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event published when a user reaches a streak milestone.
 * This event triggers achievement checks and notifications.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class StreakMilestoneEvent extends ApplicationEvent {

    private String userId;
    private int streakDays;
    private int previousStreak;
    private LocalDate milestoneDate;
    private LocalDateTime eventTime;
    private boolean isPersonalBest;
    private String milestoneType; // "DAILY", "WEEKLY", "MONTHLY", "MAJOR"

    public StreakMilestoneEvent(Object source) {
        super(source);
    }

    public StreakMilestoneEvent(Object source, String userId, int streakDays, int previousStreak) {
        super(source);
        this.userId = userId;
        this.streakDays = streakDays;
        this.previousStreak = previousStreak;
        this.milestoneDate = LocalDate.now();
        this.eventTime = LocalDateTime.now();
        this.milestoneType = determineMilestoneType(streakDays);
    }

    private String determineMilestoneType(int days) {
        if (days >= 100) return "MAJOR";
        if (days >= 30) return "MONTHLY";
        if (days >= 7) return "WEEKLY";
        return "DAILY";
    }

    public boolean isMajorMilestone() {
        return streakDays == 3 || streakDays == 7 || streakDays == 14 ||
               streakDays == 30 || streakDays == 50 || streakDays == 100 ||
               streakDays % 100 == 0; // Every 100 days after 100
    }

    public String getMilestoneMessage() {
        return switch (streakDays) {
            case 3 -> "🔥 3-day streak! You're building momentum!";
            case 7 -> "🚀 One week strong! Amazing consistency!";
            case 14 -> "💪 Two weeks! You're on fire!";
            case 30 -> "🏆 30-day milestone! You're a focus champion!";
            case 50 -> "⭐ 50 days! Incredible dedication!";
            case 100 -> "👑 100-day legend! You're unstoppable!";
            default -> String.format("🎉 %d-day streak! Keep it up!", streakDays);
        };
    }
}