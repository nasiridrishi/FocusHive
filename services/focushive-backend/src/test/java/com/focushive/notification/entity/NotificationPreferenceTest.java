package com.focushive.notification.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferenceTest {

    private Validator validator;
    private NotificationPreference notificationPreference;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        notificationPreference = new NotificationPreference();
        notificationPreference.setUserId("test-user-id");
        notificationPreference.setNotificationType(NotificationType.HIVE_INVITATION);
        notificationPreference.setInAppEnabled(true);
        notificationPreference.setEmailEnabled(true);
        notificationPreference.setPushEnabled(false);
        notificationPreference.setQuietStartTime(LocalTime.of(22, 0));
        notificationPreference.setQuietEndTime(LocalTime.of(8, 0));
        notificationPreference.setFrequency(NotificationFrequency.IMMEDIATE);
    }

    @Test
    void validNotificationPreference_shouldPassValidation() {
        // When
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void notificationPreference_withNullUserId_shouldFailValidation() {
        // Given
        notificationPreference.setUserId(null);

        // When
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User ID is required");
    }

    @Test
    void notificationPreference_withBlankUserId_shouldFailValidation() {
        // Given
        notificationPreference.setUserId("");

        // When
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("User ID is required");
    }

    @Test
    void notificationPreference_withNullNotificationType_shouldFailValidation() {
        // Given
        notificationPreference.setNotificationType(null);

        // When
        Set<ConstraintViolation<NotificationPreference>> violations = validator.validate(notificationPreference);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Notification type is required");
    }

    @Test
    void notificationPreference_withDefaultValues_shouldHaveCorrectDefaults() {
        // Given
        NotificationPreference preference = new NotificationPreference();

        // When & Then
        assertThat(preference.getInAppEnabled()).isTrue();
        assertThat(preference.getEmailEnabled()).isTrue();
        assertThat(preference.getPushEnabled()).isTrue();
        assertThat(preference.getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
    }

    @Test
    void notificationPreference_shouldHaveCorrectGettersAndSetters() {
        // Given
        String userId = "user-123";
        NotificationType type = NotificationType.TASK_ASSIGNED;
        LocalTime startTime = LocalTime.of(23, 30);
        LocalTime endTime = LocalTime.of(7, 30);
        NotificationFrequency frequency = NotificationFrequency.DAILY_DIGEST;

        // When
        notificationPreference.setUserId(userId);
        notificationPreference.setNotificationType(type);
        notificationPreference.setInAppEnabled(false);
        notificationPreference.setEmailEnabled(false);
        notificationPreference.setPushEnabled(true);
        notificationPreference.setQuietStartTime(startTime);
        notificationPreference.setQuietEndTime(endTime);
        notificationPreference.setFrequency(frequency);

        // Then
        assertThat(notificationPreference.getUserId()).isEqualTo(userId);
        assertThat(notificationPreference.getNotificationType()).isEqualTo(type);
        assertThat(notificationPreference.getInAppEnabled()).isFalse();
        assertThat(notificationPreference.getEmailEnabled()).isFalse();
        assertThat(notificationPreference.getPushEnabled()).isTrue();
        assertThat(notificationPreference.getQuietStartTime()).isEqualTo(startTime);
        assertThat(notificationPreference.getQuietEndTime()).isEqualTo(endTime);
        assertThat(notificationPreference.getFrequency()).isEqualTo(frequency);
    }

    @Test
    void isInQuietHours_duringQuietHours_shouldReturnTrue() {
        // Given
        notificationPreference.setQuietStartTime(LocalTime.of(22, 0));
        notificationPreference.setQuietEndTime(LocalTime.of(8, 0));
        LocalTime currentTime = LocalTime.of(23, 30); // During quiet hours

        // When
        boolean result = notificationPreference.isInQuietHours(currentTime);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isInQuietHours_outsideQuietHours_shouldReturnFalse() {
        // Given
        notificationPreference.setQuietStartTime(LocalTime.of(22, 0));
        notificationPreference.setQuietEndTime(LocalTime.of(8, 0));
        LocalTime currentTime = LocalTime.of(10, 30); // Outside quiet hours

        // When
        boolean result = notificationPreference.isInQuietHours(currentTime);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isInQuietHours_withNullQuietTimes_shouldReturnFalse() {
        // Given
        notificationPreference.setQuietStartTime(null);
        notificationPreference.setQuietEndTime(null);
        LocalTime currentTime = LocalTime.of(23, 30);

        // When
        boolean result = notificationPreference.isInQuietHours(currentTime);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isInQuietHours_acrossMidnight_shouldHandleCorrectly() {
        // Given - quiet hours from 22:00 to 08:00 (crosses midnight)
        notificationPreference.setQuietStartTime(LocalTime.of(22, 0));
        notificationPreference.setQuietEndTime(LocalTime.of(8, 0));

        // When & Then
        assertThat(notificationPreference.isInQuietHours(LocalTime.of(23, 0))).isTrue(); // Before midnight
        assertThat(notificationPreference.isInQuietHours(LocalTime.of(1, 0))).isTrue();  // After midnight
        assertThat(notificationPreference.isInQuietHours(LocalTime.of(7, 30))).isTrue(); // Early morning
        assertThat(notificationPreference.isInQuietHours(LocalTime.of(10, 0))).isFalse(); // Day time
        assertThat(notificationPreference.isInQuietHours(LocalTime.of(15, 0))).isFalse(); // Afternoon
    }
}