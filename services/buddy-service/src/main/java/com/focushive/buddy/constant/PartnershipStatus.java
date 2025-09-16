package com.focushive.buddy.constant;

import java.util.Set;

/**
 * Enumeration of partnership statuses.
 * Represents the current state of a buddy partnership.
 */
public enum PartnershipStatus {

    PENDING("Pending", "Partnership request is pending acceptance"),
    ACTIVE("Active", "Partners are actively collaborating and supporting each other"),
    PAUSED("Paused", "Partnership is temporarily paused or dormant"),
    ENDED("Ended", "Partnership has been permanently ended by mutual agreement or timeout");

    private final String displayName;
    private final String description;

    /**
     * Valid transitions from each status.
     */
    private static final Set<PartnershipStatus> PENDING_TRANSITIONS = Set.of(ACTIVE, ENDED);
    private static final Set<PartnershipStatus> ACTIVE_TRANSITIONS = Set.of(PAUSED, ENDED);
    private static final Set<PartnershipStatus> PAUSED_TRANSITIONS = Set.of(ACTIVE, ENDED);
    private static final Set<PartnershipStatus> ENDED_TRANSITIONS = Set.of(); // No transitions from ended

    PartnershipStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this status.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the detailed description of this status.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status can transition to the specified target status.
     *
     * @param targetStatus the target status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(PartnershipStatus targetStatus) {
        if (targetStatus == this) {
            return false; // Cannot transition to the same status
        }

        return switch (this) {
            case PENDING -> PENDING_TRANSITIONS.contains(targetStatus);
            case ACTIVE -> ACTIVE_TRANSITIONS.contains(targetStatus);
            case PAUSED -> PAUSED_TRANSITIONS.contains(targetStatus);
            case ENDED -> ENDED_TRANSITIONS.contains(targetStatus);
        };
    }
}