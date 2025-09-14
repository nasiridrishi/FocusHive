package com.focushive.notification.integration;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for notification preferences functionality.
 * Tests user preference storage, retrieval, channel selection, and business logic.
 * 
 * Test scenarios:
 * 1. User preference storage and retrieval
 * 2. Channel selection (email, in-app, push, SMS)
 * 3. Notification frequency settings
 * 4. Do-not-disturb periods and quiet hours
 * 5. Unsubscribe functionality
 * 6. Preference inheritance and defaults
 * 7. Bulk preference updates
 * 8. Preference validation and constraints
 * 9. Cross-user preference isolation
 * 10. Preference migration and versioning
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("Notification Preferences Integration Tests")
class NotificationPreferencesIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_USER_ID = "preference-test-user";
    private static final String ANOTHER_USER_ID = "another-preference-user";

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should store and retrieve user notification preferences")
    void shouldStoreAndRetrieveUserNotificationPreferences() {
        // Given - TDD: Create custom preferences for a user
        NotificationPreference emailPreference = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.WELCOME)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(22, 0))
                .quietEndTime(LocalTime.of(7, 0))
                .build();

        NotificationPreference hivePreference = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.HIVE_INVITATION)
                .inAppEnabled(true)
                .emailEnabled(false)
                .pushEnabled(true)
                .frequency(NotificationFrequency.HOURLY)
                .quietStartTime(LocalTime.of(23, 30))
                .quietEndTime(LocalTime.of(6, 30))
                .build();

        // When - TDD: Save preferences
        notificationPreferenceRepository.save(emailPreference);
        notificationPreferenceRepository.save(hivePreference);

        // Then - TDD: Verify preferences can be retrieved
        Optional<NotificationPreference> retrievedEmailPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.WELCOME);
        
        assertThat(retrievedEmailPref).isPresent();
        assertThat(retrievedEmailPref.get().getEmailEnabled()).isTrue();
        assertThat(retrievedEmailPref.get().getPushEnabled()).isFalse();
        assertThat(retrievedEmailPref.get().getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
        assertThat(retrievedEmailPref.get().getQuietStartTime()).isEqualTo(LocalTime.of(22, 0));

        Optional<NotificationPreference> retrievedHivePref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.HIVE_INVITATION);
        
        assertThat(retrievedHivePref).isPresent();
        assertThat(retrievedHivePref.get().getEmailEnabled()).isFalse();
        assertThat(retrievedHivePref.get().getPushEnabled()).isTrue();
        assertThat(retrievedHivePref.get().getFrequency()).isEqualTo(NotificationFrequency.HOURLY);
    }

    @Test
    @DisplayName("Should handle different notification channel selections")
    void shouldHandleDifferentNotificationChannelSelections() {
        // Given - Different channel combinations
        NotificationPreference allChannelsEnabled = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.ACHIEVEMENT_UNLOCKED, true, true, NotificationFrequency.IMMEDIATE);
        allChannelsEnabled.setPushEnabled(true);

        NotificationPreference emailOnlyEnabled = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.PASSWORD_RESET, true, false, NotificationFrequency.IMMEDIATE);
        emailOnlyEnabled.setPushEnabled(false);

        NotificationPreference inAppOnlyEnabled = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.SYSTEM_NOTIFICATION, false, true, NotificationFrequency.IMMEDIATE);
        inAppOnlyEnabled.setPushEnabled(false);

        NotificationPreference allChannelsDisabled = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.BUDDY_REQUEST, false, false, NotificationFrequency.OFF);
        allChannelsDisabled.setPushEnabled(false);

        // When
        notificationPreferenceRepository.save(allChannelsEnabled);
        notificationPreferenceRepository.save(emailOnlyEnabled);
        notificationPreferenceRepository.save(inAppOnlyEnabled);
        notificationPreferenceRepository.save(allChannelsDisabled);

        // Then - Verify channel logic
        assertThat(allChannelsEnabled.hasEnabledChannels()).isTrue();
        assertThat(allChannelsEnabled.isNotificationEnabled()).isTrue();

        assertThat(emailOnlyEnabled.hasEnabledChannels()).isTrue();
        assertThat(emailOnlyEnabled.getEmailEnabled()).isTrue();
        assertThat(emailOnlyEnabled.getInAppEnabled()).isFalse();
        assertThat(emailOnlyEnabled.getPushEnabled()).isFalse();

        assertThat(inAppOnlyEnabled.hasEnabledChannels()).isTrue();
        assertThat(inAppOnlyEnabled.getInAppEnabled()).isTrue();
        assertThat(inAppOnlyEnabled.getEmailEnabled()).isFalse();

        assertThat(allChannelsDisabled.hasEnabledChannels()).isFalse();
        assertThat(allChannelsDisabled.isNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should handle notification frequency settings correctly")
    void shouldHandleNotificationFrequencySettingsCorrectly() {
        // Given - Different frequency settings
        NotificationPreference immediateNotifications = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.HIVE_ACTIVITY, true, true, NotificationFrequency.IMMEDIATE);

        NotificationPreference hourlyDigest = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.CHAT_MESSAGE, true, true, NotificationFrequency.HOURLY);

        NotificationPreference dailyDigest = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.FORUM_REPLY, true, true, NotificationFrequency.DAILY);

        NotificationPreference weeklyDigest = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.SYSTEM_UPDATE, true, true, NotificationFrequency.WEEKLY);

        NotificationPreference disabledNotifications = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.MARKETING, false, false, NotificationFrequency.OFF);

        // When
        notificationPreferenceRepository.save(immediateNotifications);
        notificationPreferenceRepository.save(hourlyDigest);
        notificationPreferenceRepository.save(dailyDigest);
        notificationPreferenceRepository.save(weeklyDigest);
        notificationPreferenceRepository.save(disabledNotifications);

        // Then - Verify frequency logic
        assertThat(immediateNotifications.getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
        assertThat(immediateNotifications.isNotificationEnabled()).isTrue();

        assertThat(hourlyDigest.getFrequency()).isEqualTo(NotificationFrequency.HOURLY);
        assertThat(hourlyDigest.isNotificationEnabled()).isTrue();

        assertThat(dailyDigest.getFrequency()).isEqualTo(NotificationFrequency.DAILY);
        assertThat(weeklyDigest.getFrequency()).isEqualTo(NotificationFrequency.WEEKLY);

        assertThat(disabledNotifications.getFrequency()).isEqualTo(NotificationFrequency.OFF);
        assertThat(disabledNotifications.isNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should handle do-not-disturb periods and quiet hours")
    void shouldHandleDoNotDisturbPeriodsAndQuietHours() {
        // Given - Different quiet hour configurations
        NotificationPreference nightQuietHours = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.CHAT_MESSAGE)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(true)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(22, 0))  // 10 PM
                .quietEndTime(LocalTime.of(8, 0))     // 8 AM
                .build();

        NotificationPreference lunchQuietHours = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.HIVE_ACTIVITY)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(12, 0))  // 12 PM
                .quietEndTime(LocalTime.of(13, 0))    // 1 PM
                .build();

        NotificationPreference noQuietHours = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.ACHIEVEMENT_UNLOCKED)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(true)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(null)
                .quietEndTime(null)
                .build();

        notificationPreferenceRepository.save(nightQuietHours);
        notificationPreferenceRepository.save(lunchQuietHours);
        notificationPreferenceRepository.save(noQuietHours);

        // When & Then - Test quiet hours logic
        // Night quiet hours (crosses midnight)
        assertThat(nightQuietHours.isInQuietHours(LocalTime.of(23, 30))).isTrue();  // 11:30 PM
        assertThat(nightQuietHours.isInQuietHours(LocalTime.of(2, 0))).isTrue();    // 2 AM
        assertThat(nightQuietHours.isInQuietHours(LocalTime.of(7, 30))).isTrue();   // 7:30 AM
        assertThat(nightQuietHours.isInQuietHours(LocalTime.of(8, 30))).isFalse();  // 8:30 AM
        assertThat(nightQuietHours.isInQuietHours(LocalTime.of(15, 0))).isFalse();  // 3 PM

        // Lunch quiet hours (same day)
        assertThat(lunchQuietHours.isInQuietHours(LocalTime.of(12, 30))).isTrue();  // 12:30 PM
        assertThat(lunchQuietHours.isInQuietHours(LocalTime.of(11, 30))).isFalse(); // 11:30 AM
        assertThat(lunchQuietHours.isInQuietHours(LocalTime.of(13, 30))).isFalse(); // 1:30 PM

        // No quiet hours
        assertThat(noQuietHours.isInQuietHours(LocalTime.of(1, 0))).isFalse();
        assertThat(noQuietHours.isInQuietHours(LocalTime.of(12, 0))).isFalse();
        assertThat(noQuietHours.isInQuietHours(LocalTime.of(23, 0))).isFalse();
    }

    @Test
    @DisplayName("Should implement unsubscribe functionality")
    void shouldImplementUnsubscribeFunctionality() {
        // Given - User with various notification preferences
        for (NotificationType type : NotificationType.values()) {
            NotificationPreference preference = createTestNotificationPreference(
                    TEST_USER_ID, type, true, true, NotificationFrequency.IMMEDIATE);
            notificationPreferenceRepository.save(preference);
        }

        // When - User unsubscribes from specific notification type
        Optional<NotificationPreference> emailPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.MARKETING);
        
        assertThat(emailPref).isPresent();
        emailPref.get().setEmailEnabled(false);
        emailPref.get().setInAppEnabled(false);
        emailPref.get().setPushEnabled(false);
        emailPref.get().setFrequency(NotificationFrequency.OFF);
        notificationPreferenceRepository.save(emailPref.get());

        // Then - Verify unsubscribe
        Optional<NotificationPreference> updatedPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.MARKETING);
        
        assertThat(updatedPref).isPresent();
        assertThat(updatedPref.get().isNotificationEnabled()).isFalse();
        assertThat(updatedPref.get().hasEnabledChannels()).isFalse();

        // Verify other preferences are still enabled
        Optional<NotificationPreference> achievementPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.ACHIEVEMENT_UNLOCKED);
        
        assertThat(achievementPref).isPresent();
        assertThat(achievementPref.get().isNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should handle preference inheritance and defaults")
    void shouldHandlePreferenceInheritanceAndDefaults() {
        // Given - User without explicit preferences for some notification types
        String newUserId = "new-user-without-preferences";

        // When - Query for non-existent preference
        Optional<NotificationPreference> nonExistentPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(newUserId, NotificationType.WELCOME);

        // Then - Should not exist (no default creation in repository layer)
        assertThat(nonExistentPref).isEmpty();

        // Given - Create preference with default values
        NotificationPreference defaultPreference = NotificationPreference.builder()
                .userId(newUserId)
                .notificationType(NotificationType.WELCOME)
                .build(); // Uses @Builder.Default values

        notificationPreferenceRepository.save(defaultPreference);

        // When - Retrieve saved preference
        Optional<NotificationPreference> savedPref = notificationPreferenceRepository
                .findByUserIdAndNotificationType(newUserId, NotificationType.WELCOME);

        // Then - Should have default values
        assertThat(savedPref).isPresent();
        assertThat(savedPref.get().getInAppEnabled()).isTrue();     // Default from @Builder.Default
        assertThat(savedPref.get().getEmailEnabled()).isTrue();    // Default from @Builder.Default
        assertThat(savedPref.get().getPushEnabled()).isTrue();     // Default from @Builder.Default (entity shows true)
        assertThat(savedPref.get().getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
    }

    @Test
    @DisplayName("Should handle bulk preference updates efficiently")
    void shouldHandleBulkPreferenceUpdatesEfficiently() {
        // Given - Multiple preferences for the same user
        String bulkUserId = "bulk-update-user";
        
        for (NotificationType type : NotificationType.values()) {
            NotificationPreference preference = createTestNotificationPreference(
                    bulkUserId, type, true, true, NotificationFrequency.IMMEDIATE);
            notificationPreferenceRepository.save(preference);
        }

        // When - Bulk update to disable email for all notification types
        List<NotificationPreference> userPreferences = notificationPreferenceRepository
                .findByUserId(bulkUserId);
        
        assertThat(userPreferences).hasSize(NotificationType.values().length);

        userPreferences.forEach(pref -> {
            pref.setEmailEnabled(false);
            pref.setFrequency(NotificationFrequency.DAILY);
        });
        
        notificationPreferenceRepository.saveAll(userPreferences);

        // Then - Verify all preferences were updated
        List<NotificationPreference> updatedPreferences = notificationPreferenceRepository
                .findByUserId(bulkUserId);
        
        assertThat(updatedPreferences).hasSize(NotificationType.values().length);
        assertThat(updatedPreferences).allMatch(pref -> !pref.getEmailEnabled());
        assertThat(updatedPreferences).allMatch(pref -> pref.getFrequency() == NotificationFrequency.DAILY);
        assertThat(updatedPreferences).allMatch(pref -> pref.getInAppEnabled()); // Should remain true
    }

    @Test
    @DisplayName("Should validate preference constraints and business rules")
    void shouldValidatePreferenceConstraintsAndBusinessRules() {
        // Given - Invalid preference configurations
        NotificationPreference invalidQuietHours = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.SYSTEM_NOTIFICATION)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(8, 0))
                .quietEndTime(LocalTime.of(8, 0))  // Same start and end time
                .build();

        // When & Then - Save should succeed (validation at service layer)
        notificationPreferenceRepository.save(invalidQuietHours);
        
        // Business logic validation
        assertThat(invalidQuietHours.isInQuietHours(LocalTime.of(8, 0))).isFalse(); // Same time = not in quiet hours

        // Given - Test edge cases
        NotificationPreference edgeCase = NotificationPreference.builder()
                .userId(TEST_USER_ID)
                .notificationType(NotificationType.BUDDY_MATCHED)
                .inAppEnabled(false)
                .emailEnabled(false)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE) // Immediate but all channels disabled
                .build();

        notificationPreferenceRepository.save(edgeCase);

        // Then - Should handle edge case appropriately
        assertThat(edgeCase.hasEnabledChannels()).isFalse();
        assertThat(edgeCase.isNotificationEnabled()).isFalse(); // No channels = not enabled despite IMMEDIATE frequency
    }

    @Test
    @DisplayName("Should ensure cross-user preference isolation")
    void shouldEnsureCrossUserPreferenceIsolation() {
        // Given - Preferences for multiple users with same notification types
        NotificationPreference user1Pref = createTestNotificationPreference(
                TEST_USER_ID, NotificationType.WELCOME, true, true, NotificationFrequency.IMMEDIATE);
        
        NotificationPreference user2Pref = createTestNotificationPreference(
                ANOTHER_USER_ID, NotificationType.WELCOME, false, false, NotificationFrequency.OFF);

        notificationPreferenceRepository.save(user1Pref);
        notificationPreferenceRepository.save(user2Pref);

        // When - Query preferences for each user
        Optional<NotificationPreference> user1Retrieved = notificationPreferenceRepository
                .findByUserIdAndNotificationType(TEST_USER_ID, NotificationType.WELCOME);
        
        Optional<NotificationPreference> user2Retrieved = notificationPreferenceRepository
                .findByUserIdAndNotificationType(ANOTHER_USER_ID, NotificationType.WELCOME);

        List<NotificationPreference> user1AllPrefs = notificationPreferenceRepository
                .findByUserId(TEST_USER_ID);
        
        List<NotificationPreference> user2AllPrefs = notificationPreferenceRepository
                .findByUserId(ANOTHER_USER_ID);

        // Then - Verify complete isolation
        assertThat(user1Retrieved).isPresent();
        assertThat(user1Retrieved.get().getEmailEnabled()).isTrue();
        assertThat(user1Retrieved.get().isNotificationEnabled()).isTrue();

        assertThat(user2Retrieved).isPresent();
        assertThat(user2Retrieved.get().getEmailEnabled()).isFalse();
        assertThat(user2Retrieved.get().isNotificationEnabled()).isFalse();

        // Verify no cross-contamination
        assertThat(user1AllPrefs).noneMatch(pref -> pref.getUserId().equals(ANOTHER_USER_ID));
        assertThat(user2AllPrefs).noneMatch(pref -> pref.getUserId().equals(TEST_USER_ID));
    }

    @Test
    @DisplayName("Should handle database constraints and unique constraints")
    @Transactional
    @Rollback
    void shouldHandleDatabaseConstraintsAndUniqueConstraints() {
        // Given - Use a unique user ID for this test to avoid conflicts
        String uniqueTestUserId = "constraint-test-user-" + System.currentTimeMillis();
        
        // Clean up any existing data for this specific test (just in case)
        notificationPreferenceRepository.deleteByUserIdAndNotificationType(uniqueTestUserId, NotificationType.HIVE_INVITATION);
        entityManager.flush();
        entityManager.clear();
        
        // Given - First preference for user and notification type
        NotificationPreference originalPreference = createTestNotificationPreference(
                uniqueTestUserId, NotificationType.HIVE_INVITATION, true, true, NotificationFrequency.IMMEDIATE);
        
        NotificationPreference savedOriginal = notificationPreferenceRepository.save(originalPreference);
        entityManager.flush();

        // When - Try to create duplicate preference (same user + notification type) 
        // Use different ID to avoid primary key conflict, but same user+type for unique constraint test
        NotificationPreference duplicatePreference = NotificationPreference.builder()
                .userId(uniqueTestUserId)
                .notificationType(NotificationType.HIVE_INVITATION)
                .emailEnabled(false)
                .inAppEnabled(false)
                .pushEnabled(false)
                .frequency(NotificationFrequency.OFF)
                .build();

        // Then - Should handle unique constraint violation
        assertThatThrownBy(() -> {
            notificationPreferenceRepository.save(duplicatePreference);
            entityManager.flush(); // Force constraint check
        }).isInstanceOf(Exception.class) // Accept any constraint-related exception
         .hasMessageContaining("CONSTRAINT_INDEX_9"); // Verify it's the unique constraint we expect

        // Note: After a constraint violation, the transaction may be in an inconsistent state
        // so we don't try to verify database state - the important thing is that the constraint was enforced
    }

    @Test
    @DisplayName("Should support complex preference queries and filtering")
    void shouldSupportComplexPreferenceQueriesAndFiltering() {
        // Given - Various preferences for testing queries
        String queryTestUser = "query-test-user";
        
        // Create preferences with different configurations
        NotificationPreference emailEnabledPref = createTestNotificationPreference(
                queryTestUser, NotificationType.WELCOME, true, true, NotificationFrequency.IMMEDIATE);
        
        NotificationPreference emailDisabledPref = createTestNotificationPreference(
                queryTestUser, NotificationType.MARKETING, false, true, NotificationFrequency.DAILY);
        
        NotificationPreference immediateFreqPref = createTestNotificationPreference(
                queryTestUser, NotificationType.ACHIEVEMENT_UNLOCKED, true, true, NotificationFrequency.IMMEDIATE);
        
        NotificationPreference weeklyFreqPref = createTestNotificationPreference(
                queryTestUser, NotificationType.SYSTEM_UPDATE, true, true, NotificationFrequency.WEEKLY);

        notificationPreferenceRepository.save(emailEnabledPref);
        notificationPreferenceRepository.save(emailDisabledPref);
        notificationPreferenceRepository.save(immediateFreqPref);
        notificationPreferenceRepository.save(weeklyFreqPref);

        // When & Then - Test various queries
        // Find all preferences for user
        List<NotificationPreference> allUserPrefs = notificationPreferenceRepository
                .findByUserId(queryTestUser);
        assertThat(allUserPrefs).hasSize(4);

        // Find email-enabled preferences (custom query would be needed)
        List<NotificationPreference> emailEnabledPrefs = allUserPrefs.stream()
                .filter(NotificationPreference::getEmailEnabled)
                .toList();
        assertThat(emailEnabledPrefs).hasSize(3);

        // Find immediate frequency preferences
        List<NotificationPreference> immediatePrefs = allUserPrefs.stream()
                .filter(pref -> pref.getFrequency() == NotificationFrequency.IMMEDIATE)
                .toList();
        assertThat(immediatePrefs).hasSize(2);

        // Find preferences with any enabled channel
        List<NotificationPreference> anyChannelEnabled = allUserPrefs.stream()
                .filter(NotificationPreference::hasEnabledChannels)
                .toList();
        assertThat(anyChannelEnabled).hasSize(4); // All have at least in-app enabled
    }
}