package com.focushive.integration.service;

import com.focushive.integration.client.NotificationServiceClient;
import com.focushive.integration.dto.NotificationDtos;
import com.focushive.integration.fallback.NotificationServiceFallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Integration Service Tests")
class NotificationIntegrationServiceTest {

    @Mock
    private NotificationServiceClient notificationClient;

    private NotificationIntegrationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationIntegrationService(notificationClient);
    }

    @Test
    @DisplayName("Should send notification successfully")
    void shouldSendNotificationSuccessfully() {
        // Given
        String userId = "user123";
        String type = "INFO";
        String title = "Test Notification";
        String message = "This is a test message";
        String channel = "EMAIL";

        NotificationDtos.NotificationResponse mockResponse = new NotificationDtos.NotificationResponse();
        mockResponse.setId("notif-001");
        mockResponse.setUserId(userId);
        mockResponse.setType(type);
        mockResponse.setTitle(title);
        mockResponse.setMessage(message);
        mockResponse.setStatus("SENT");
        mockResponse.setChannel(channel);
        mockResponse.setTimestamp(LocalDateTime.now());

        when(notificationClient.sendNotification(any(NotificationDtos.NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        NotificationDtos.NotificationResponse response = notificationService.sendNotification(
                userId, type, title, message, channel
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("notif-001");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(notificationClient).sendNotification(any(NotificationDtos.NotificationRequest.class));
    }

    @Test
    @DisplayName("Should send batch notifications successfully")
    void shouldSendBatchNotificationsSuccessfully() {
        // Given
        NotificationDtos.NotificationRequest request1 = new NotificationDtos.NotificationRequest();
        request1.setUserId("user1");
        request1.setType("INFO");
        request1.setTitle("Notification 1");
        request1.setMessage("Message 1");
        request1.setChannel("EMAIL");

        NotificationDtos.NotificationRequest request2 = new NotificationDtos.NotificationRequest();
        request2.setUserId("user2");
        request2.setType("WARNING");
        request2.setTitle("Notification 2");
        request2.setMessage("Message 2");
        request2.setChannel("PUSH");

        List<NotificationDtos.NotificationRequest> requests = Arrays.asList(request1, request2);

        NotificationDtos.NotificationResponse response1 = new NotificationDtos.NotificationResponse();
        response1.setId("notif-001");
        response1.setStatus("SENT");

        NotificationDtos.NotificationResponse response2 = new NotificationDtos.NotificationResponse();
        response2.setId("notif-002");
        response2.setStatus("SENT");

        when(notificationClient.sendBatchNotifications(requests))
                .thenReturn(Arrays.asList(response1, response2));

        // When
        List<NotificationDtos.NotificationResponse> responses = notificationService.sendBatchNotifications(requests);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("notif-001");
        assertThat(responses.get(1).getId()).isEqualTo("notif-002");
        verify(notificationClient).sendBatchNotifications(requests);
    }

    @Test
    @DisplayName("Should send hive notification with metadata")
    void shouldSendHiveNotificationWithMetadata() {
        // Given
        String userId = "user123";
        String hiveId = "hive456";
        String event = "USER_JOINED";
        String message = "A new user joined the hive";

        NotificationDtos.NotificationResponse mockResponse = new NotificationDtos.NotificationResponse();
        mockResponse.setId("notif-003");
        mockResponse.setType("HIVE_EVENT");
        mockResponse.setMetadata(Map.of("hiveId", hiveId, "event", event));

        when(notificationClient.sendNotification(any(NotificationDtos.NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        NotificationDtos.NotificationResponse response = notificationService.sendHiveNotification(
                userId, hiveId, event, message
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("HIVE_EVENT");
        assertThat(response.getMetadata()).containsEntry("hiveId", hiveId);
        assertThat(response.getMetadata()).containsEntry("event", event);
        verify(notificationClient).sendNotification(any(NotificationDtos.NotificationRequest.class));
    }

    @Test
    @DisplayName("Should send buddy notification")
    void shouldSendBuddyNotification() {
        // Given
        String userId = "user123";
        String buddyId = "buddy456";
        String event = "MATCH_REQUEST";
        String message = "You have a new buddy request";

        NotificationDtos.NotificationResponse mockResponse = new NotificationDtos.NotificationResponse();
        mockResponse.setId("notif-004");
        mockResponse.setType("BUDDY_EVENT");

        when(notificationClient.sendNotification(any(NotificationDtos.NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        NotificationDtos.NotificationResponse response = notificationService.sendBuddyNotification(
                userId, buddyId, event, message
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("BUDDY_EVENT");
        verify(notificationClient).sendNotification(any(NotificationDtos.NotificationRequest.class));
    }

    @Test
    @DisplayName("Should send focus session reminder")
    void shouldSendFocusSessionReminder() {
        // Given
        String userId = "user123";
        String sessionId = "session789";
        int minutesBefore = 15;

        NotificationDtos.NotificationResponse mockResponse = new NotificationDtos.NotificationResponse();
        mockResponse.setId("notif-005");
        mockResponse.setType("REMINDER");

        when(notificationClient.sendNotification(any(NotificationDtos.NotificationRequest.class)))
                .thenReturn(mockResponse);

        // When
        NotificationDtos.NotificationResponse response = notificationService.sendFocusSessionReminder(
                userId, sessionId, minutesBefore
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("REMINDER");
        verify(notificationClient).sendNotification(any(NotificationDtos.NotificationRequest.class));
    }

    @Test
    @DisplayName("Should get user notifications")
    void shouldGetUserNotifications() {
        // Given
        String userId = "user123";

        NotificationDtos.NotificationResponse notif1 = new NotificationDtos.NotificationResponse();
        notif1.setId("notif-001");
        notif1.setUserId(userId);

        NotificationDtos.NotificationResponse notif2 = new NotificationDtos.NotificationResponse();
        notif2.setId("notif-002");
        notif2.setUserId(userId);

        when(notificationClient.getUserNotifications(userId))
                .thenReturn(Arrays.asList(notif1, notif2));

        // When
        List<NotificationDtos.NotificationResponse> notifications = notificationService.getUserNotifications(userId);

        // Then
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getUserId()).isEqualTo(userId);
        assertThat(notifications.get(1).getUserId()).isEqualTo(userId);
        verify(notificationClient).getUserNotifications(userId);
    }

    @Test
    @DisplayName("Should mark notification as read")
    void shouldMarkNotificationAsRead() {
        // Given
        String notificationId = "notif-001";
        doNothing().when(notificationClient).markAsRead(notificationId);

        // When
        notificationService.markNotificationAsRead(notificationId);

        // Then
        verify(notificationClient).markAsRead(notificationId);
    }

    @Test
    @DisplayName("Should delete notification")
    void shouldDeleteNotification() {
        // Given
        String notificationId = "notif-001";
        doNothing().when(notificationClient).deleteNotification(notificationId);

        // When
        notificationService.deleteNotification(notificationId);

        // Then
        verify(notificationClient).deleteNotification(notificationId);
    }

    @Test
    @DisplayName("Should check notification service health")
    void shouldCheckNotificationServiceHealth() {
        // Given
        when(notificationClient.healthCheck()).thenReturn("{\"status\":\"UP\"}");

        // When
        boolean isHealthy = notificationService.isNotificationServiceHealthy();

        // Then
        assertThat(isHealthy).isTrue();
        verify(notificationClient).healthCheck();
    }

    @Test
    @DisplayName("Should return false when service is unhealthy")
    void shouldReturnFalseWhenServiceIsUnhealthy() {
        // Given
        when(notificationClient.healthCheck()).thenReturn("{\"status\":\"DOWN\"}");

        // When
        boolean isHealthy = notificationService.isNotificationServiceHealthy();

        // Then
        assertThat(isHealthy).isFalse();
        verify(notificationClient).healthCheck();
    }

    @Test
    @DisplayName("Should return false when health check throws exception")
    void shouldReturnFalseWhenHealthCheckThrowsException() {
        // Given
        when(notificationClient.healthCheck()).thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean isHealthy = notificationService.isNotificationServiceHealthy();

        // Then
        assertThat(isHealthy).isFalse();
        verify(notificationClient).healthCheck();
    }

    @Test
    @DisplayName("Should handle exception when sending notification fails")
    void shouldHandleExceptionWhenSendingNotificationFails() {
        // Given
        when(notificationClient.sendNotification(any(NotificationDtos.NotificationRequest.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When/Then
        assertThrows(RuntimeException.class, () ->
                notificationService.sendNotification("user123", "INFO", "Title", "Message", "EMAIL")
        );
        verify(notificationClient).sendNotification(any(NotificationDtos.NotificationRequest.class));
    }

    @Test
    @DisplayName("Should test fallback behavior")
    void shouldTestFallbackBehavior() {
        // Given - Using the fallback directly
        NotificationServiceFallback fallback = new NotificationServiceFallback();

        NotificationDtos.NotificationRequest request = new NotificationDtos.NotificationRequest();
        request.setUserId("user123");
        request.setType("INFO");
        request.setTitle("Test");
        request.setMessage("Test message");
        request.setChannel("EMAIL");

        // When
        NotificationDtos.NotificationResponse response = fallback.sendNotification(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FALLBACK");
        assertThat(response.getMessage()).contains("queued for later delivery");
    }

    @Test
    @DisplayName("Should test batch notification fallback")
    void shouldTestBatchNotificationFallback() {
        // Given
        NotificationServiceFallback fallback = new NotificationServiceFallback();
        List<NotificationDtos.NotificationRequest> requests = Arrays.asList(
                new NotificationDtos.NotificationRequest(),
                new NotificationDtos.NotificationRequest()
        );

        // When
        List<NotificationDtos.NotificationResponse> responses = fallback.sendBatchNotifications(requests);

        // Then
        assertThat(responses).isNotEmpty();
        assertThat(responses.get(0).getStatus()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("Should return empty list for user notifications in fallback")
    void shouldReturnEmptyListForUserNotificationsInFallback() {
        // Given
        NotificationServiceFallback fallback = new NotificationServiceFallback();

        // When
        List<NotificationDtos.NotificationResponse> notifications = fallback.getUserNotifications("user123");

        // Then
        assertThat(notifications).isEmpty();
    }

    @Test
    @DisplayName("Should return DOWN status in fallback health check")
    void shouldReturnDownStatusInFallbackHealthCheck() {
        // Given
        NotificationServiceFallback fallback = new NotificationServiceFallback();

        // When
        String health = fallback.healthCheck();

        // Then
        assertThat(health).contains("DOWN");
        assertThat(health).contains("unreachable");
    }
}