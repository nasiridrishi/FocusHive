package com.focushive.buddy.constant;

/**
 * Enumeration of check-in types.
 * Represents different types of check-ins between buddies.
 */
public enum CheckInType {

    DAILY("Daily Check-in", 24),
    WEEKLY("Weekly Check-in", 168), // 7 * 24 hours
    MILESTONE("Milestone Update", 0), // Variable frequency
    GOAL_UPDATE("Goal Progress Update", 72), // Every 3 days
    ENCOURAGEMENT("Encouragement Message", 0); // Ad-hoc

    private final String displayName;
    private final int expectedFrequencyHours;

    CheckInType(String displayName, int expectedFrequencyHours) {
        this.displayName = displayName;
        this.expectedFrequencyHours = expectedFrequencyHours;
    }

    /**
     * Gets the human-readable display name for this check-in type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the expected frequency of this check-in type in hours.
     * A value of 0 indicates variable or ad-hoc frequency.
     *
     * @return the expected frequency in hours
     */
    public int getExpectedFrequencyHours() {
        return expectedFrequencyHours;
    }
}