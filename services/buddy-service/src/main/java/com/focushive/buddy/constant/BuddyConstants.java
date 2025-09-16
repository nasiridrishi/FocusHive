package com.focushive.buddy.constant;

/**
 * Constants used throughout the buddy service.
 * Contains default values, thresholds, and configuration parameters.
 */
public final class BuddyConstants {

    /**
     * Partnership duration constants.
     */
    public static final int DEFAULT_PARTNERSHIP_DURATION_DAYS = 30;
    public static final int MIN_PARTNERSHIP_DURATION_DAYS = 7;
    public static final int MAX_PARTNERSHIP_DURATION_DAYS = 365;

    /**
     * Partnership limits and thresholds.
     */
    public static final int MAX_PARTNERSHIPS_PER_USER = 3;
    public static final double COMPATIBILITY_THRESHOLD = 0.6;
    public static final double MIN_CHECKIN_RATE_GOOD_STANDING = 0.7;

    /**
     * Check-in and streak constants.
     */
    public static final int STREAK_BREAK_THRESHOLD_DAYS = 2;
    public static final int DEFAULT_CHECKIN_REMINDER_HOUR = 20; // 8 PM
    public static final int CHECKIN_GRACE_PERIOD_HOURS = 24;

    /**
     * Matching constants.
     */
    public static final int MAX_MATCH_SUGGESTIONS = 10;

    /**
     * Default values.
     */
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final int PARTNERSHIP_REQUEST_EXPIRES_HOURS = 72; // 3 days

    /**
     * Private constructor to prevent instantiation.
     */
    private BuddyConstants() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}