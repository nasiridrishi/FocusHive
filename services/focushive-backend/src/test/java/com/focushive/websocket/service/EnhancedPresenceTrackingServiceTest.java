package com.focushive.websocket.service;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Enhanced unit tests for PresenceTrackingService.
 * Tests advanced features like presence synchronization, recovery, and batch operations.
 * THIS WILL FAIL initially as enhanced features don't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class EnhancedPresenceTrackingServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Captor
    private ArgumentCaptor<WebSocketMessage<PresenceUpdate>> messageCaptor;

    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    private PresenceTrackingService presenceTrackingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        presenceTrackingService = new PresenceTrackingService(messagingTemplate, redisTemplate);
    }

    @Test
    void shouldTrackConnectionLifecycleEvents() {
        // Given
        Long userId = 1001L;
        String sessionId = "session-123";
        Long hiveId = 100L;

        // When - User connects
        presenceTrackingService.handleUserConnection(userId, sessionId, hiveId);

        // Then
        verify(valueOperations).set(
            eq("presence:user:" + userId),
            any(PresenceUpdate.class),
            any(Duration.class)
        );
        verify(setOperations).add(eq("presence:hive:" + hiveId), eq(userId));
        verify(messagingTemplate).convertAndSend(
            eq("/topic/presence"),
            any(WebSocketMessage.class)
        );

        // When - User disconnects
        presenceTrackingService.handleUserDisconnection(userId, sessionId);

        // Then
        verify(redisTemplate).delete("presence:user:" + userId);
        verify(setOperations).remove(eq("presence:hive:" + hiveId), eq(userId));
    }

    @Test
    void shouldHandleBatchPresenceUpdates() {
        // Given
        Map<Long, PresenceUpdate.PresenceStatus> batchUpdates = Map.of(
            1001L, PresenceUpdate.PresenceStatus.ONLINE,
            1002L, PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION,
            1003L, PresenceUpdate.PresenceStatus.AWAY
        );

        // When
        presenceTrackingService.updateBatchPresence(batchUpdates, 100L);

        // Then
        verify(valueOperations, times(3)).set(
            anyString(),
            any(PresenceUpdate.class),
            any(Duration.class)
        );
        verify(messagingTemplate, times(3)).convertAndSend(
            anyString(),
            any(WebSocketMessage.class)
        );
    }

    @Test
    void shouldSynchronizePresenceAcrossInstances() {
        // Given
        Long userId = 2001L;
        PresenceUpdate localPresence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.ONLINE)
            .hiveId(200L)
            .lastSeen(LocalDateTime.now())
            .build();

        when(valueOperations.get("presence:user:" + userId)).thenReturn(localPresence);

        // When
        PresenceUpdate syncedPresence = presenceTrackingService.synchronizePresence(userId);

        // Then
        assertThat(syncedPresence).isNotNull();
        assertThat(syncedPresence.getUserId()).isEqualTo(userId);
        assertThat(syncedPresence.getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.ONLINE);
        verify(valueOperations).set(
            eq("presence:sync:" + userId),
            any(PresenceUpdate.class),
            any(Duration.class)
        );
    }

    @Test
    void shouldRecoverPresenceStateAfterReconnection() {
        // Given
        Long userId = 3001L;
        String newSessionId = "new-session-456";
        PresenceUpdate previousState = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION)
            .hiveId(300L)
            .currentActivity("Deep Work")
            .focusMinutesRemaining(15)
            .lastSeen(LocalDateTime.now().minusMinutes(2))
            .build();

        when(valueOperations.get("presence:recovery:" + userId)).thenReturn(previousState);

        // When
        PresenceUpdate recoveredState = presenceTrackingService.recoverPresenceState(userId, newSessionId);

        // Then
        assertThat(recoveredState).isNotNull();
        assertThat(recoveredState.getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION);
        assertThat(recoveredState.getFocusMinutesRemaining()).isEqualTo(13); // Adjusted for elapsed time
        verify(valueOperations).set(
            eq("presence:user:" + userId),
            any(PresenceUpdate.class),
            any(Duration.class)
        );
    }

    @Test
    void shouldTrackPresenceMetrics() {
        // Given
        Long userId = 4001L;

        // When - Multiple presence updates
        presenceTrackingService.updateUserPresence(userId,
            PresenceUpdate.PresenceStatus.ONLINE, 400L, "Working");
        presenceTrackingService.updateUserPresence(userId,
            PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION, 400L, "Focus");
        presenceTrackingService.updateUserPresence(userId,
            PresenceUpdate.PresenceStatus.AWAY, 400L, null);

        // Then
        PresenceMetrics metrics = presenceTrackingService.getPresenceMetrics();
        assertThat(metrics.getTotalUpdates()).isEqualTo(3);
        assertThat(metrics.getActiveUsers()).isGreaterThan(0);
        assertThat(metrics.getAverageSessionDuration()).isNotNull();
    }

    @Test
    void shouldHandlePresenceConflictResolution() {
        // Given
        Long userId = 5001L;
        String session1 = "session-1";
        String session2 = "session-2";

        // When - Same user connects from multiple sessions
        presenceTrackingService.handleUserConnection(userId, session1, 500L);
        presenceTrackingService.handleUserConnection(userId, session2, 501L);

        // Then - Should handle conflict and maintain single presence
        verify(valueOperations, atLeast(2)).set(
            eq("presence:user:" + userId),
            any(PresenceUpdate.class),
            any(Duration.class)
        );

        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);
        assertThat(presence).isNotNull();
        assertThat(presence.getActiveSessionCount()).isEqualTo(2);
    }

    @Test
    void shouldPrioritizePresenceUpdates() {
        // Given
        Long userId = 6001L;
        Queue<PresenceUpdate> priorityQueue = new PriorityQueue<>();

        // When - Updates with different priorities
        presenceTrackingService.queuePresenceUpdate(userId,
            PresenceUpdate.PresenceStatus.ONLINE, PresencePriority.LOW);
        presenceTrackingService.queuePresenceUpdate(userId,
            PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION, PresencePriority.HIGH);
        presenceTrackingService.queuePresenceUpdate(userId,
            PresenceUpdate.PresenceStatus.AWAY, PresencePriority.NORMAL);

        // Then - Process in priority order
        presenceTrackingService.processPresenceQueue();

        ArgumentCaptor<PresenceUpdate> updateCaptor = ArgumentCaptor.forClass(PresenceUpdate.class);
        verify(valueOperations, atLeast(3)).set(
            anyString(),
            updateCaptor.capture(),
            any(Duration.class)
        );

        List<PresenceUpdate> updates = updateCaptor.getAllValues();
        // First update should be HIGH priority (IN_FOCUS_SESSION)
        assertThat(updates.get(0).getStatus()).isEqualTo(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION);
    }

    @Test
    void shouldCleanupStalePresenceData() {
        // Given
        Map<Long, LocalDateTime> staleUsers = Map.of(
            7001L, LocalDateTime.now().minusMinutes(20),
            7002L, LocalDateTime.now().minusMinutes(30),
            7003L, LocalDateTime.now().minusMinutes(5) // Not stale
        );

        // Mock presence cache
        presenceTrackingService.populatePresenceCache(staleUsers);

        // When
        int cleanedCount = presenceTrackingService.cleanupStalePresence();

        // Then
        assertThat(cleanedCount).isEqualTo(2);
        verify(redisTemplate, times(2)).delete(anyString());
        verify(messagingTemplate, times(2)).convertAndSend(
            eq("/topic/presence"),
            any(WebSocketMessage.class)
        );
    }

    @Test
    void shouldHandlePresenceSubscriptionManagement() {
        // Given
        Long userId = 8001L;
        Set<Long> subscribedHives = Set.of(800L, 801L, 802L);

        // When
        presenceTrackingService.subscribeToHivePresence(userId, subscribedHives);

        // Then
        verify(setOperations, times(3)).add(anyString(), eq(userId));

        // When - Unsubscribe from one hive
        presenceTrackingService.unsubscribeFromHivePresence(userId, 801L);

        // Then
        verify(setOperations).remove(eq("presence:hive:801"), eq(userId));
    }

    @Test
    void shouldProvidePresenceSnapshot() {
        // Given
        Set<Object> hiveUsers = Set.of(9001L, 9002L, 9003L);
        when(setOperations.members("presence:hive:900")).thenReturn(hiveUsers);

        // Mock individual presence
        hiveUsers.forEach(userId -> {
            when(valueOperations.get("presence:user:" + userId)).thenReturn(
                PresenceUpdate.builder()
                    .userId(Long.parseLong(userId.toString()))
                    .status(PresenceUpdate.PresenceStatus.ONLINE)
                    .build()
            );
        });

        // When
        PresenceSnapshot snapshot = presenceTrackingService.getPresenceSnapshot(900L);

        // Then
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getHiveId()).isEqualTo(900L);
        assertThat(snapshot.getOnlineUsers()).hasSize(3);
        assertThat(snapshot.getTotalUsers()).isEqualTo(3);
        assertThat(snapshot.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCalculatePresenceStatistics() {
        // Given
        Long hiveId = 1000L;
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        // When
        PresenceStatistics stats = presenceTrackingService.calculatePresenceStatistics(
            hiveId, startTime, endTime);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getAverageOnlineUsers()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getPeakOnlineUsers()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getTotalSessionTime()).isNotNull();
        assertThat(stats.getMostActiveHour()).isNotNull();
    }

    // Helper classes for enhanced testing

    static class PresenceMetrics {
        private long totalUpdates;
        private int activeUsers;
        private Duration averageSessionDuration;

        // Getters and setters
        public long getTotalUpdates() { return totalUpdates; }
        public int getActiveUsers() { return activeUsers; }
        public Duration getAverageSessionDuration() { return averageSessionDuration; }
    }

    enum PresencePriority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    static class PresenceSnapshot {
        private Long hiveId;
        private List<PresenceUpdate> onlineUsers;
        private int totalUsers;
        private LocalDateTime timestamp;

        // Getters and setters
        public Long getHiveId() { return hiveId; }
        public List<PresenceUpdate> getOnlineUsers() { return onlineUsers; }
        public int getTotalUsers() { return totalUsers; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    static class PresenceStatistics {
        private double averageOnlineUsers;
        private int peakOnlineUsers;
        private Duration totalSessionTime;
        private Integer mostActiveHour;

        // Getters and setters
        public double getAverageOnlineUsers() { return averageOnlineUsers; }
        public int getPeakOnlineUsers() { return peakOnlineUsers; }
        public Duration getTotalSessionTime() { return totalSessionTime; }
        public Integer getMostActiveHour() { return mostActiveHour; }
    }
}