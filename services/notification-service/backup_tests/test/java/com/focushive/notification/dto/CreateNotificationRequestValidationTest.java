package com.focushive.notification.dto;

import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.entity.Notification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateNotificationRequest Validation Tests")
class CreateNotificationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation with valid data")
    void shouldPassValidationWithValidData() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid notification title")
                .content("Valid notification content")
                .priority(Notification.NotificationPriority.NORMAL)
                .language("en")
                .forceDelivery(false)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no validation violations");
    }

    @Test
    @DisplayName("Should fail validation when userId is null")
    void shouldFailValidationWhenUserIdIsNull() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(null)
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("User ID is required")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "user@invalid", "user with spaces", "user-with-$pecial"})
    @DisplayName("Should fail validation with invalid userId values")
    void shouldFailValidationWithInvalidUserIds(String invalidUserId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(invalidUserId)
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Should have validation violations for userId: " + invalidUserId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user123", "user_test", "user-test", "User123", "TEST_USER", "a1b2c3"})
    @DisplayName("Should pass validation with valid userId patterns")
    void shouldPassValidationWithValidUserIds(String validUserId) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(validUserId)
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should pass validation for userId: " + validUserId);
    }

    @Test
    @DisplayName("Should fail validation when title is null")
    void shouldFailValidationWhenTitleIsNull() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title(null)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title is required")));
    }

    @Test
    @DisplayName("Should fail validation when title exceeds 200 characters")
    void shouldFailValidationWhenTitleTooLong() {
        String longTitle = "a".repeat(201);
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title(longTitle)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title must not exceed 200 characters")));
    }

    @Test
    @DisplayName("Should fail validation when content exceeds 5000 characters")
    void shouldFailValidationWhenContentTooLong() {
        String longContent = "a".repeat(5001);
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .content(longContent)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Content must not exceed 5000 characters")));
    }

    @Test
    @DisplayName("Should fail validation when title contains XSS content")
    void shouldFailValidationWithXSSInTitle() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("<script>alert('xss')</script>")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("potentially dangerous content")));
    }

    @Test
    @DisplayName("Should pass validation when content contains basic HTML")
    void shouldPassValidationWithBasicHtmlInContent() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .content("This is <b>bold</b> and <i>italic</i> text.")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should pass validation with basic HTML");
    }

    @Test
    @DisplayName("Should fail validation when content contains dangerous HTML")
    void shouldFailValidationWithDangerousHtmlInContent() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .content("Click here: <a href=\"javascript:alert('xss')\">link</a>")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("potentially dangerous content")));
    }

    @Test
    @DisplayName("Should fail validation when type is null")
    void shouldFailValidationWhenTypeIsNull() {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(null)
                .title("Valid title")
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Notification type is required")));
    }

    @Test
    @DisplayName("Should pass validation with valid metadata")
    void shouldPassValidationWithValidMetadata() {
        NotificationMetadata metadata = NotificationMetadata.builder()
                .emailOverride("test@example.com")
                .trackingId("track123")
                .source("web-app")
                .build();

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .metadata(metadata)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should pass validation with valid metadata");
    }

    @Test
    @DisplayName("Should fail validation with invalid email in metadata")
    void shouldFailValidationWithInvalidEmailInMetadata() {
        NotificationMetadata metadata = NotificationMetadata.builder()
                .emailOverride("invalid-email")
                .build();

        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("user123")
                .type(NotificationType.SYSTEM_NOTIFICATION)
                .title("Valid title")
                .metadata(metadata)
                .build();

        Set<ConstraintViolation<CreateNotificationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Invalid email format")));
    }
}