package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Event fired when a new partnership is created (request sent).
 *
 * Triggers:
 * - Notification to recipient about new partnership request
 * - Analytics tracking for partnership creation metrics
 * - Recommendation engine updates for future matching
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartnershipCreatedEvent extends ApplicationEvent {

    private final UUID partnershipId;
    private final String requesterId;
    private final String recipientId;
    private final Integer durationDays;
    private final String agreementText;
    private final ZonedDateTime createdAt;

    public PartnershipCreatedEvent(Object source, UUID partnershipId, String requesterId,
                                  String recipientId, Integer durationDays, String agreementText) {
        super(source);
        this.partnershipId = partnershipId;
        this.requesterId = requesterId;
        this.recipientId = recipientId;
        this.durationDays = durationDays;
        this.agreementText = agreementText;
        this.createdAt = ZonedDateTime.now();
    }
}