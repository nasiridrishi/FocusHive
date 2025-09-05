package com.focushive.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.NotificationService;
import com.focushive.user.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Import mock dependencies that might be needed
import com.focushive.backend.service.IdentityIntegrationService;
import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@WebMvcTest(controllers = NotificationController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    })
@ActiveProfiles("test")  
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    // Mock all potential dependencies that might be required by WebMvcTest
    @MockBean
    private IdentityIntegrationService identityIntegrationService;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private NotificationDto testNotificationDto;
    private CreateNotificationRequest createRequest;
    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        
        testNotificationDto = NotificationDto.builder()
                .id("notification-123")
                .userId(testUserId)
                .type("HIVE_INVITATION")
                .title("Test Notification")
                .content("Test content")
                .priority(Notification.NotificationPriority.NORMAL)
                .isRead(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .data(Map.of())
                .build();

        createRequest = CreateNotificationRequest.builder()
                .userId(testUserId)
                .type(NotificationType.HIVE_INVITATION)
                .title("Test Notification")
                .content("Test content")
                .priority(Notification.NotificationPriority.NORMAL)
                .variables(Map.of("hiveName", "Study Group"))
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void createNotification_Success() throws Exception {
        // Given
        when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                .thenReturn(testNotificationDto);

        // When & Then
        mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())  // Debug: print response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("notification-123"))
                .andExpect(jsonPath("$.title").value("Test Notification"))
                .andExpect(jsonPath("$.type").value("HIVE_INVITATION"))
                .andExpect(jsonPath("$.isRead").value(false));

        verify(notificationService).createNotification(any(CreateNotificationRequest.class));
    }

    @Test
    void createNotification_InvalidRequest_BadRequest() throws Exception {
        // Given - request with missing title
        CreateNotificationRequest invalidRequest = CreateNotificationRequest.builder()
                .userId(testUserId)
                .type(NotificationType.HIVE_INVITATION)
                .build();

        // When & Then
        mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).createNotification(any());
    }

    @Test
    void createNotification_UserNotFound_BadRequest() throws Exception {
        // Given
        when(notificationService.createNotification(any(CreateNotificationRequest.class)))
                .thenThrow(new IllegalArgumentException("User not found: " + testUserId));

        // When & Then
        mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                // The controller returns an empty NotificationDto in the catch block, not a message
                .andExpect(jsonPath("$.id").value((String) null));
    }

    @Test
    void getNotifications_Success() throws Exception {
        // Given
        NotificationResponse response = NotificationResponse.builder()
                .notifications(Arrays.asList(testNotificationDto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .numberOfElements(1)
                .empty(false)
                .build();

        when(notificationService.getNotifications(eq(testUserId), any(Pageable.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/notifications")
                .param("userId", testUserId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.notifications[0].id").value("notification-123"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(notificationService).getNotifications(eq(testUserId), any(Pageable.class));
    }

    @Test
    void getNotifications_MissingUserId_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getNotifications(anyString(), any(Pageable.class));
    }

    @Test
    void getUnreadNotifications_Success() throws Exception {
        // Given
        NotificationResponse response = NotificationResponse.builder()
                .notifications(Arrays.asList(testNotificationDto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .numberOfElements(1)
                .empty(false)
                .build();

        when(notificationService.getUnreadNotifications(eq(testUserId), any(Pageable.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread")
                .param("userId", testUserId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(notificationService).getUnreadNotifications(eq(testUserId), any(Pageable.class));
    }

    @Test
    void markAsRead_Success() throws Exception {
        // Given
        String notificationId = "notification-123";

        // When & Then
        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                .param("userId", testUserId))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(notificationId, testUserId);
    }

    @Test
    void markAsRead_NotificationNotFound_BadRequest() throws Exception {
        // Given
        String notificationId = "non-existent";
        doThrow(new IllegalArgumentException("Notification not found: " + notificationId))
                .when(notificationService).markAsRead(notificationId, testUserId);

        // When & Then
        mockMvc.perform(patch("/api/notifications/{id}/read", notificationId)
                .param("userId", testUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Notification not found: " + notificationId));
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        // Given
        when(notificationService.markAllAsRead(testUserId)).thenReturn(5);

        // When & Then
        mockMvc.perform(patch("/api/notifications/read-all")
                .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedCount").value(5));

        verify(notificationService).markAllAsRead(testUserId);
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        // Given
        when(notificationService.getUnreadCount(testUserId)).thenReturn(3L);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread/count")
                .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));

        verify(notificationService).getUnreadCount(testUserId);
    }

    @Test
    void deleteNotification_Success() throws Exception {
        // Given
        String notificationId = "notification-123";

        // When & Then
        mockMvc.perform(delete("/api/notifications/{id}", notificationId)
                .param("userId", testUserId))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(notificationId, testUserId);
    }

    @Test
    void deleteNotification_NotificationNotFound_BadRequest() throws Exception {
        // Given
        String notificationId = "non-existent";
        doThrow(new IllegalArgumentException("Notification not found: " + notificationId))
                .when(notificationService).deleteNotification(notificationId, testUserId);

        // When & Then
        mockMvc.perform(delete("/api/notifications/{id}", notificationId)
                .param("userId", testUserId))
                .andExpect(status().isBadRequest())
                // The controller returns empty body in the catch block, not a JSON message
                .andExpect(content().string(""));
    }

    @Test
    void archiveNotification_Success() throws Exception {
        // Given
        String notificationId = "notification-123";

        // When & Then
        mockMvc.perform(patch("/api/notifications/{id}/archive", notificationId)
                .param("userId", testUserId))
                .andExpect(status().isOk());

        verify(notificationService).archiveNotification(notificationId, testUserId);
    }

    @Test
    void getNotificationsByType_Success() throws Exception {
        // Given
        String type = "HIVE_INVITATION";
        NotificationResponse response = NotificationResponse.builder()
                .notifications(Arrays.asList(testNotificationDto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .numberOfElements(1)
                .empty(false)
                .build();

        when(notificationService.getNotificationsByType(eq(testUserId), eq(type), any(Pageable.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/notifications/type/{type}", type)
                .param("userId", testUserId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.notifications[0].type").value("HIVE_INVITATION"));

        verify(notificationService).getNotificationsByType(eq(testUserId), eq(type), any(Pageable.class));
    }

    @Test
    void cleanupOldNotifications_Success() throws Exception {
        // Given
        int daysToKeep = 30;
        when(notificationService.cleanupOldNotifications(testUserId, daysToKeep)).thenReturn(10);

        // When & Then
        mockMvc.perform(delete("/api/notifications/cleanup")
                .param("userId", testUserId)
                .param("daysToKeep", String.valueOf(daysToKeep)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(10));

        verify(notificationService).cleanupOldNotifications(testUserId, daysToKeep);
    }

    @Test
    void cleanupOldNotifications_DefaultDays_Success() throws Exception {
        // Given
        int defaultDays = 30; // Default value
        when(notificationService.cleanupOldNotifications(testUserId, defaultDays)).thenReturn(5);

        // When & Then
        mockMvc.perform(delete("/api/notifications/cleanup")
                .param("userId", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(5));

        verify(notificationService).cleanupOldNotifications(testUserId, defaultDays);
    }
}