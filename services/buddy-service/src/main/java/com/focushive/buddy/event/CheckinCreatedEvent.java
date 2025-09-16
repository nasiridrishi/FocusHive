package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a user creates a check-in.
 *
 * Triggers:
 * - Streak calculation updates
 * - Partner notifications
 * - Accountability score updates
 * - Progress tracking analytics
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CheckinCreatedEvent extends ApplicationEvent {

    private final UUID checkinId;
    private final UUID userId;
    private final UUID partnershipId;
    private final String checkinType;
    private final Integer productivityScore;
    private final String moodType;
    private final ZonedDateTime checkinDate;
    private final String notes;

    public CheckinCreatedEvent(Object source, UUID checkinId, UUID userId, UUID partnershipId,
                              String checkinType, Integer productivityScore, String moodType, String notes) {
        super(source);
        this.checkinId = checkinId;
        this.userId = userId;
        this.partnershipId = partnershipId;
        this.checkinType = checkinType;
        this.productivityScore = productivityScore;
        this.moodType = moodType;
        this.checkinDate = ZonedDateTime.now();
        this.notes = notes;
    }
}