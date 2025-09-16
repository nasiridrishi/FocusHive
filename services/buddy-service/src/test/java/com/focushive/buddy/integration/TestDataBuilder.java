package com.focushive.buddy.integration;

import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.entity.BuddyPartnership;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
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
    public static final double DEFAULT_HEALTH_SCORE = 1.00; // Match entity default

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

    // BuddyPartnership Builder

    /**
     * Creates a BuddyPartnership builder with default values
     */
    public BuddyPartnershipBuilder buildBuddyPartnership() {
        return new BuddyPartnershipBuilder();
    }

    /**
     * Creates a list of test BuddyPartnerships
     */
    public List<BuddyPartnership> buildBuddyPartnerships(int count) {
        List<BuddyPartnership> partnerships = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            partnerships.add(buildBuddyPartnership()
                .withUser1Id(UUID.randomUUID())
                .withUser2Id(UUID.randomUUID())
                .build());
        }
        return partnerships;
    }

    /**
     * Builder class for BuddyPartnership test data
     */
    public static class BuddyPartnershipBuilder {
        private UUID user1Id = UUID.randomUUID();
        private UUID user2Id = UUID.randomUUID();
        private PartnershipStatus status = PartnershipStatus.PENDING;
        private ZonedDateTime startedAt;
        private ZonedDateTime endedAt;
        private String endReason;
        private String agreementText = DEFAULT_AGREEMENT_TEXT;
        private Integer durationDays = DEFAULT_PARTNERSHIP_DURATION;
        private BigDecimal compatibilityScore = BigDecimal.valueOf(DEFAULT_COMPATIBILITY_SCORE);
        private BigDecimal healthScore = BigDecimal.valueOf(DEFAULT_HEALTH_SCORE);
        private ZonedDateTime lastInteractionAt;
        private LocalDateTime createdAt;

        public BuddyPartnershipBuilder withUser1Id(UUID user1Id) {
            this.user1Id = user1Id;
            return this;
        }

        public BuddyPartnershipBuilder withUser2Id(UUID user2Id) {
            this.user2Id = user2Id;
            return this;
        }

        public BuddyPartnershipBuilder withStatus(PartnershipStatus status) {
            this.status = status;
            return this;
        }

        public BuddyPartnershipBuilder withStartedAt(ZonedDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public BuddyPartnershipBuilder withEndedAt(ZonedDateTime endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public BuddyPartnershipBuilder withEndReason(String endReason) {
            this.endReason = endReason;
            return this;
        }

        public BuddyPartnershipBuilder withAgreementText(String agreementText) {
            this.agreementText = agreementText;
            return this;
        }

        public BuddyPartnershipBuilder withDurationDays(Integer durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public BuddyPartnershipBuilder withCompatibilityScore(BigDecimal compatibilityScore) {
            this.compatibilityScore = compatibilityScore;
            return this;
        }

        public BuddyPartnershipBuilder withHealthScore(BigDecimal healthScore) {
            this.healthScore = healthScore;
            return this;
        }

        public BuddyPartnershipBuilder withLastInteractionAt(ZonedDateTime lastInteractionAt) {
            this.lastInteractionAt = lastInteractionAt;
            return this;
        }

        public BuddyPartnershipBuilder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BuddyPartnership build() {
            return BuddyPartnership.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .status(status)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .endReason(endReason)
                .agreementText(agreementText)
                .durationDays(durationDays)
                .compatibilityScore(compatibilityScore)
                .healthScore(healthScore)
                .lastInteractionAt(lastInteractionAt)
                .createdAt(createdAt)
                .build();
        }
    }
}