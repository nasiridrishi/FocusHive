package com.focushive.notification.service;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.messaging.dto.NotificationMessage;
import com.focushive.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationDigestService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDigestServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private NotificationDigestService digestService;

    private NotificationPreference dailyPreference;
    private NotificationPreference weeklyPreference;
    private List<Notification> testNotifications;

    @BeforeEach
    void setUp() {
        // Create test preferences
        dailyPreference = NotificationPreference.builder()
                .userId("user1")
                .notificationType(NotificationType.TASK_ASSIGNED)
                .frequency(NotificationFrequency.DAILY_DIGEST)
                .emailEnabled(true)
                .build();
        dailyPreference.setId("pref1"); // Set ID manually since it's inherited from BaseEntity

        weeklyPreference = NotificationPreference.builder()
                .userId("user2")
                .notificationType(NotificationType.WEEKLY_SUMMARY)
                .frequency(NotificationFrequency.WEEKLY_DIGEST)
                .emailEnabled(true)
                .build();
        weeklyPreference.setId("pref2"); // Set ID manually since it's inherited from BaseEntity

        // Create test notifications
        testNotifications = Arrays.asList(
                createTestNotification("notif1", "user1", NotificationType.TASK_ASSIGNED, "Task assigned to you"),
                createTestNotification("notif2", "user1", NotificationType.BUDDY_REQUEST, "New buddy request"),
                createTestNotification("notif3", "user1", NotificationType.TASK_ASSIGNED, "Another task assigned")
        );
    }

    @Test
    void processDailyDigests_ShouldProcessAllDailyPreferences() {
        // Given
        when(preferenceService.getDigestPreferences(NotificationFrequency.DAILY_DIGEST))
                .thenReturn(List.of(dailyPreference));
        when(preferenceService.isInQuietHours(anyString(), any(NotificationType.class), any(LocalTime.class)))
                .thenReturn(false);
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq("user1"), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        digestService.processDailyDigests();

        // Then
        verify(preferenceService).getDigestPreferences(NotificationFrequency.DAILY_DIGEST);
        verify(emailNotificationService).sendEmail(any(NotificationMessage.class));
        verify(notificationRepository).saveAll(testNotifications);
        
        // Verify notifications are marked as digest processed
        assertThat(testNotifications).allMatch(n -> n.getDigestProcessedAt() != null);
    }

    @Test
    void processDailyDigests_ShouldSkipUsersInQuietHours() {
        // Given
        when(preferenceService.getDigestPreferences(NotificationFrequency.DAILY_DIGEST))
                .thenReturn(List.of(dailyPreference));
        when(preferenceService.isInQuietHours(anyString(), any(NotificationType.class), any(LocalTime.class)))
                .thenReturn(true);

        // When
        digestService.processDailyDigests();

        // Then
        verify(preferenceService).getDigestPreferences(NotificationFrequency.DAILY_DIGEST);
        verify(preferenceService).isInQuietHours(eq("user1"), eq(NotificationType.TASK_ASSIGNED), any(LocalTime.class));
        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void processWeeklyDigests_ShouldProcessAllWeeklyPreferences() {
        // Given
        when(preferenceService.getDigestPreferences(NotificationFrequency.WEEKLY_DIGEST))
                .thenReturn(List.of(weeklyPreference));
        when(preferenceService.isInQuietHours(anyString(), any(NotificationType.class), any(LocalTime.class)))
                .thenReturn(false);
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq("user2"), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        digestService.processWeeklyDigests();

        // Then
        verify(preferenceService).getDigestPreferences(NotificationFrequency.WEEKLY_DIGEST);
        verify(emailNotificationService).sendEmail(any(NotificationMessage.class));
    }

    @Test
    void processDigestForUser_ShouldGroupNotificationsByType() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        digestService.processDigestForUser(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(emailNotificationService).sendEmail(messageCaptor.capture());
        
        NotificationMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getEmailSubject()).isEqualTo("Your Daily Notification Digest");
        assertThat(capturedMessage.getEmailTo()).isEqualTo("user1@focushive.com");
        String emailContent = capturedMessage.getMessage();
        assertThat(emailContent).contains("Task Assigned (2)"); // 2 task assigned notifications
        assertThat(emailContent).contains("Buddy Request (1)"); // 1 buddy request notification
        assertThat(emailContent).contains("You have 3 new notifications");
    }

    @Test
    void processDigestForUser_ShouldSkipWhenNoNotifications() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        digestService.processDigestForUser(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        verifyNoInteractions(emailNotificationService);
    }

    @Test
    void getPendingDigestNotifications_ShouldReturnCorrectNotifications() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        List<Notification> result = digestService.getPendingDigestNotifications(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyElementsOf(testNotifications);
    }

    @Test
    void hasPendingDigestNotifications_ShouldReturnTrueWhenNotificationsExist() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        boolean result = digestService.hasPendingDigestNotifications(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasPendingDigestNotifications_ShouldReturnFalseWhenNoNotifications() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = digestService.hasPendingDigestNotifications(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getDigestSummary_ShouldReturnCorrectSummary() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(testNotifications);

        // When
        Map<String, Object> result = digestService.getDigestSummary(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        assertThat(result).containsEntry("totalCount", 3);
        assertThat(result).containsEntry("frequency", "DAILY_DIGEST");
        
        @SuppressWarnings("unchecked")
        Map<NotificationType, Long> typeBreakdown = (Map<NotificationType, Long>) result.get("typeBreakdown");
        assertThat(typeBreakdown).containsEntry(NotificationType.TASK_ASSIGNED, 2L);
        assertThat(typeBreakdown).containsEntry(NotificationType.BUDDY_REQUEST, 1L);
    }

    @Test
    void processDigestForUser_ShouldHandleEmailFailureGracefully() {
        // Given
        String userId = "user1";
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(testNotifications);
        
        doThrow(new RuntimeException("Email service unavailable"))
                .when(emailNotificationService).sendEmail(any(NotificationMessage.class));

        // When - should not throw exception
        digestService.processDigestForUser(userId, NotificationFrequency.DAILY_DIGEST);

        // Then - notifications should still be marked as processed
        verify(notificationRepository).saveAll(testNotifications);
        assertThat(testNotifications).allMatch(n -> n.getDigestProcessedAt() != null);
    }

    @Test
    void processDigestForUser_WeeklyFrequency_ShouldUseDifferentCutoffTime() {
        // Given
        String userId = "user1";
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), cutoffCaptor.capture()))
                .thenReturn(testNotifications);

        // When
        digestService.processDigestForUser(userId, NotificationFrequency.WEEKLY_DIGEST);

        // Then - cutoff time should be approximately 7 days ago
        LocalDateTime cutoffTime = cutoffCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(7);
        assertThat(cutoffTime).isBefore(expectedCutoff.plusMinutes(1));
        assertThat(cutoffTime).isAfter(expectedCutoff.minusMinutes(1));
    }

    @Test
    void processDailyDigests_ShouldHandleMultipleUsers() {
        // Given
        NotificationPreference pref1 = NotificationPreference.builder()
                .userId("user1")
                .notificationType(NotificationType.TASK_ASSIGNED)
                .frequency(NotificationFrequency.DAILY_DIGEST)
                .build();
        
        NotificationPreference pref2 = NotificationPreference.builder()
                .userId("user2")
                .notificationType(NotificationType.BUDDY_REQUEST)
                .frequency(NotificationFrequency.DAILY_DIGEST)
                .build();

        when(preferenceService.getDigestPreferences(NotificationFrequency.DAILY_DIGEST))
                .thenReturn(Arrays.asList(pref1, pref2));
        when(preferenceService.isInQuietHours(anyString(), any(NotificationType.class), any(LocalTime.class)))
                .thenReturn(false);
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(createTestNotification("notif", "user", NotificationType.TASK_ASSIGNED, "Test")));

        // When
        digestService.processDailyDigests();

        // Then - should process both users
        verify(emailNotificationService, times(2)).sendEmail(any(NotificationMessage.class));
    }

    @Test
    void processDigestForUser_ShouldLimitNotificationsInEmail() {
        // Given
        String userId = "user1";
        List<Notification> manyNotifications = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            manyNotifications.add(createTestNotification("notif" + i, userId, NotificationType.TASK_ASSIGNED, "Task " + i));
        }
        
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtAfterAndDigestProcessedAtIsNullOrderByCreatedAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(manyNotifications);

        // When
        digestService.processDigestForUser(userId, NotificationFrequency.DAILY_DIGEST);

        // Then
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(emailNotificationService).sendEmail(messageCaptor.capture());

        String emailContent = messageCaptor.getValue().getMessage();
        assertThat(emailContent).contains("...and 3 more"); // Should show "and X more" for notifications > 5
        assertThat(emailContent).contains("You have 8 new notifications");
    }

    private Notification createTestNotification(String id, String userId, NotificationType type, String title) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content("Test content")
                .isRead(false)
                .build();
        notification.setId(id);
        return notification;
    }
}