package com.focushive.buddy.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations in the buddy service.
 * All methods are static and the class cannot be instantiated.
 */
@UtilityClass
public class ValidationUtils {

    // Constants for validation rules
    private static final int MIN_PARTNERSHIP_DURATION_DAYS = 7;
    private static final int MAX_PARTNERSHIP_DURATION_DAYS = 365;
    private static final int MAX_GOAL_TITLE_LENGTH = 255;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    // Email validation pattern - RFC 5322 compliant but simplified
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // User ID pattern - alphanumeric, hyphens, underscores only
    private static final Pattern USER_ID_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]+$"
    );

    /**
     * Validates an email address format.
     *
     * @param email the email address to validate
     * @return true if the email format is valid, false otherwise
     */
    public static boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();

        // Additional checks for common invalid cases
        if (trimmedEmail.contains("..") || // Double dots
            trimmedEmail.contains(" ") ||  // Spaces
            trimmedEmail.startsWith(".") || // Leading dot
            trimmedEmail.endsWith(".")) {   // Trailing dot
            return false;
        }

        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }

    /**
     * Validates a timezone string.
     *
     * @param timezone the timezone string to validate
     * @return true if the timezone is valid, false otherwise
     */
    public static boolean validateTimezone(String timezone) {
        if (timezone == null || timezone.trim().isEmpty()) {
            return false;
        }

        try {
            String trimmedTimezone = timezone.trim();
            ZoneId.of(trimmedTimezone);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates that a date range is valid (end date is not before start date).
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return true if the date range is valid, false otherwise
     */
    public static boolean validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }

        return !endDate.isBefore(startDate);
    }

    /**
     * Validates a compatibility score (must be between 0.0 and 1.0 inclusive).
     *
     * @param score the compatibility score to validate
     * @return true if the score is valid, false otherwise
     */
    public static boolean validateCompatibilityScore(Double score) {
        if (score == null) {
            return false;
        }

        // Check for invalid double values
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return false;
        }

        return isInRange(score, 0.0, 1.0);
    }

    /**
     * Validates partnership duration in days.
     *
     * @param durationDays the duration in days
     * @return true if the duration is valid, false otherwise
     */
    public static boolean validatePartnershipDuration(Integer durationDays) {
        if (durationDays == null) {
            return false;
        }

        return isInRange(durationDays, MIN_PARTNERSHIP_DURATION_DAYS, MAX_PARTNERSHIP_DURATION_DAYS);
    }

    /**
     * Validates a user ID format.
     *
     * @param userId the user ID to validate
     * @return true if the user ID format is valid, false otherwise
     */
    public static boolean validateUserId(String userId) {
        if (!isNotBlank(userId)) {
            return false;
        }

        String trimmedUserId = userId.trim();
        return USER_ID_PATTERN.matcher(trimmedUserId).matches();
    }

    /**
     * Validates a goal title.
     *
     * @param title the goal title to validate
     * @return true if the title is valid, false otherwise
     */
    public static boolean validateGoalTitle(String title) {
        if (!isNotBlank(title)) {
            return false;
        }

        String trimmedTitle = title.trim();
        return trimmedTitle.length() <= MAX_GOAL_TITLE_LENGTH;
    }

    /**
     * Validates a message content.
     *
     * @param message the message to validate
     * @return true if the message is valid, false otherwise
     */
    public static boolean validateMessage(String message) {
        if (!isNotBlank(message)) {
            return false;
        }

        String trimmedMessage = message.trim();
        return trimmedMessage.length() <= MAX_MESSAGE_LENGTH;
    }

    /**
     * Checks if a string is not null, not empty, and contains non-whitespace characters.
     *
     * @param str the string to check
     * @return true if the string is not blank, false otherwise
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Checks if a Double value is within the specified range (inclusive).
     *
     * @param value the value to check
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (inclusive)
     * @return true if the value is in range, false otherwise
     */
    public static boolean isInRange(Double value, Double min, Double max) {
        if (value == null || min == null || max == null) {
            return false;
        }

        return value >= min && value <= max;
    }

    /**
     * Checks if an Integer value is within the specified range (inclusive).
     *
     * @param value the value to check
     * @param min   the minimum value (inclusive)
     * @param max   the maximum value (inclusive)
     * @return true if the value is in range, false otherwise
     */
    public static boolean isInRange(Integer value, Integer min, Integer max) {
        if (value == null || min == null || max == null) {
            return false;
        }

        return value >= min && value <= max;
    }
}