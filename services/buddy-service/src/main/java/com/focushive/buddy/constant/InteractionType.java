package com.focushive.buddy.constant;

/**
 * Enumeration of interaction types between buddies.
 * Represents different types of communications and interactions.
 */
public enum InteractionType {

    MESSAGE("communication", true),
    NUDGE("motivation", true),
    CELEBRATION("recognition", true),
    CHECK_IN_REMINDER("reminder", true),
    GOAL_REMINDER("reminder", true),
    MEETING_INVITE("scheduling", true);

    private final String category;
    private final boolean requiresNotification;

    InteractionType(String category, boolean requiresNotification) {
        this.category = category;
        this.requiresNotification = requiresNotification;
    }

    /**
     * Gets the category of this interaction type.
     *
     * @return the interaction category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Checks if this interaction type requires a notification to be sent.
     *
     * @return true if notification is required, false otherwise
     */
    public boolean requiresNotification() {
        return requiresNotification;
    }
}