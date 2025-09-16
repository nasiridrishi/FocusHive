package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a user's streak is broken.
 *
 * Triggers:
 * - Motivation/encouragement notifications
 * - Streak recovery suggestions
 * - Partner support notifications
 * - Analytics for streak patterns
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StreakBrokenEvent extends ApplicationEvent {

    private final UUID userId;
    private final UUID partnershipId;
    private final Integer brokenStreakLength;
    private final String streakType; // "daily" or "weekly"
    private final ZonedDateTime lastCheckinDate;
    private final ZonedDateTime brokenAt;
    private final String encouragementMessage;

    public StreakBrokenEvent(Object source, UUID userId, UUID partnershipId, Integer brokenStreakLength,
                            String streakType, ZonedDateTime lastCheckinDate) {
        super(source);
        this.userId = userId;
        this.partnershipId = partnershipId;
        this.brokenStreakLength = brokenStreakLength;
        this.streakType = streakType;
        this.lastCheckinDate = lastCheckinDate;
        this.brokenAt = ZonedDateTime.now();
        this.encouragementMessage = generateEncouragementMessage(brokenStreakLength, streakType);
    }

    private String generateEncouragementMessage(Integer streakLength, String type) {
        return String.format("Don't worry! Your %d-day %s streak was amazing. Let's start a new one today!",
                           streakLength, type);
    }
}