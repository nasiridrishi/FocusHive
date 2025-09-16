package com.focushive.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.service.impl.NotificationCleanupServiceImpl;
import com.focushive.notification.monitoring.NotificationMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCleanupService Tests")
class NotificationCleanupServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMetricsService metricsService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Counter cleanupCounter;

    @Mock
    private Counter archiveCounter;

    @Mock
    private Timer cleanupTimer;

    @Mock
    private Timer.Sample timerSample;

    @InjectMocks
    private NotificationCleanupServiceImpl cleanupService;

    private MockedStatic<Timer> timerMockedStatic;
    private Notification oldNotification;
    private Notification recentNotification;
    private LocalDateTime cutoffDate;

    @BeforeEach
    void setUp() {
        // Setup timer static mocking
        timerMockedStatic = mockStatic(Timer.class);
        timerMockedStatic.when(() -> Timer.start(meterRegistry)).thenReturn(timerSample);

        // Mock meter registry responses - must be done before initializeMetrics is called
        when(meterRegistry.counter("notification.cleanup.processed")).thenReturn(cleanupCounter);
        when(meterRegistry.counter("notification.cleanup.archived")).thenReturn(archiveCounter);
        when(meterRegistry.timer("notification.cleanup.duration")).thenReturn(cleanupTimer);

        // Initialize metrics manually since @PostConstruct won't be called in unit tests
        cleanupService.initializeMetrics();

        // Set configuration properties
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 90);
        ReflectionTestUtils.setField(cleanupService, "batchSize", 1000);
        ReflectionTestUtils.setField(cleanupService, "enableCleanup", true);

        // Create test data
        cutoffDate = LocalDateTime.now().minusDays(90);

        oldNotification = spy(new Notification());
        oldNotification.setId("1");
        oldNotification.setUserId("user1");
        oldNotification.setTitle("Old notification");
        oldNotification.setCreatedAt(LocalDateTime.now().minusDays(100));

        recentNotification = spy(new Notification());
        recentNotification.setId("2");
        recentNotification.setUserId("user2");
        recentNotification.setTitle("Recent notification");
        recentNotification.setCreatedAt(LocalDateTime.now().minusDays(30));
    }

    @AfterEach
    void tearDown() {
        if (timerMockedStatic != null) {
            timerMockedStatic.close();
        }
    }

    @Test
    @DisplayName("Should successfully cleanup old notifications")
    void shouldCleanupOldNotifications() {
        // Given
        List<Notification> oldNotifications = Arrays.asList(oldNotification);
        Page<Notification> page = new PageImpl<>(oldNotifications);
        
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        
        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getArchivedCount());
        assertEquals(0, result.getDeletedCount());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertTrue(result.getDurationMillis() >= 0);
        
        verify(notificationRepository).findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class));
        verify(notificationRepository).saveAll(oldNotifications);
        verify(cleanupCounter).increment(1);
        verify(archiveCounter).increment(1);
        verify(timerSample).stop(cleanupTimer);
    }

    @Test
    @DisplayName("Should handle empty result set gracefully")
    void shouldHandleEmptyResultSet() {
        // Given
        Page<Notification> emptyPage = new PageImpl<>(Arrays.asList());
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(emptyPage);
        
        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(0, result.getProcessedCount());
        assertEquals(0, result.getArchivedCount());
        assertEquals(0, result.getDeletedCount());
        
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should handle database errors gracefully")
    void shouldHandleDatabaseErrors() {
        // Given
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getProcessedCount());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Database connection failed"));
    }

    @Test
    @DisplayName("Should respect cleanup enabled flag")
    void shouldRespectCleanupEnabledFlag() {
        // Given
        ReflectionTestUtils.setField(cleanupService, "enableCleanup", false);
        
        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("disabled"));
        verify(notificationRepository, never()).findOldNotificationsForCleanup(any(), any());
    }

    @Test
    @DisplayName("Should process large datasets in batches")
    void shouldProcessLargeDatasetsInBatches() {
        // Given
        ReflectionTestUtils.setField(cleanupService, "batchSize", 2);

        List<Notification> batch1 = Arrays.asList(oldNotification, createOldNotification("3"));
        List<Notification> batch2 = Arrays.asList(createOldNotification("4"));

        // Create pages with proper hasNext() behavior
        Page<Notification> page1 = new PageImpl<>(batch1, PageRequest.of(0, 2), 3);
        Page<Notification> page2 = new PageImpl<>(batch2, PageRequest.of(1, 2), 3);

        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), eq(PageRequest.of(0, 2))))
                .thenReturn(page1);
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), eq(PageRequest.of(1, 2))))
                .thenReturn(page2);

        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getProcessedCount());
        assertEquals(3, result.getArchivedCount());

        verify(notificationRepository, times(2)).findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class));
        verify(notificationRepository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Should cleanup by user ID")
    void shouldCleanupByUserId() {
        // Given
        String userId = "user1";
        List<Notification> userNotifications = Arrays.asList(oldNotification);
        Page<Notification> page = new PageImpl<>(userNotifications);
        
        when(notificationRepository.findOldNotificationsByUserForCleanup(eq(userId), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        
        // When
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupUserNotifications(userId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getArchivedCount());
        
        verify(notificationRepository).findOldNotificationsByUserForCleanup(eq(userId), any(LocalDateTime.class), any(PageRequest.class));
        verify(notificationRepository).saveAll(userNotifications);
    }

    @Test
    @DisplayName("Should validate user ID input")
    void shouldValidateUserIdInput() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cleanupService.cleanupUserNotifications(null));
        assertThrows(IllegalArgumentException.class, () -> cleanupService.cleanupUserNotifications(""));
        assertThrows(IllegalArgumentException.class, () -> cleanupService.cleanupUserNotifications("   "));
    }

    @Test
    @DisplayName("Should get cleanup statistics")
    void shouldGetCleanupStatistics() {
        // Given
        when(notificationRepository.countNotificationsOlderThan(any(LocalDateTime.class))).thenReturn(50L);
        when(notificationRepository.countArchivedNotifications()).thenReturn(100L);
        when(notificationRepository.countDeletedNotifications()).thenReturn(25L);

        // When
        NotificationCleanupService.CleanupStatistics stats = cleanupService.getCleanupStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(50L, stats.getEligibleForCleanup());
        assertEquals(100L, stats.getTotalArchived());
        assertEquals(25L, stats.getTotalDeleted());
        // lastCleanupTime is null initially, which is expected
        assertEquals(90, stats.getRetentionDays());
        assertTrue(stats.isCleanupEnabled());
        assertEquals("IDLE", stats.getStatus());
    }

    @Test
    @DisplayName("Should export archived data")
    void shouldExportArchivedData() throws Exception {
        // Given
        // Set fields on notification for export
        oldNotification.setContent("Test content");
        oldNotification.setType(NotificationType.SYSTEM_NOTIFICATION);
        oldNotification.setPriority(Notification.NotificationPriority.NORMAL);

        List<Notification> archivedNotifications = Arrays.asList(oldNotification);
        Page<Notification> page = new PageImpl<>(archivedNotifications);

        when(notificationRepository.findArchivedNotifications(any(PageRequest.class)))
                .thenReturn(page);

        // Mock ObjectMapper to return a JSON string
        String mockJson = "[{\"id\":\"1\",\"userId\":\"user1\",\"title\":\"Old notification\"}]";
        when(objectMapper.writeValueAsString(any())).thenReturn(mockJson);

        // When
        NotificationCleanupService.ExportResult result = cleanupService.exportArchivedData();

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());
        assertNotNull(result.getData());
        assertEquals(mockJson, result.getData());
        assertEquals("JSON", result.getFormat());
    }

    @Test
    @DisplayName("Should run async cleanup job")
    void shouldRunAsyncCleanupJob() {
        // Given
        List<Notification> oldNotifications = Arrays.asList(oldNotification);
        Page<Notification> page = new PageImpl<>(oldNotifications);
        
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(page);
        
        // When
        CompletableFuture<NotificationCleanupService.CleanupResult> future = cleanupService.runAsyncCleanup();
        
        // Then
        assertNotNull(future);
        assertDoesNotThrow(() -> {
            NotificationCleanupService.CleanupResult result = future.get();
            assertNotNull(result);
            assertTrue(result.isSuccess());
        });
    }

    @Test
    @DisplayName("Should handle concurrent cleanup requests")
    void shouldHandleConcurrentCleanupRequests() {
        // Given - First cleanup is running
        when(notificationRepository.findOldNotificationsForCleanup(any(LocalDateTime.class), any(PageRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(100); // Simulate slow operation
                    return new PageImpl<>(Arrays.asList());
                });
        
        // When - Start two cleanups concurrently
        CompletableFuture<NotificationCleanupService.CleanupResult> future1 = cleanupService.runAsyncCleanup();
        CompletableFuture<NotificationCleanupService.CleanupResult> future2 = cleanupService.runAsyncCleanup();
        
        // Then
        assertDoesNotThrow(() -> {
            NotificationCleanupService.CleanupResult result1 = future1.get();
            NotificationCleanupService.CleanupResult result2 = future2.get();
            
            // One should succeed, one should be skipped
            assertTrue(result1.isSuccess() || result2.isSuccess());
            
            if (!result1.isSuccess()) {
                assertTrue(result1.getErrorMessage().contains("already running"));
            }
            if (!result2.isSuccess()) {
                assertTrue(result2.getErrorMessage().contains("already running"));
            }
        });
    }

    @Test
    @DisplayName("Should calculate retention cutoff date correctly")
    void shouldCalculateRetentionCutoffDateCorrectly() {
        // Given
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 30);
        
        // When
        LocalDateTime cutoff = (LocalDateTime) ReflectionTestUtils.invokeMethod(cleanupService, "getRetentionCutoffDate");
        
        // Then
        assertNotNull(cutoff);
        assertTrue(cutoff.isBefore(LocalDateTime.now()));
        
        // Should be approximately 30 days ago (within 1 minute tolerance)
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);
        assertTrue(Math.abs(cutoff.until(expectedCutoff, java.time.temporal.ChronoUnit.MINUTES)) <= 1);
    }

    @Test
    @DisplayName("Should mark notifications as archived correctly")
    void shouldMarkNotificationsAsArchivedCorrectly() {
        // Given
        Notification notification = new Notification();
        // No setDeleted method - deletedAt is null by default
        notification.setArchivedAt(null);
        
        // When
        ReflectionTestUtils.invokeMethod(cleanupService, "archiveNotification", notification);
        
        // Then
        assertNotNull(notification.getDeletedAt());
        assertNotNull(notification.getArchivedAt());
        assertTrue(notification.getArchivedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    private Notification createOldNotification(String id) {
        Notification notification = spy(new Notification());
        notification.setId(id);
        notification.setUserId("user" + id);
        notification.setTitle("Old notification " + id);
        notification.setCreatedAt(LocalDateTime.now().minusDays(100));
        return notification;
    }
}