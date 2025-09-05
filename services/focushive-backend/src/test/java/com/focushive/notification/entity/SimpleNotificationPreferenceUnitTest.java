package com.focushive.notification.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple unit test for NotificationPreference entity without Spring context.
 * This tests the entity logic without requiring database or Spring configuration.
 */
class SimpleNotificationPreferenceUnitTest {

    @Test
    void createNotificationPreference_shouldBuildCorrectly() {
        // Given
        String userId = "test-user-123";
        NotificationType type = NotificationType.HIVE_INVITATION;
        LocalTime quietStart = LocalTime.of(22, 0);
        LocalTime quietEnd = LocalTime.of(8, 0);
        NotificationFrequency frequency = NotificationFrequency.IMMEDIATE;

        // When
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .notificationType(type)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .quietStartTime(quietStart)
                .quietEndTime(quietEnd)
                .frequency(frequency)
                .build();

        // Then
        assertThat(preference.getUserId()).isEqualTo(userId);
        assertThat(preference.getNotificationType()).isEqualTo(type);
        assertThat(preference.getInAppEnabled()).isTrue();
        assertThat(preference.getEmailEnabled()).isTrue();
        assertThat(preference.getPushEnabled()).isFalse();
        assertThat(preference.getQuietStartTime()).isEqualTo(quietStart);
        assertThat(preference.getQuietEndTime()).isEqualTo(quietEnd);
        assertThat(preference.getFrequency()).isEqualTo(frequency);
    }

    @Test
    void updatePreference_shouldModifyCorrectly() {
        // Given
        NotificationPreference preference = NotificationPreference.builder()
                .userId("user-123")
                .notificationType(NotificationType.HIVE_INVITATION)
                .inAppEnabled(false)
                .emailEnabled(false)
                .pushEnabled(false)
                .frequency(NotificationFrequency.OFF)
                .build();

        // When
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(true);
        preference.setFrequency(NotificationFrequency.IMMEDIATE);

        // Then
        assertThat(preference.getInAppEnabled()).isTrue();
        assertThat(preference.getEmailEnabled()).isTrue();
        assertThat(preference.getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
        assertThat(preference.getPushEnabled()).isFalse(); // Should remain unchanged
    }

    @Test
    void isAnyDeliveryMethodEnabled_shouldReturnTrueWhenAtLeastOneEnabled() {
        // Given
        NotificationPreference preferenceWithInApp = NotificationPreference.builder()
                .inAppEnabled(true)
                .emailEnabled(false)
                .pushEnabled(false)
                .build();
        
        NotificationPreference preferenceWithEmail = NotificationPreference.builder()
                .inAppEnabled(false)
                .emailEnabled(true)
                .pushEnabled(false)
                .build();
        
        NotificationPreference preferenceAllDisabled = NotificationPreference.builder()
                .inAppEnabled(false)
                .emailEnabled(false)
                .pushEnabled(false)
                .build();

        // Then
        assertThat(preferenceWithInApp.getInAppEnabled() || preferenceWithInApp.getEmailEnabled() || preferenceWithInApp.getPushEnabled()).isTrue();
        assertThat(preferenceWithEmail.getInAppEnabled() || preferenceWithEmail.getEmailEnabled() || preferenceWithEmail.getPushEnabled()).isTrue();
        assertThat(preferenceAllDisabled.getInAppEnabled() || preferenceAllDisabled.getEmailEnabled() || preferenceAllDisabled.getPushEnabled()).isFalse();
    }

    @Test
    void frequencyEnum_shouldHaveAllExpectedValues() {
        // When & Then
        assertThat(NotificationFrequency.values()).containsExactlyInAnyOrder(
            NotificationFrequency.OFF,
            NotificationFrequency.IMMEDIATE,
            NotificationFrequency.DAILY_DIGEST,
            NotificationFrequency.WEEKLY_DIGEST
        );
    }

    @Test
    void notificationTypeEnum_shouldHaveAllExpectedValues() {
        // When & Then
        assertThat(NotificationType.values()).contains(
            NotificationType.HIVE_INVITATION,
            NotificationType.TASK_ASSIGNED,
            NotificationType.BUDDY_REQUEST,
            NotificationType.BUDDY_SESSION_COMPLETED,
            NotificationType.FORUM_REPLY,
            NotificationType.SYSTEM_ANNOUNCEMENT
        );
    }
}