package com.focushive.buddy.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ValidationUtils class.
 *
 * Following TDD approach:
 * 1. RED: These tests will initially FAIL because ValidationUtils class doesn't exist
 * 2. GREEN: Implement ValidationUtils class to make tests pass
 * 3. REFACTOR: Improve implementation while keeping tests green
 */
@DisplayName("Validation Utils")
class ValidationUtilsTest {

    @Test
    @DisplayName("validateEmail should return true for valid emails")
    void testValidateEmailValid() {
        assertThat(ValidationUtils.validateEmail("user@example.com")).isTrue();
        assertThat(ValidationUtils.validateEmail("test.user+tag@domain.co.uk")).isTrue();
        assertThat(ValidationUtils.validateEmail("user123@example-domain.org")).isTrue();
        assertThat(ValidationUtils.validateEmail("simple@test.io")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "plainaddress",
        "@example.com",
        "user@",
        "user..name@example.com",
        "user@example",
        "user@.com",
        "user name@example.com",
        "user@exam ple.com"
    })
    @DisplayName("validateEmail should return false for invalid emails")
    void testValidateEmailInvalid(String email) {
        assertThat(ValidationUtils.validateEmail(email)).isFalse();
    }

    @Test
    @DisplayName("validateEmail should return false for null email")
    void testValidateEmailNull() {
        assertThat(ValidationUtils.validateEmail(null)).isFalse();
    }

    @Test
    @DisplayName("validateTimezone should return true for valid timezones")
    void testValidateTimezoneValid() {
        assertThat(ValidationUtils.validateTimezone("UTC")).isTrue();
        assertThat(ValidationUtils.validateTimezone("America/New_York")).isTrue();
        assertThat(ValidationUtils.validateTimezone("Europe/London")).isTrue();
        assertThat(ValidationUtils.validateTimezone("Asia/Tokyo")).isTrue();
        assertThat(ValidationUtils.validateTimezone("GMT")).isTrue();
        assertThat(ValidationUtils.validateTimezone("GMT+5")).isTrue();
        assertThat(ValidationUtils.validateTimezone("GMT-8")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "Invalid/Zone",
        "NOT_A_TIMEZONE",
        "America/NonExistent",
        "GMT+25",
        "UTC+25"
    })
    @DisplayName("validateTimezone should return false for invalid timezones")
    void testValidateTimezoneInvalid(String timezone) {
        assertThat(ValidationUtils.validateTimezone(timezone)).isFalse();
    }

    @Test
    @DisplayName("validateTimezone should return false for null timezone")
    void testValidateTimezoneNull() {
        assertThat(ValidationUtils.validateTimezone(null)).isFalse();
    }

    @Test
    @DisplayName("validateDateRange should return true for valid date ranges")
    void testValidateDateRangeValid() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        assertThat(ValidationUtils.validateDateRange(startDate, endDate)).isTrue();

        // Same date should be valid
        assertThat(ValidationUtils.validateDateRange(startDate, startDate)).isTrue();

        // Future dates should be valid
        LocalDate futureStart = LocalDate.now().plusDays(10);
        LocalDate futureEnd = LocalDate.now().plusDays(20);
        assertThat(ValidationUtils.validateDateRange(futureStart, futureEnd)).isTrue();
    }

    @Test
    @DisplayName("validateDateRange should return false when end date is before start date")
    void testValidateDateRangeInvalid() {
        LocalDate startDate = LocalDate.of(2025, 12, 31);
        LocalDate endDate = LocalDate.of(2025, 1, 1);

        assertThat(ValidationUtils.validateDateRange(startDate, endDate)).isFalse();
    }

    @Test
    @DisplayName("validateDateRange should return false for null dates")
    void testValidateDateRangeNull() {
        LocalDate validDate = LocalDate.now();

        assertThat(ValidationUtils.validateDateRange(null, validDate)).isFalse();
        assertThat(ValidationUtils.validateDateRange(validDate, null)).isFalse();
        assertThat(ValidationUtils.validateDateRange(null, null)).isFalse();
    }

    @Test
    @DisplayName("validateCompatibilityScore should return true for valid scores")
    void testValidateCompatibilityScoreValid() {
        assertThat(ValidationUtils.validateCompatibilityScore(0.0)).isTrue();
        assertThat(ValidationUtils.validateCompatibilityScore(0.5)).isTrue();
        assertThat(ValidationUtils.validateCompatibilityScore(1.0)).isTrue();
        assertThat(ValidationUtils.validateCompatibilityScore(0.75)).isTrue();
        assertThat(ValidationUtils.validateCompatibilityScore(0.001)).isTrue();
        assertThat(ValidationUtils.validateCompatibilityScore(0.999)).isTrue();
    }

    @Test
    @DisplayName("validateCompatibilityScore should return false for invalid scores")
    void testValidateCompatibilityScoreInvalid() {
        assertThat(ValidationUtils.validateCompatibilityScore(-0.1)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(1.1)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(-1.0)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(2.0)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(Double.NEGATIVE_INFINITY)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(ValidationUtils.validateCompatibilityScore(Double.NaN)).isFalse();
    }

    @Test
    @DisplayName("validateCompatibilityScore should return false for null score")
    void testValidateCompatibilityScoreNull() {
        assertThat(ValidationUtils.validateCompatibilityScore(null)).isFalse();
    }

    @Test
    @DisplayName("validatePartnershipDuration should return true for valid durations")
    void testValidatePartnershipDurationValid() {
        assertThat(ValidationUtils.validatePartnershipDuration(7)).isTrue();   // Minimum 7 days
        assertThat(ValidationUtils.validatePartnershipDuration(30)).isTrue();  // Default 30 days
        assertThat(ValidationUtils.validatePartnershipDuration(90)).isTrue();  // 3 months
        assertThat(ValidationUtils.validatePartnershipDuration(365)).isTrue(); // Maximum 1 year
        assertThat(ValidationUtils.validatePartnershipDuration(180)).isTrue(); // 6 months
    }

    @Test
    @DisplayName("validatePartnershipDuration should return false for invalid durations")
    void testValidatePartnershipDurationInvalid() {
        assertThat(ValidationUtils.validatePartnershipDuration(0)).isFalse();    // Too short
        assertThat(ValidationUtils.validatePartnershipDuration(6)).isFalse();    // Below minimum
        assertThat(ValidationUtils.validatePartnershipDuration(-5)).isFalse();   // Negative
        assertThat(ValidationUtils.validatePartnershipDuration(366)).isFalse();  // Above maximum
        assertThat(ValidationUtils.validatePartnershipDuration(1000)).isFalse(); // Way too long
    }

    @Test
    @DisplayName("validatePartnershipDuration should return false for null duration")
    void testValidatePartnershipDurationNull() {
        assertThat(ValidationUtils.validatePartnershipDuration(null)).isFalse();
    }

    @Test
    @DisplayName("validateUserId should return true for valid user IDs")
    void testValidateUserIdValid() {
        assertThat(ValidationUtils.validateUserId("user123")).isTrue();
        assertThat(ValidationUtils.validateUserId("abc-123-def")).isTrue();
        assertThat(ValidationUtils.validateUserId("user_123")).isTrue();
        assertThat(ValidationUtils.validateUserId("123456789")).isTrue();
        assertThat(ValidationUtils.validateUserId("a")).isTrue(); // Single character
    }

    @Test
    @DisplayName("validateUserId should return false for invalid user IDs")
    void testValidateUserIdInvalid() {
        assertThat(ValidationUtils.validateUserId("")).isFalse();     // Empty
        assertThat(ValidationUtils.validateUserId("   ")).isFalse(); // Whitespace only
        assertThat(ValidationUtils.validateUserId("user@123")).isFalse(); // Special chars
        assertThat(ValidationUtils.validateUserId("user 123")).isFalse(); // Contains space
        assertThat(ValidationUtils.validateUserId("user#123")).isFalse(); // Hash symbol
    }

    @Test
    @DisplayName("validateUserId should return false for null user ID")
    void testValidateUserIdNull() {
        assertThat(ValidationUtils.validateUserId(null)).isFalse();
    }

    @Test
    @DisplayName("validateGoalTitle should return true for valid goal titles")
    void testValidateGoalTitleValid() {
        assertThat(ValidationUtils.validateGoalTitle("Complete online course")).isTrue();
        assertThat(ValidationUtils.validateGoalTitle("Read 10 books")).isTrue();
        assertThat(ValidationUtils.validateGoalTitle("A")).isTrue(); // Minimum length

        // Max length test (255 chars)
        String maxLengthTitle = "A".repeat(255);
        assertThat(ValidationUtils.validateGoalTitle(maxLengthTitle)).isTrue();
    }

    @Test
    @DisplayName("validateGoalTitle should return false for invalid goal titles")
    void testValidateGoalTitleInvalid() {
        assertThat(ValidationUtils.validateGoalTitle("")).isFalse();     // Empty
        assertThat(ValidationUtils.validateGoalTitle("   ")).isFalse(); // Whitespace only

        // Too long (256 chars)
        String tooLongTitle = "A".repeat(256);
        assertThat(ValidationUtils.validateGoalTitle(tooLongTitle)).isFalse();
    }

    @Test
    @DisplayName("validateGoalTitle should return false for null goal title")
    void testValidateGoalTitleNull() {
        assertThat(ValidationUtils.validateGoalTitle(null)).isFalse();
    }

    @Test
    @DisplayName("validateMessage should return true for valid messages")
    void testValidateMessageValid() {
        assertThat(ValidationUtils.validateMessage("Hello!")).isTrue();
        assertThat(ValidationUtils.validateMessage("Great job on your progress today!")).isTrue();

        // Max length test (1000 chars)
        String maxLengthMessage = "A".repeat(1000);
        assertThat(ValidationUtils.validateMessage(maxLengthMessage)).isTrue();
    }

    @Test
    @DisplayName("validateMessage should return false for invalid messages")
    void testValidateMessageInvalid() {
        assertThat(ValidationUtils.validateMessage("")).isFalse();     // Empty
        assertThat(ValidationUtils.validateMessage("   ")).isFalse(); // Whitespace only

        // Too long (1001 chars)
        String tooLongMessage = "A".repeat(1001);
        assertThat(ValidationUtils.validateMessage(tooLongMessage)).isFalse();
    }

    @Test
    @DisplayName("validateMessage should return false for null message")
    void testValidateMessageNull() {
        assertThat(ValidationUtils.validateMessage(null)).isFalse();
    }

    @Test
    @DisplayName("isNotBlank should work correctly")
    void testIsNotBlank() {
        // Valid strings
        assertThat(ValidationUtils.isNotBlank("hello")).isTrue();
        assertThat(ValidationUtils.isNotBlank("  hello  ")).isTrue();
        assertThat(ValidationUtils.isNotBlank("a")).isTrue();

        // Invalid strings
        assertThat(ValidationUtils.isNotBlank(null)).isFalse();
        assertThat(ValidationUtils.isNotBlank("")).isFalse();
        assertThat(ValidationUtils.isNotBlank("   ")).isFalse();
        assertThat(ValidationUtils.isNotBlank("\t\n")).isFalse();
    }

    @Test
    @DisplayName("isInRange should work correctly for different numeric types")
    void testIsInRange() {
        // Double range tests
        assertThat(ValidationUtils.isInRange(0.5, 0.0, 1.0)).isTrue();
        assertThat(ValidationUtils.isInRange(0.0, 0.0, 1.0)).isTrue(); // Boundary
        assertThat(ValidationUtils.isInRange(1.0, 0.0, 1.0)).isTrue(); // Boundary
        assertThat(ValidationUtils.isInRange(-0.1, 0.0, 1.0)).isFalse();
        assertThat(ValidationUtils.isInRange(1.1, 0.0, 1.0)).isFalse();

        // Integer range tests
        assertThat(ValidationUtils.isInRange(15, 7, 365)).isTrue();
        assertThat(ValidationUtils.isInRange(7, 7, 365)).isTrue();   // Boundary
        assertThat(ValidationUtils.isInRange(365, 7, 365)).isTrue(); // Boundary
        assertThat(ValidationUtils.isInRange(6, 7, 365)).isFalse();
        assertThat(ValidationUtils.isInRange(366, 7, 365)).isFalse();
    }

    @Test
    @DisplayName("isInRange should handle null values correctly")
    void testIsInRangeNull() {
        assertThat(ValidationUtils.isInRange(null, 0.0, 1.0)).isFalse();
        assertThat(ValidationUtils.isInRange(null, 7, 365)).isFalse();
    }
}