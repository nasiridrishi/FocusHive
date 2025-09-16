package com.focushive.buddy.constant;

/**
 * Enumeration of communication styles for buddy preferences.
 * Represents how frequently and in what manner partners prefer to communicate.
 *
 * Values must match database CHECK constraint: ('FREQUENT', 'MODERATE', 'MINIMAL')
 */
public enum CommunicationStyle {

    FREQUENT("Frequent", "Prefers daily check-ins and regular communication"),
    MODERATE("Moderate", "Prefers regular but not daily communication, flexible timing"),
    MINIMAL("Minimal", "Prefers minimal communication, focus on essential updates only");

    private final String displayName;
    private final String description;

    CommunicationStyle(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for this communication style.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the detailed description of this communication style.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this communication style is compatible with another.
     * Frequent is compatible with Moderate and Frequent.
     * Moderate is compatible with all styles.
     * Minimal is compatible with Moderate and Minimal.
     *
     * @param other the other communication style
     * @return true if compatible, false otherwise
     */
    public boolean isCompatibleWith(CommunicationStyle other) {
        return switch (this) {
            case FREQUENT -> other == FREQUENT || other == MODERATE;
            case MODERATE -> true; // Moderate is compatible with all
            case MINIMAL -> other == MINIMAL || other == MODERATE;
        };
    }
}