package com.focushive.buddy.constant;

/**
 * Enumeration of goal statuses.
 * Represents the current state of a shared goal between buddies.
 */
public enum GoalStatus {

    NOT_STARTED("Not Started", false, false),
    IN_PROGRESS("In Progress", true, false),
    COMPLETED("Completed", false, true),
    PAUSED("Paused", false, false),
    CANCELLED("Cancelled", false, false),
    OVERDUE("Overdue", true, false);

    private final String displayName;
    private final boolean isActive;
    private final boolean isCompleted;

    GoalStatus(String displayName, boolean isActive, boolean isCompleted) {
        this.displayName = displayName;
        this.isActive = isActive;
        this.isCompleted = isCompleted;
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
     * Checks if goals in this status are considered active.
     *
     * @return true if the goal is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Checks if goals in this status are completed.
     *
     * @return true if the goal is completed, false otherwise
     */
    public boolean isCompleted() {
        return isCompleted;
    }
}