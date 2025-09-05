package com.focushive.notification.repository;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple repository test that focuses only on JPA functionality.
 * This test uses @DataJpaTest slice test to avoid WebSocket configuration issues.
 */
@DataJpaTest
@ActiveProfiles("test") 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
class SimpleNotificationPreferenceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private String testUserId;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
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
        
        testPreference = entityManager.persistAndFlush(testPreference);
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
}