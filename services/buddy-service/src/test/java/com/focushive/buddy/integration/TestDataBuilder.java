package com.focushive.buddy.integration;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test data builder providing realistic test data for buddy service integration tests.
 * Contains entity builders following the builder pattern for TDD approach.
 * Provides factory methods for creating test entities with sensible defaults.
 *
 * NOTE: Entity builders will be implemented when entities are created.
 * This class serves as the foundation for TDD test infrastructure.
 */
public class TestDataBuilder {

    // Test user constants
    public static final String TEST_USER_1 = "test-user-1";
    public static final String TEST_USER_2 = "test-user-2";
    public static final String TEST_USER_3 = "test-user-3";
    public static final String TEST_ADMIN = "test-admin";

    // Test partnership constants
    public static final String DEFAULT_AGREEMENT_TEXT = "Let's support each other in achieving our goals through mutual accountability and encouragement!";
    public static final int DEFAULT_PARTNERSHIP_DURATION = 30; // days
    public static final double DEFAULT_COMPATIBILITY_SCORE = 0.85;
    public static final double DEFAULT_HEALTH_SCORE = 0.90;

    // Default interests and goals for testing
    public static final String DEFAULT_INTERESTS = "programming,studying,productivity,fitness";
    public static final String DEFAULT_GOALS = "complete_project,learn_new_skills,improve_focus,build_habits";
    public static final String DEFAULT_FOCUS_TIMES = "morning,afternoon";

    // Entity builders will be implemented when entities are created
    // This is a placeholder class for TDD infrastructure setup

    // Utility methods for generating test data

    /**
     * Generate random user ID for testing
     */
    public static String generateTestUserId() {
        return "test-user-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * Generate random partnership ID
     */
    public static UUID generateTestPartnershipId() {
        return UUID.randomUUID();
    }

    /**
     * Calculate compatibility score based on interests overlap
     */
    public static double calculateCompatibilityScore(String interests1, String interests2) {
        String[] int1 = interests1.toLowerCase().split(",");
        String[] int2 = interests2.toLowerCase().split(",");

        int overlap = 0;
        for (String i1 : int1) {
            for (String i2 : int2) {
                if (i1.trim().equals(i2.trim())) {
                    overlap++;
                    break;
                }
            }
        }

        double maxLength = Math.max(int1.length, int2.length);
        return Math.min(1.0, (overlap / maxLength) * 1.2); // Slight boost for realistic scores
    }

    /**
     * Generate realistic interests for different personality types
     */
    public static String generateInterestsForPersonality(String personalityType) {
        return switch (personalityType.toLowerCase()) {
            case "introvert" -> "reading,programming,meditation,writing,research";
            case "extrovert" -> "networking,group_studies,presentations,collaboration,mentoring";
            case "ambivert" -> "programming,learning,fitness,music,creative_projects";
            default -> DEFAULT_INTERESTS;
        };
    }

    /**
     * Generate focus times based on preferences
     */
    public static String generateFocusTimesForType(String type) {
        return switch (type.toLowerCase()) {
            case "early_bird" -> "early_morning,morning";
            case "night_owl" -> "evening,night";
            case "flexible" -> "morning,afternoon,evening";
            default -> DEFAULT_FOCUS_TIMES;
        };
    }
}