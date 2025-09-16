package com.focushive.buddy.constant;

/**
 * Enumeration of milestone statuses for goal tracking.
 * Represents the current state of a milestone within a goal.
 */
public enum MilestoneStatus {

    NOT_STARTED("Not Started", "Milestone has not been started yet", 0),
    IN_PROGRESS("In Progress", "Currently working on this milestone", 50),
    BLOCKED("Blocked", "Progress is blocked by dependencies or issues", 25),
    COMPLETED("Completed", "Milestone has been successfully completed", 100),
    SKIPPED("Skipped", "Milestone was skipped or deemed unnecessary", 0),
    OVERDUE("Overdue", "Milestone has passed its target date", 0);

    private final String displayName;
    private final String description;
    private final int progressPercentage;

    MilestoneStatus(String displayName, String description, int progressPercentage) {
        this.displayName = displayName;
        this.description = description;
        this.progressPercentage = progressPercentage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    /**
     * Checks if this status represents completion.
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * Checks if this status indicates active work.
     */
    public boolean isActive() {
        return this == IN_PROGRESS;
    }

    /**
     * Checks if this status indicates a problem.
     */
    public boolean isProblematic() {
        return this == BLOCKED || this == OVERDUE;
    }

    /**
     * Determines if transition to another status is valid.
     */
    public boolean canTransitionTo(MilestoneStatus newStatus) {
        if (this == newStatus) return false;

        return switch (this) {
            case NOT_STARTED -> true; // Can transition to any status
            case IN_PROGRESS -> newStatus != NOT_STARTED; // Can't go back to not started
            case BLOCKED -> newStatus != NOT_STARTED; // Can unblock but not restart
            case COMPLETED -> newStatus == IN_PROGRESS || newStatus == BLOCKED; // Can reopen if needed
            case SKIPPED -> newStatus == IN_PROGRESS || newStatus == NOT_STARTED; // Can unskip
            case OVERDUE -> true; // Can transition to any status
        };
    }
}