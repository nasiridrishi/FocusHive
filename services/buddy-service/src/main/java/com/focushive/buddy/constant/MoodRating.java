package com.focushive.buddy.constant;

/**
 * Mood rating scale for buddy check-ins.
 * Helps track emotional state and well-being.
 */
public enum MoodRating {
    EXCELLENT("Excellent", 5, "Feeling great and highly motivated"),
    GOOD("Good", 4, "Positive mood and productive"),
    NEUTRAL("Neutral", 3, "Stable mood, neither positive nor negative"),
    FAIR("Fair", 2, "Some challenges but managing"),
    POOR("Poor", 1, "Struggling and may need support");

    private final String displayName;
    private final int score;
    private final String description;

    MoodRating(String displayName, int score, String description) {
        this.displayName = displayName;
        this.score = score;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this mood indicates the user may need support
     */
    public boolean needsSupport() {
        return score <= 2;
    }

    /**
     * Check if this mood is positive
     */
    public boolean isPositive() {
        return score >= 4;
    }

    /**
     * Get mood by score value
     */
    public static MoodRating fromScore(int score) {
        for (MoodRating mood : values()) {
            if (mood.score == score) {
                return mood;
            }
        }
        return NEUTRAL; // Default to neutral if score not found
    }
}