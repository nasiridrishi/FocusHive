package com.focushive.notification.repository;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationPreferenceRepositoryTest {

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private String testUserId;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
        notificationPreferenceRepository.deleteAll();
        
        testUserId = "test-user-123";
        testPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.HIVE_INVITATION)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .quietStartTime(LocalTime.of(22, 0))
                .quietEndTime(LocalTime.of(8, 0))
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
        
        notificationPreferenceRepository.save(testPreference);
    }

    @Test
    void findByUserId_shouldReturnUserPreferences() {
        // When
        List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(testUserId);

        // Then
        assertThat(preferences).hasSize(1);
        assertThat(preferences.get(0).getUserId()).isEqualTo(testUserId);
        assertThat(preferences.get(0).getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
    }

    @Test
    void findByUserIdAndNotificationType_shouldReturnSpecificPreference() {
        // When
        Optional<NotificationPreference> preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(testUserId, NotificationType.HIVE_INVITATION);

        // Then
        assertThat(preference).isPresent();
        assertThat(preference.get().getUserId()).isEqualTo(testUserId);
        assertThat(preference.get().getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
    }

    @Test
    void findByUserIdAndNotificationType_withNonExistentType_shouldReturnEmpty() {
        // When
        Optional<NotificationPreference> preference = notificationPreferenceRepository
                .findByUserIdAndNotificationType(testUserId, NotificationType.TASK_ASSIGNED);

        // Then
        assertThat(preference).isEmpty();
    }

    @Test
    void findEnabledPreferencesForUser_shouldReturnOnlyEnabledPreferences() {
        // Given
        NotificationPreference disabledPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(false)
                .emailEnabled(false)
                .pushEnabled(false)
                .frequency(NotificationFrequency.OFF)
                .build();
        notificationPreferenceRepository.save(disabledPreference);

        // When
        List<NotificationPreference> enabledPreferences = notificationPreferenceRepository
                .findEnabledPreferencesForUser(testUserId);

        // Then
        assertThat(enabledPreferences).hasSize(1);
        assertThat(enabledPreferences.get(0).getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
    }

    @Test
    void findByUserIdAndInAppEnabledTrue_shouldReturnInAppEnabledPreferences() {
        // Given
        NotificationPreference noInAppPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(false)
                .emailEnabled(true)
                .pushEnabled(true)
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
        notificationPreferenceRepository.save(noInAppPreference);

        // When
        List<NotificationPreference> inAppEnabled = notificationPreferenceRepository
                .findByUserIdAndInAppEnabledTrue(testUserId);

        // Then
        assertThat(inAppEnabled).hasSize(1);
        assertThat(inAppEnabled.get(0).getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
        assertThat(inAppEnabled.get(0).getInAppEnabled()).isTrue();
    }

    @Test
    void findByUserIdAndEmailEnabledTrue_shouldReturnEmailEnabledPreferences() {
        // Given
        NotificationPreference noEmailPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(true)
                .emailEnabled(false)
                .pushEnabled(true)
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
        notificationPreferenceRepository.save(noEmailPreference);

        // When
        List<NotificationPreference> emailEnabled = notificationPreferenceRepository
                .findByUserIdAndEmailEnabledTrue(testUserId);

        // Then
        assertThat(emailEnabled).hasSize(1);
        assertThat(emailEnabled.get(0).getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
        assertThat(emailEnabled.get(0).getEmailEnabled()).isTrue();
    }

    @Test
    void findByUserIdAndPushEnabledTrue_shouldReturnPushEnabledPreferences() {
        // Given
        testPreference.setPushEnabled(true);
        notificationPreferenceRepository.save(testPreference);

        NotificationPreference noPushPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
        notificationPreferenceRepository.save(noPushPreference);

        // When
        List<NotificationPreference> pushEnabled = notificationPreferenceRepository
                .findByUserIdAndPushEnabledTrue(testUserId);

        // Then
        assertThat(pushEnabled).hasSize(1);
        assertThat(pushEnabled.get(0).getNotificationType()).isEqualTo(NotificationType.HIVE_INVITATION);
        assertThat(pushEnabled.get(0).getPushEnabled()).isTrue();
    }

    @Test
    void findByFrequency_shouldReturnPreferencesWithSpecificFrequency() {
        // Given
        NotificationPreference digestPreference = NotificationPreference.builder()
                .userId("another-user")
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.DAILY_DIGEST)
                .build();
        notificationPreferenceRepository.save(digestPreference);

        // When
        List<NotificationPreference> immediatePreferences = notificationPreferenceRepository
                .findByFrequency(NotificationFrequency.IMMEDIATE);
        List<NotificationPreference> digestPreferences = notificationPreferenceRepository
                .findByFrequency(NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(immediatePreferences).hasSize(1);
        assertThat(immediatePreferences.get(0).getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
        
        assertThat(digestPreferences).hasSize(1);
        assertThat(digestPreferences.get(0).getFrequency()).isEqualTo(NotificationFrequency.DAILY_DIGEST);
    }

    @Test
    void deleteByUserIdAndNotificationType_shouldRemoveSpecificPreference() {
        // Given
        assertThat(notificationPreferenceRepository.findByUserId(testUserId)).hasSize(1);

        // When
        notificationPreferenceRepository.deleteByUserIdAndNotificationType(
                testUserId, NotificationType.HIVE_INVITATION);

        // Then
        assertThat(notificationPreferenceRepository.findByUserId(testUserId)).isEmpty();
    }

    @Test
    void existsByUserIdAndNotificationType_shouldReturnTrueWhenExists() {
        // When
        boolean exists = notificationPreferenceRepository.existsByUserIdAndNotificationType(
                testUserId, NotificationType.HIVE_INVITATION);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndNotificationType_shouldReturnFalseWhenNotExists() {
        // When
        boolean exists = notificationPreferenceRepository.existsByUserIdAndNotificationType(
                testUserId, NotificationType.TASK_ASSIGNED);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void save_shouldCreateNewPreference() {
        // Given
        NotificationPreference newPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.TASK_ASSIGNED)
                .inAppEnabled(true)
                .emailEnabled(false)
                .pushEnabled(true)
                .frequency(NotificationFrequency.DAILY_DIGEST)
                .build();

        // When
        NotificationPreference saved = notificationPreferenceRepository.save(newPreference);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(testUserId);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(saved.getFrequency()).isEqualTo(NotificationFrequency.DAILY_DIGEST);
    }

    @Test
    void save_shouldUpdateExistingPreference() {
        // Given
        testPreference.setFrequency(NotificationFrequency.WEEKLY_DIGEST);
        testPreference.setPushEnabled(true);

        // When
        NotificationPreference updated = notificationPreferenceRepository.save(testPreference);

        // Then
        assertThat(updated.getId()).isEqualTo(testPreference.getId());
        assertThat(updated.getFrequency()).isEqualTo(NotificationFrequency.WEEKLY_DIGEST);
        assertThat(updated.getPushEnabled()).isTrue();
    }
}