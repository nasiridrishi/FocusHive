package com.focushive.notification.service;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import com.focushive.notification.service.delivery.NotificationDeliveryService;
import com.focushive.notification.service.impl.NotificationServiceImpl;
import com.focushive.user.entity.Notification;
import com.focushive.user.entity.User;
import com.focushive.user.repository.NotificationRepository;
import com.focushive.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private NotificationDeliveryService notificationDeliveryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private String testUserId;
    private Notification testNotification;
    private NotificationPreference testPreference;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testNotification = new Notification();
        testNotification.setId("notification-123");
        testNotification.setUser(testUser);
        testNotification.setType("HIVE_INVITATION");
        testNotification.setTitle("Test Notification");
        testNotification.setContent("Test content");
        testNotification.setPriority(Notification.NotificationPriority.NORMAL);
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        testPreference = NotificationPreference.builder()
                .userId(testUserId)
                .notificationType(NotificationType.HIVE_INVITATION)
                .inAppEnabled(true)
                .emailEnabled(true)
                .pushEnabled(false)
                .frequency(NotificationFrequency.IMMEDIATE)
                .build();
    }

    @Test
    void createNotification_Success() {
        // Given
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(testUserId)
                .type(NotificationType.HIVE_INVITATION)
                .title("Test Notification")
                .content("Test content")
                .priority(Notification.NotificationPriority.NORMAL)
                .variables(Map.of("hiveName", "Study Group"))
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(testUserId, NotificationType.HIVE_INVITATION))
                .thenReturn(Optional.of(testPreference));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.createNotification(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("notification-123");
        assertThat(result.getTitle()).isEqualTo("Test Notification");
        assertThat(result.getType()).isEqualTo("HIVE_INVITATION");

        // Verify notification was saved
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUser()).isEqualTo(testUser);
        assertThat(savedNotification.getType()).isEqualTo("HIVE_INVITATION");

        // Verify delivery was triggered
        verify(notificationDeliveryService).deliverNotification(any(NotificationDto.class), eq(testPreference));
    }

    @Test
    void createNotification_WithUserNotFound_ShouldThrowException() {
        // Given
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId("non-existent-user")
                .type(NotificationType.HIVE_INVITATION)
                .title("Test Notification")
                .build();

        when(userRepository.findById("non-existent-user")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createNotification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found: non-existent-user");
    }

    @Test
    void createNotification_WithNoPreferences_ShouldUseDefaults() {
        // Given
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(testUserId)
                .type(NotificationType.HIVE_INVITATION)
                .title("Test Notification")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(testUserId, NotificationType.HIVE_INVITATION))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        NotificationDto result = notificationService.createNotification(request);

        // Then
        assertThat(result).isNotNull();
        
        // Verify delivery was triggered with default preferences
        ArgumentCaptor<NotificationPreference> preferenceCaptor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationDeliveryService).deliverNotification(any(NotificationDto.class), preferenceCaptor.capture());
        
        NotificationPreference usedPreference = preferenceCaptor.getValue();
        assertThat(usedPreference.getInAppEnabled()).isTrue();
        assertThat(usedPreference.getEmailEnabled()).isTrue();
        assertThat(usedPreference.getPushEnabled()).isTrue();
        assertThat(usedPreference.getFrequency()).isEqualTo(NotificationFrequency.IMMEDIATE);
    }

    @Test
    void createNotification_DuringQuietHours_ShouldNotDeliver() {
        // Given
        testPreference.setQuietStartTime(LocalTime.of(22, 0));
        testPreference.setQuietEndTime(LocalTime.of(8, 0));
        
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(testUserId)
                .type(NotificationType.HIVE_INVITATION)
                .title("Test Notification")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(notificationPreferenceRepository.findByUserIdAndNotificationType(testUserId, NotificationType.HIVE_INVITATION))
                .thenReturn(Optional.of(testPreference));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Mock current time to be during quiet hours (23:00)
        LocalTime currentTime = LocalTime.of(23, 0);

        // When
        NotificationDto result = notificationService.createNotification(request, currentTime);

        // Then
        assertThat(result).isNotNull();
        
        // Verify notification was saved but not delivered
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationDeliveryService, never()).deliverNotification(any(), any());
    }

    @Test
    void getNotifications_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(notificationPage);

        // When
        NotificationResponse result = notificationService.getNotifications(testUserId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotifications()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    void getUnreadNotifications_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, 1);

        when(notificationRepository.findUnreadByUserId(testUserId, pageable))
                .thenReturn(notificationPage);

        // When
        NotificationResponse result = notificationService.getUnreadNotifications(testUserId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotifications()).hasSize(1);
        assertThat(result.getNotifications().get(0).getIsRead()).isFalse();
    }

    @Test
    void markAsRead_Success() {
        // Given
        String notificationId = "notification-123";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        // When
        notificationService.markAsRead(notificationId, testUserId);

        // Then
        verify(notificationRepository).markAsRead(eq(notificationId), eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    void markAsRead_NotificationNotFound_ShouldThrowException() {
        // Given
        String notificationId = "non-existent";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, testUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification not found: non-existent");
    }

    @Test
    void markAsRead_WrongUser_ShouldThrowException() {
        // Given
        String notificationId = "notification-123";
        String wrongUserId = "wrong-user";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        // When & Then
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, wrongUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification does not belong to user: wrong-user");
    }

    @Test
    void markAllAsRead_Success() {
        // Given
        when(notificationRepository.markAllAsRead(eq(testUserId), any(LocalDateTime.class))).thenReturn(5);

        // When
        int result = notificationService.markAllAsRead(testUserId);

        // Then
        assertThat(result).isEqualTo(5);
        verify(notificationRepository).markAllAsRead(eq(testUserId), any(LocalDateTime.class));
    }

    @Test
    void getUnreadCount_Success() {
        // Given
        when(notificationRepository.countByUserIdAndIsReadFalse(testUserId)).thenReturn(3L);

        // When
        long result = notificationService.getUnreadCount(testUserId);

        // Then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    void deleteNotification_Success() {
        // Given
        String notificationId = "notification-123";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        // When
        notificationService.deleteNotification(notificationId, testUserId);

        // Then
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void deleteNotification_NotificationNotFound_ShouldThrowException() {
        // Given
        String notificationId = "non-existent";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, testUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification not found: non-existent");
    }

    @Test
    void archiveNotification_Success() {
        // Given
        String notificationId = "notification-123";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        // When
        notificationService.archiveNotification(notificationId, testUserId);

        // Then
        verify(notificationRepository).markAsArchived(notificationId, testUserId);
    }

    @Test
    void cleanupOldNotifications_Success() {
        // Given
        int daysToKeep = 30;
        when(notificationRepository.deleteOldReadNotifications(eq(testUserId), any(LocalDateTime.class)))
                .thenReturn(10);

        // When
        int result = notificationService.cleanupOldNotifications(testUserId, daysToKeep);

        // Then
        assertThat(result).isEqualTo(10);
        
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteOldReadNotifications(eq(testUserId), dateCaptor.capture());
        
        LocalDateTime capturedDate = dateCaptor.getValue();
        assertThat(capturedDate).isBefore(LocalDateTime.now().minusDays(daysToKeep - 1));
    }
}