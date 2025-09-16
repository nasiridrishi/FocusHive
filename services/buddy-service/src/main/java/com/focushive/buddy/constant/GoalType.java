package com.focushive.buddy.constant;

/**
 * Enumeration of goal types for buddy partnerships.
 * Represents different categories of goals that buddies can work on together.
 */
public enum GoalType {

    PERSONAL("Personal", "Individual growth and development goals"),
    PROFESSIONAL("Professional", "Career and work-related goals"),
    HEALTH("Health", "Physical and mental health goals"),
    EDUCATIONAL("Educational", "Learning and skill development goals"),
    FITNESS("Fitness", "Exercise and physical fitness goals"),
    HABIT("Habit", "Building or breaking habits"),
    PROJECT("Project", "Specific project completion goals"),
    FINANCIAL("Financial", "Money management and financial goals"),
    SOCIAL("Social", "Relationship and social skill goals"),
    CREATIVE("Creative", "Artistic and creative pursuits");

    private final String displayName;
    private final String description;

    GoalType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this goal type requires daily check-ins.
     * Habit and fitness goals typically need daily tracking.
     */
    public boolean requiresDailyCheckin() {
        return this == HABIT || this == FITNESS || this == HEALTH;
    }

    /**
     * Gets the default duration in days for this goal type.
     */
    public int getDefaultDurationDays() {
        return switch (this) {
            case HABIT -> 30;
            case FITNESS -> 90;
            case PROJECT -> 60;
            case EDUCATIONAL -> 120;
            default -> 30;
        };
    }
}