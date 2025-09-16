package com.focushive.buddy.constant;

/**
 * Enumeration of mood types for check-ins.
 * Represents different emotional states that users can report.
 */
public enum MoodType {

    MOTIVATED("Motivated", 9, "💪"),
    FOCUSED("Focused", 8, "🎯"),
    STRESSED("Stressed", 3, "😰"),
    TIRED("Tired", 4, "😴"),
    EXCITED("Excited", 10, "🎉"),
    NEUTRAL("Neutral", 5, "😐"),
    FRUSTRATED("Frustrated", 2, "😤"),
    ACCOMPLISHED("Accomplished", 9, "🏆");

    private final String displayName;
    private final int emotionalScore; // 1-10 scale, where 10 is most positive
    private final String emoji;

    // Thresholds for mood categorization
    private static final int POSITIVE_THRESHOLD = 7;
    private static final int NEGATIVE_THRESHOLD = 4;

    MoodType(String displayName, int emotionalScore, String emoji) {
        this.displayName = displayName;
        this.emotionalScore = emotionalScore;
        this.emoji = emoji;
    }

    /**
     * Gets the human-readable display name for this mood.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the emotional score for this mood on a scale of 1-10.
     * Higher scores indicate more positive emotions.
     *
     * @return the emotional score
     */
    public int getEmotionalScore() {
        return emotionalScore;
    }

    /**
     * Gets the emoji representation for this mood.
     *
     * @return the emoji string
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Checks if this mood is considered positive (score >= 7).
     *
     * @return true if the mood is positive, false otherwise
     */
    public boolean isPositive() {
        return emotionalScore >= POSITIVE_THRESHOLD;
    }

    /**
     * Checks if this mood is considered negative (score <= 4).
     *
     * @return true if the mood is negative, false otherwise
     */
    public boolean isNegative() {
        return emotionalScore <= NEGATIVE_THRESHOLD;
    }

    /**
     * Checks if this mood is considered neutral (score == 5).
     *
     * @return true if the mood is neutral, false otherwise
     */
    public boolean isNeutral() {
        return emotionalScore == 5;
    }
}