package com.focushive.analytics.enums;

/**
 * Enumeration of different achievement types in the FocusHive application.
 * Each achievement has specific criteria for unlocking.
 */
public enum AchievementType {

    // Beginner Achievements
    FIRST_FOCUS("First Focus", "Complete your first focus session", 1),
    EARLY_BIRD("Early Bird", "Complete a session before 7 AM", 1),
    NIGHT_OWL("Night Owl", "Complete a session after 10 PM", 1),

    // Session Count Achievements
    TEN_SESSIONS("Decade", "Complete 10 focus sessions", 10),
    FIFTY_SESSIONS("Half Century", "Complete 50 focus sessions", 50),
    HUNDRED_SESSIONS("Century", "Complete 100 focus sessions", 100),

    // Streak Achievements
    THREE_DAY_STREAK("Consistency Starter", "Maintain a 3-day focus streak", 3),
    WEEK_WARRIOR("Week Warrior", "Maintain a 7-day focus streak", 7),
    MONTH_MASTER("Month Master", "Maintain a 30-day focus streak", 30),
    CENTURY_STREAK("Century Streak", "Maintain a 100-day focus streak", 100),

    // Duration Achievements
    MARATHON_RUNNER("Marathon Runner", "Complete a 3-hour focus session", 180),
    ULTRA_RUNNER("Ultra Runner", "Complete a 5-hour focus session", 300),
    ENDURANCE_MASTER("Endurance Master", "Complete a 8-hour focus session", 480),

    // Productivity Score Achievements
    HIGH_PERFORMER("High Performer", "Achieve productivity score of 90+", 90),
    PEAK_PERFORMER("Peak Performer", "Achieve productivity score of 95+", 95),
    PERFECT_SCORE("Perfect Score", "Achieve productivity score of 100", 100),

    // Hive Participation Achievements
    TEAM_PLAYER("Team Player", "Complete 10 sessions in hives", 10),
    HIVE_LEADER("Hive Leader", "Lead productivity in a hive for 7 days", 7),
    SOCIAL_BUTTERFLY("Social Butterfly", "Be active in 5 different hives", 5),

    // Special Achievements
    DISTRACTION_FREE("Distraction Free", "Complete 10 sessions with zero distractions", 10),
    GOAL_CRUSHER("Goal Crusher", "Achieve daily goals for 30 consecutive days", 30),
    WEEKEND_WARRIOR("Weekend Warrior", "Complete sessions on 10 weekends", 10);

    private final String name;
    private final String description;
    private final int targetValue;

    AchievementType(String name, String description, int targetValue) {
        this.name = name;
        this.description = description;
        this.targetValue = targetValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getTargetValue() {
        return targetValue;
    }

    /**
     * Get achievement category for grouping purposes
     */
    public String getCategory() {
        return switch (this) {
            case FIRST_FOCUS, EARLY_BIRD, NIGHT_OWL -> "Getting Started";
            case TEN_SESSIONS, FIFTY_SESSIONS, HUNDRED_SESSIONS -> "Session Milestones";
            case THREE_DAY_STREAK, WEEK_WARRIOR, MONTH_MASTER, CENTURY_STREAK -> "Consistency";
            case MARATHON_RUNNER, ULTRA_RUNNER, ENDURANCE_MASTER -> "Endurance";
            case HIGH_PERFORMER, PEAK_PERFORMER, PERFECT_SCORE -> "Performance";
            case TEAM_PLAYER, HIVE_LEADER, SOCIAL_BUTTERFLY -> "Social";
            case DISTRACTION_FREE, GOAL_CRUSHER, WEEKEND_WARRIOR -> "Special";
        };
    }

    /**
     * Get achievement points awarded when unlocked
     */
    public int getPoints() {
        return switch (this) {
            case FIRST_FOCUS, EARLY_BIRD, NIGHT_OWL -> 10;
            case TEN_SESSIONS, THREE_DAY_STREAK -> 25;
            case FIFTY_SESSIONS, WEEK_WARRIOR, MARATHON_RUNNER, HIGH_PERFORMER, TEAM_PLAYER -> 50;
            case HUNDRED_SESSIONS, MONTH_MASTER, ULTRA_RUNNER, PEAK_PERFORMER, HIVE_LEADER -> 100;
            case CENTURY_STREAK, ENDURANCE_MASTER, PERFECT_SCORE, SOCIAL_BUTTERFLY -> 200;
            case DISTRACTION_FREE, GOAL_CRUSHER, WEEKEND_WARRIOR -> 75;
        };
    }
}