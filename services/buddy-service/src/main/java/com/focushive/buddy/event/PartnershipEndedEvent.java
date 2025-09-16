package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a partnership is ended (dissolved).
 *
 * Triggers:
 * - Cleanup of shared goals and data
 * - Final analytics calculation
 * - Feedback collection from both partners
 * - Recommendation updates for future matching
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnershipEndedEvent extends ApplicationEvent {

    private final UUID partnershipId;
    private final String initiatorId;
    private final String partnerId;
    private final String endReason;
    private final ZonedDateTime endedAt;
    private final Long durationDays;
    private final boolean wasSuccessful;

    public PartnershipEndedEvent(Object source, UUID partnershipId, String initiatorId,
                                String partnerId, String endReason, Long durationDays, boolean wasSuccessful) {
        super(source);
        this.partnershipId = partnershipId;
        this.initiatorId = initiatorId;
        this.partnerId = partnerId;
        this.endReason = endReason;
        this.endedAt = ZonedDateTime.now();
        this.durationDays = durationDays;
        this.wasSuccessful = wasSuccessful;
    }
}