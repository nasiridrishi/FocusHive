package com.focushive.buddy.constant;

/**
 * Enumeration of check-in frequencies for buddy partnerships.
 * Defines how often buddies are expected to check in with each other.
 */
public enum CheckinFrequency {

    DAILY("Daily", "Check-in required every day", 1),
    EVERY_OTHER_DAY("Every Other Day", "Check-in required every 2 days", 2),
    TWICE_WEEKLY("Twice Weekly", "Check-in required twice per week", 3),
    WEEKLY("Weekly", "Check-in required once per week", 7),
    BIWEEKLY("Biweekly", "Check-in required every two weeks", 14),
    MONTHLY("Monthly", "Check-in required once per month", 30),
    AS_NEEDED("As Needed", "No fixed schedule, check-in when needed", 0);

    private final String displayName;
    private final String description;
    private final int daysBetweenCheckins;

    CheckinFrequency(String displayName, String description, int daysBetweenCheckins) {
        this.displayName = displayName;
        this.description = description;
        this.daysBetweenCheckins = daysBetweenCheckins;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getDaysBetweenCheckins() {
        return daysBetweenCheckins;
    }

    /**
     * Checks if this frequency requires strict adherence.
     */
    public boolean isStrict() {
        return this != AS_NEEDED;
    }

    /**
     * Gets the minimum acceptable check-in rate for this frequency.
     * Used for accountability scoring.
     */
    public double getMinimumComplianceRate() {
        return switch (this) {
            case DAILY -> 0.85; // 85% compliance for daily
            case EVERY_OTHER_DAY -> 0.80; // 80% for every other day
            case TWICE_WEEKLY -> 0.75; // 75% for twice weekly
            case WEEKLY -> 0.70; // 70% for weekly
            case BIWEEKLY -> 0.65; // 65% for biweekly
            case MONTHLY -> 0.60; // 60% for monthly
            case AS_NEEDED -> 0.0; // No minimum for as-needed
        };
    }

    /**
     * Calculates the grace period in hours for late check-ins.
     */
    public int getGracePeriodHours() {
        return switch (this) {
            case DAILY -> 6; // 6 hour grace period
            case EVERY_OTHER_DAY -> 12; // 12 hour grace period
            case TWICE_WEEKLY, WEEKLY -> 24; // 24 hour grace period
            case BIWEEKLY, MONTHLY -> 48; // 48 hour grace period
            case AS_NEEDED -> Integer.MAX_VALUE; // No deadline
        };
    }

    /**
     * Determines the recommended frequency based on goal type.
     */
    public static CheckinFrequency recommendedForGoalType(GoalType goalType) {
        return switch (goalType) {
            case HABIT, FITNESS -> DAILY;
            case HEALTH -> EVERY_OTHER_DAY;
            case PERSONAL, PROFESSIONAL -> TWICE_WEEKLY;
            case EDUCATIONAL, PROJECT -> WEEKLY;
            case FINANCIAL, SOCIAL -> BIWEEKLY;
            case CREATIVE -> AS_NEEDED;
        };
    }
}