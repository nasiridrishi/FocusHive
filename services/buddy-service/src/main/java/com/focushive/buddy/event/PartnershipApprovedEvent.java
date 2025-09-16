package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a partnership request is approved and activated.
 *
 * Triggers:
 * - Celebration notifications to both partners
 * - Goal synchronization setup
 * - Onboarding workflow initiation
 * - Analytics tracking for approval rates
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnershipApprovedEvent extends ApplicationEvent {

    private final UUID partnershipId;
    private final String approverId;
    private final String partnerId;
    private final ZonedDateTime approvedAt;
    private final ZonedDateTime startedAt;

    public PartnershipApprovedEvent(Object source, UUID partnershipId, String approverId, String partnerId) {
        super(source);
        this.partnershipId = partnershipId;
        this.approverId = approverId;
        this.partnerId = partnerId;
        this.approvedAt = ZonedDateTime.now();
        this.startedAt = ZonedDateTime.now();
    }
}