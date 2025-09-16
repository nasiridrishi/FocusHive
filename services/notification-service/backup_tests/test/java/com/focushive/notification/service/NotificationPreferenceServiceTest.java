package com.focushive.notification.service;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.exception.ResourceNotFoundException;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test class for NotificationPreferenceService following TDD approach.
 * Tests all CRUD operations, validation logic, and business rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceService Tests")
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private NotificationPreference defaultPreference;
    private static final String USER_ID = "user-123";
    private static final NotificationType NOTIFICATION_TYPE = NotificationType.WELCOME;

    @BeforeEach
    void setUp() {
        defaultPreference = NotificationPreference.builder()
                .userId(USER_ID)
                .notificationType(NOTIFICATION_TYPE)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .quietStartTime(LocalTime.of(22, 0))
                .quietEndTime(LocalTime.of(8, 0))
                .build();
        // Set ID after building since it's normally auto-generated
        defaultPreference.setId("pref-1");
    }

    @Test
    @DisplayName("Should create new notification preference")
    void shouldCreateNewNotificationPreference() {
        // Given
        given(preferenceRepository.existsByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(false);
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willReturn(defaultPreference);

        // When
        NotificationPreference result = preferenceService.createPreference(
                USER_ID, NOTIFICATION_TYPE, true, true, false, 
                NotificationFrequency.IMMEDIATE, LocalTime.of(22, 0), LocalTime.of(8, 0)
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getNotificationType()).isEqualTo(NOTIFICATION_TYPE);
        assertThat(result.getInAppEnabled()).isTrue();
        assertThat(result.getEmailEnabled()).isTrue();
        assertThat(result.getPushEnabled()).isFalse();
        
        verify(preferenceRepository).existsByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate preference")
    void shouldThrowExceptionWhenCreatingDuplicatePreference() {
        // Given
        given(preferenceRepository.existsByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            preferenceService.createPreference(
                USER_ID, NOTIFICATION_TYPE, true, true, false, 
                NotificationFrequency.IMMEDIATE, null, null
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("already exists");
        
        verify(preferenceRepository).existsByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get user preferences by ID")
    void shouldGetUserPreferencesById() {
        // Given
        String preferenceId = "pref-1";
        given(preferenceRepository.findById(preferenceId))
                .willReturn(Optional.of(defaultPreference));

        // When
        NotificationPreference result = preferenceService.getPreferenceById(preferenceId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(preferenceId);
        verify(preferenceRepository).findById(preferenceId);
    }

    @Test
    @DisplayName("Should throw exception when preference not found by ID")
    void shouldThrowExceptionWhenPreferenceNotFoundById() {
        // Given
        String preferenceId = "non-existent";
        given(preferenceRepository.findById(preferenceId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> preferenceService.getPreferenceById(preferenceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification preference not found");
        
        verify(preferenceRepository).findById(preferenceId);
    }

    @Test
    @DisplayName("Should get all preferences for user")
    void shouldGetAllPreferencesForUser() {
        // Given
        List<NotificationPreference> preferences = Arrays.asList(defaultPreference);
        given(preferenceRepository.findByUserId(USER_ID))
                .willReturn(preferences);

        // When
        List<NotificationPreference> result = preferenceService.getUserPreferences(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(USER_ID);
        verify(preferenceRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Should get specific user preference by type")
    void shouldGetSpecificUserPreferenceByType() {
        // Given
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.of(defaultPreference));

        // When
        Optional<NotificationPreference> result = preferenceService.getUserPreference(USER_ID, NOTIFICATION_TYPE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo(NOTIFICATION_TYPE);
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should update existing preference")
    void shouldUpdateExistingPreference() {
        // Given
        given(preferenceRepository.findById("pref-1"))
                .willReturn(Optional.of(defaultPreference));
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        NotificationPreference result = preferenceService.updatePreference(
                "pref-1", false, true, true, NotificationFrequency.DAILY, null, null
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInAppEnabled()).isFalse();
        assertThat(result.getEmailEnabled()).isTrue();
        assertThat(result.getPushEnabled()).isTrue();
        assertThat(result.getFrequency()).isEqualTo(NotificationFrequency.DAILY);
        
        verify(preferenceRepository).findById("pref-1");
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Should create or update preference")
    void shouldCreateOrUpdatePreference() {
        // Given - preference doesn't exist
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.empty());
        given(preferenceRepository.save(any(NotificationPreference.class)))
                .willReturn(defaultPreference);

        // When
        NotificationPreference result = preferenceService.createOrUpdatePreference(
                USER_ID, NOTIFICATION_TYPE, true, false, true, 
                NotificationFrequency.HOURLY, LocalTime.of(23, 0), LocalTime.of(7, 0)
        );

        // Then
        assertThat(result).isNotNull();
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Should delete preference by ID")
    void shouldDeletePreferenceById() {
        // Given
        given(preferenceRepository.findById("pref-1")).willReturn(Optional.of(defaultPreference));

        // When
        preferenceService.deletePreference("pref-1");

        // Then
        verify(preferenceRepository).findById("pref-1");
        verify(preferenceRepository).deleteById("pref-1");
        verify(securityAuditService).logPreferenceChange(eq("pref-1"), eq(NOTIFICATION_TYPE.name()), any(Map.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent preference")
    void shouldThrowExceptionWhenDeletingNonExistentPreference() {
        // Given
        given(preferenceRepository.findById("non-existent")).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> preferenceService.deletePreference("non-existent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification preference not found");
        
        verify(preferenceRepository).findById("non-existent");
        verify(preferenceRepository, never()).deleteById(any());
        verify(securityAuditService, never()).logPreferenceChange(any(), any(), any());
    }

    @Test
    @DisplayName("Should get enabled preferences for user")
    void shouldGetEnabledPreferencesForUser() {
        // Given
        List<NotificationPreference> enabledPrefs = Arrays.asList(defaultPreference);
        given(preferenceRepository.findEnabledPreferencesForUser(USER_ID))
                .willReturn(enabledPrefs);

        // When
        List<NotificationPreference> result = preferenceService.getEnabledPreferencesForUser(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNotificationEnabled()).isTrue();
        verify(preferenceRepository).findEnabledPreferencesForUser(USER_ID);
    }

    @Test
    @DisplayName("Should check if user has notification enabled for type")
    void shouldCheckIfUserHasNotificationEnabledForType() {
        // Given
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.of(defaultPreference));

        // When
        boolean result = preferenceService.isNotificationEnabled(USER_ID, NOTIFICATION_TYPE);

        // Then
        assertThat(result).isTrue();
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should return true when no preference exists (default enabled)")
    void shouldReturnTrueWhenNoPreferenceExists() {
        // Given
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.empty());

        // When
        boolean result = preferenceService.isNotificationEnabled(USER_ID, NOTIFICATION_TYPE);

        // Then
        assertThat(result).isTrue(); // Default is enabled
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should check if user is in quiet hours")
    void shouldCheckIfUserIsInQuietHours() {
        // Given
        LocalTime currentTime = LocalTime.of(23, 30); // Within quiet hours (22:00 - 08:00)
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.of(defaultPreference));

        // When
        boolean result = preferenceService.isInQuietHours(USER_ID, NOTIFICATION_TYPE, currentTime);

        // Then
        assertThat(result).isTrue();
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should return false for quiet hours when no preference exists")
    void shouldReturnFalseForQuietHoursWhenNoPreferenceExists() {
        // Given
        LocalTime currentTime = LocalTime.of(23, 30);
        given(preferenceRepository.findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE))
                .willReturn(Optional.empty());

        // When
        boolean result = preferenceService.isInQuietHours(USER_ID, NOTIFICATION_TYPE, currentTime);

        // Then
        assertThat(result).isFalse(); // No quiet hours by default
        verify(preferenceRepository).findByUserIdAndNotificationType(USER_ID, NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should get users eligible for in-app notifications")
    void shouldGetUsersEligibleForInAppNotifications() {
        // Given
        List<String> userIds = Arrays.asList("user1", "user2", "user3");
        given(preferenceRepository.findUserIdsForInAppNotifications(NOTIFICATION_TYPE))
                .willReturn(userIds);

        // When
        List<String> result = preferenceService.getUsersForInAppNotifications(NOTIFICATION_TYPE);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("user1", "user2", "user3");
        verify(preferenceRepository).findUserIdsForInAppNotifications(NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should get users eligible for email notifications")
    void shouldGetUsersEligibleForEmailNotifications() {
        // Given
        List<String> userIds = Arrays.asList("user1", "user2");
        given(preferenceRepository.findUserIdsForEmailNotifications(NOTIFICATION_TYPE))
                .willReturn(userIds);

        // When
        List<String> result = preferenceService.getUsersForEmailNotifications(NOTIFICATION_TYPE);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("user1", "user2");
        verify(preferenceRepository).findUserIdsForEmailNotifications(NOTIFICATION_TYPE);
    }

    @Test
    @DisplayName("Should get digest preferences for frequency")
    void shouldGetDigestPreferencesForFrequency() {
        // Given
        List<NotificationPreference> digestPrefs = Arrays.asList(defaultPreference);
        given(preferenceRepository.findDigestPreferences(NotificationFrequency.DAILY_DIGEST))
                .willReturn(digestPrefs);

        // When
        List<NotificationPreference> result = preferenceService.getDigestPreferences(NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(result).hasSize(1);
        verify(preferenceRepository).findDigestPreferences(NotificationFrequency.DAILY_DIGEST);
    }

    @Test
    @DisplayName("Should bulk create default preferences for user")
    void shouldBulkCreateDefaultPreferencesForUser() {
        // Given
        given(preferenceRepository.findByUserId(USER_ID)).willReturn(Arrays.asList());
        given(preferenceRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        List<NotificationPreference> result = preferenceService.createDefaultPreferencesForUser(USER_ID);

        // Then
        assertThat(result).hasSize(NotificationType.values().length);
        verify(preferenceRepository).findByUserId(USER_ID);
        verify(preferenceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should not create defaults when user already has preferences")
    void shouldNotCreateDefaultsWhenUserAlreadyHasPreferences() {
        // Given
        given(preferenceRepository.findByUserId(USER_ID))
                .willReturn(Arrays.asList(defaultPreference));

        // When
        List<NotificationPreference> result = preferenceService.createDefaultPreferencesForUser(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(defaultPreference);
        verify(preferenceRepository).findByUserId(USER_ID);
        verify(preferenceRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should validate input parameters")
    void shouldValidateInputParameters() {
        // When & Then
        assertThatThrownBy(() -> 
            preferenceService.createPreference(null, NOTIFICATION_TYPE, true, true, false, 
                    NotificationFrequency.IMMEDIATE, null, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("User ID cannot be null or blank");

        assertThatThrownBy(() -> 
            preferenceService.createPreference(USER_ID, null, true, true, false, 
                    NotificationFrequency.IMMEDIATE, null, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Notification type cannot be null");

        assertThatThrownBy(() -> 
            preferenceService.createPreference(USER_ID, NOTIFICATION_TYPE, true, true, false, 
                    null, null, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Frequency cannot be null");
    }
}