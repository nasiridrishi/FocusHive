package com.focushive.buddy.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for buddy service exception classes.
 *
 * Following TDD approach:
 * 1. RED: These tests will initially FAIL because exception classes don't exist
 * 2. GREEN: Implement exception classes to make tests pass
 * 3. REFACTOR: Improve implementation while keeping tests green
 */
@DisplayName("Buddy Service Exceptions")
class BuddyExceptionsTest {

    @Test
    @DisplayName("BuddyServiceException should be base exception")
    void testBuddyServiceException() {
        // Test simple message constructor
        String message = "Buddy service error";
        BuddyServiceException exception = new BuddyServiceException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("BuddyServiceException should support message with cause")
    void testBuddyServiceExceptionWithCause() {
        String message = "Service failure";
        Throwable cause = new RuntimeException("Root cause");

        BuddyServiceException exception = new BuddyServiceException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BuddyNotFoundException should extend BuddyServiceException")
    void testBuddyNotFoundException() {
        String message = "Buddy not found";
        BuddyNotFoundException exception = new BuddyNotFoundException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(BuddyServiceException.class);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("BuddyNotFoundException should support resource-specific constructor")
    void testBuddyNotFoundExceptionWithResourceDetails() {
        String userId = "user123";
        BuddyNotFoundException exception = new BuddyNotFoundException("User", "id", userId);

        assertThat(exception.getMessage()).isEqualTo("User not found with id : 'user123'");
        assertThat(exception).isInstanceOf(BuddyServiceException.class);
    }

    @Test
    @DisplayName("BuddyNotFoundException should support cause")
    void testBuddyNotFoundExceptionWithCause() {
        String message = "Partnership not found";
        Throwable cause = new RuntimeException("Database error");

        BuddyNotFoundException exception = new BuddyNotFoundException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("InvalidBuddyRequestException should extend BuddyServiceException")
    void testInvalidBuddyRequestException() {
        String message = "Invalid buddy request";
        InvalidBuddyRequestException exception = new InvalidBuddyRequestException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(BuddyServiceException.class);
        assertThat(exception.getValidationErrors()).isNotNull();
        assertThat(exception.getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("InvalidBuddyRequestException should support validation errors")
    void testInvalidBuddyRequestExceptionWithErrors() {
        String message = "Validation failed";
        java.util.List<String> errors = java.util.List.of(
            "Email is required",
            "Timezone is invalid"
        );

        InvalidBuddyRequestException exception = new InvalidBuddyRequestException(message, errors);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getValidationErrors()).containsExactly(
            "Email is required",
            "Timezone is invalid"
        );
    }

    @Test
    @DisplayName("PartnershipConflictException should extend BuddyServiceException")
    void testPartnershipConflictException() {
        String message = "Partnership conflict detected";
        PartnershipConflictException exception = new PartnershipConflictException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(BuddyServiceException.class);
        assertThat(exception.getConflictType()).isNull();
    }

    @Test
    @DisplayName("PartnershipConflictException should support conflict type")
    void testPartnershipConflictExceptionWithType() {
        String message = "Duplicate partnership";
        String conflictType = "DUPLICATE_PARTNERSHIP";

        PartnershipConflictException exception = new PartnershipConflictException(message, conflictType);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getConflictType()).isEqualTo(conflictType);
    }

    @Test
    @DisplayName("InsufficientCompatibilityException should extend BuddyServiceException")
    void testInsufficientCompatibilityException() {
        String message = "Compatibility too low";
        InsufficientCompatibilityException exception = new InsufficientCompatibilityException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(BuddyServiceException.class);
        assertThat(exception.getActualScore()).isNull();
        assertThat(exception.getRequiredScore()).isNull();
    }

    @Test
    @DisplayName("InsufficientCompatibilityException should support compatibility scores")
    void testInsufficientCompatibilityExceptionWithScores() {
        String message = "Compatibility below threshold";
        Double actualScore = 0.45;
        Double requiredScore = 0.60;

        InsufficientCompatibilityException exception = new InsufficientCompatibilityException(
            message, actualScore, requiredScore
        );

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getActualScore()).isEqualTo(actualScore);
        assertThat(exception.getRequiredScore()).isEqualTo(requiredScore);
    }

    @Test
    @DisplayName("Exception inheritance hierarchy should be correct")
    void testExceptionHierarchy() {
        BuddyNotFoundException notFound = new BuddyNotFoundException("Not found");
        InvalidBuddyRequestException invalid = new InvalidBuddyRequestException("Invalid");
        PartnershipConflictException conflict = new PartnershipConflictException("Conflict");
        InsufficientCompatibilityException compatibility = new InsufficientCompatibilityException("Low compatibility");

        // All should extend BuddyServiceException
        assertThat(notFound).isInstanceOf(BuddyServiceException.class);
        assertThat(invalid).isInstanceOf(BuddyServiceException.class);
        assertThat(conflict).isInstanceOf(BuddyServiceException.class);
        assertThat(compatibility).isInstanceOf(BuddyServiceException.class);

        // All should be RuntimeExceptions
        assertThat(notFound).isInstanceOf(RuntimeException.class);
        assertThat(invalid).isInstanceOf(RuntimeException.class);
        assertThat(conflict).isInstanceOf(RuntimeException.class);
        assertThat(compatibility).isInstanceOf(RuntimeException.class);
    }
}