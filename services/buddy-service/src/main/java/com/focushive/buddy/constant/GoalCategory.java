package com.focushive.buddy.constant;

/**
 * Enumeration of goal categories for organization and filtering.
 * Provides a way to group similar goals together.
 */
public enum GoalCategory {

    PRODUCTIVITY("Productivity", "Goals related to productivity and efficiency"),
    WELLNESS("Wellness", "Health and wellness related goals"),
    LEARNING("Learning", "Educational and skill development goals"),
    CAREER("Career", "Professional and career advancement goals"),
    LIFESTYLE("Lifestyle", "Personal lifestyle and habit goals"),
    RELATIONSHIPS("Relationships", "Social and relationship goals"),
    FINANCE("Finance", "Financial planning and management goals"),
    CREATIVITY("Creativity", "Creative and artistic goals"),
    MINDFULNESS("Mindfulness", "Mental health and mindfulness goals"),
    FITNESS("Fitness", "Physical fitness and exercise goals");

    private final String displayName;
    private final String description;

    GoalCategory(String displayName, String description) {
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
     * Maps a goal type to its primary category.
     */
    public static GoalCategory fromGoalType(GoalType goalType) {
        return switch (goalType) {
            case PERSONAL, HABIT -> LIFESTYLE;
            case PROFESSIONAL -> CAREER;
            case HEALTH -> WELLNESS;
            case EDUCATIONAL -> LEARNING;
            case FITNESS -> FITNESS;
            case PROJECT -> PRODUCTIVITY;
            case FINANCIAL -> FINANCE;
            case SOCIAL -> RELATIONSHIPS;
            case CREATIVE -> CREATIVITY;
        };
    }

    /**
     * Checks if this category typically requires accountability partners.
     */
    public boolean requiresAccountability() {
        return this == FITNESS || this == PRODUCTIVITY || this == LEARNING || this == WELLNESS;
    }
}