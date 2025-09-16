package com.focushive.buddy.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Event fired when potential matches are found for a user.
 *
 * Triggers:
 * - Match recommendation notifications
 * - Compatibility score calculations
 * - User preference learning updates
 * - Matching algorithm improvements
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatchFoundEvent extends ApplicationEvent {

    private final String userId;
    private final List<String> matchedUserIds;
    private final List<Double> compatibilityScores;
    private final String matchingCriteria;
    private final ZonedDateTime foundAt;
    private final Integer totalMatches;

    public MatchFoundEvent(Object source, String userId, List<String> matchedUserIds,
                          List<Double> compatibilityScores, String matchingCriteria) {
        super(source);
        this.userId = userId;
        this.matchedUserIds = matchedUserIds;
        this.compatibilityScores = compatibilityScores;
        this.matchingCriteria = matchingCriteria;
        this.foundAt = ZonedDateTime.now();
        this.totalMatches = matchedUserIds.size();
    }
}