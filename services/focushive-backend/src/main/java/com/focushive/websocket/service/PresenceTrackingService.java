package com.focushive.websocket.service;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class PresenceTrackingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final String HIVE_PRESENCE_KEY_PREFIX = "presence:hive:";
    private static final Duration PRESENCE_TTL = Duration.ofMinutes(5);
    private static final Duration AWAY_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration OFFLINE_THRESHOLD = Duration.ofMinutes(15);
    
    // In-memory cache for quick access
    private final Map<Long, PresenceUpdate> presenceCache = new ConcurrentHashMap<>();

    // Health monitoring metrics
    private long totalPresenceUpdates = 0;

    // Connection management
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> userSubscriptions = new ConcurrentHashMap<>();

    // Metrics tracking
    private int totalConnections = 0;
    private int activeConnections = 0;
    
    /**
     * Update user presence status
     */
    public void updateUserPresence(Long userId, PresenceUpdate.PresenceStatus status, Long hiveId, String activity) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        
        PresenceUpdate presence = PresenceUpdate.builder()
            .userId(userId)
            .status(status)
            .hiveId(hiveId)
            .currentActivity(activity)
            .lastSeen(LocalDateTime.now())
            .build();
        
        // Store in Redis with TTL
        redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL);
        
        // Update cache
        presenceCache.put(userId, presence);

        // If user is in a hive, update hive presence
        if (hiveId != null) {
            updateHivePresence(hiveId, userId, status);
        }

        // Increment metrics
        totalPresenceUpdates++;

        // Broadcast presence update
        broadcastPresenceUpdate(presence);
    }
    
    /**
     * Update user activity/heartbeat
     */
    public void updateUserActivity(Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        
        // Get existing presence or create new one
        PresenceUpdate presence = (PresenceUpdate) redisTemplate.opsForValue().get(presenceKey);
        if (presence == null) {
            presence = PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.ONLINE)
                .lastSeen(LocalDateTime.now())
                .build();
        } else {
            presence.setLastSeen(LocalDateTime.now());
            
            // Auto-update status based on activity
            if (presence.getStatus() == PresenceUpdate.PresenceStatus.AWAY) {
                presence.setStatus(PresenceUpdate.PresenceStatus.ONLINE);
                broadcastPresenceUpdate(presence);
            }
        }
        
        // Update Redis with new TTL
        redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL);
        presenceCache.put(userId, presence);
    }
    
    /**
     * Set user as focusing
     */
    public void startFocusSession(Long userId, Long hiveId, Integer focusMinutes) {
        PresenceUpdate presence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION)
            .hiveId(hiveId)
            .currentActivity("Focus Session")
            .focusMinutesRemaining(focusMinutes)
            .lastSeen(LocalDateTime.now())
            .build();
        
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(presenceKey, presence, Duration.ofMinutes(focusMinutes + 5));
        presenceCache.put(userId, presence);
        
        broadcastPresenceUpdate(presence);
    }
    
    /**
     * Set user as in buddy session
     */
    public void startBuddySession(Long userId, Long buddyId) {
        PresenceUpdate presence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.IN_BUDDY_SESSION)
            .currentActivity("Buddy Session with User " + buddyId)
            .lastSeen(LocalDateTime.now())
            .build();
        
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(presenceKey, presence, Duration.ofHours(2));
        presenceCache.put(userId, presence);
        
        broadcastPresenceUpdate(presence);
    }
    
    /**
     * Get user presence
     */
    public PresenceUpdate getUserPresence(Long userId) {
        // Try cache first
        PresenceUpdate cached = presenceCache.get(userId);
        if (cached != null && isPresenceValid(cached)) {
            // Update session count
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                cached.setActiveSessionCount(sessions.size());
            }
            return cached;
        }

        // Fallback to Redis
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        PresenceUpdate presence = (PresenceUpdate) redisTemplate.opsForValue().get(presenceKey);

        if (presence == null) {
            // User is offline
            presence = PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.OFFLINE)
                .lastSeen(LocalDateTime.now())
                .activeSessionCount(0)
                .build();
        } else {
            // Update session count
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                presence.setActiveSessionCount(sessions.size());
            }
            // Update cache
            presenceCache.put(userId, presence);
        }

        return presence;
    }
    
    /**
     * Get all online users in a hive
     */
    public List<PresenceUpdate> getHivePresence(Long hiveId) {
        String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
        Set<Object> userIds = redisTemplate.opsForSet().members(hiveKey);
        
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return userIds.stream()
            .map(id -> getUserPresence(Long.parseLong(id.toString())))
            .filter(p -> p.getStatus() != PresenceUpdate.PresenceStatus.OFFLINE)
            .collect(Collectors.toList());
    }
    
    /**
     * Get count of online users in a hive
     */
    public long getHiveOnlineCount(Long hiveId) {
        String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
        Long count = redisTemplate.opsForSet().size(hiveKey);
        return count != null ? count : 0;
    }
    
    /**
     * Remove user from all presence tracking
     */
    public void removeUserPresence(Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        PresenceUpdate presence = (PresenceUpdate) redisTemplate.opsForValue().get(presenceKey);
        
        // Remove from Redis
        redisTemplate.delete(presenceKey);
        
        // Remove from hive if present
        if (presence != null && presence.getHiveId() != null) {
            removeFromHivePresence(presence.getHiveId(), userId);
        }
        
        // Remove from cache
        presenceCache.remove(userId);
        
        // Broadcast offline status
        PresenceUpdate offlinePresence = PresenceUpdate.builder()
            .userId(userId)
            .status(PresenceUpdate.PresenceStatus.OFFLINE)
            .lastSeen(LocalDateTime.now())
            .build();
        
        broadcastPresenceUpdate(offlinePresence);
    }
    
    /**
     * Scheduled task to check for inactive users
     */
    @Scheduled(fixedDelay = 60000) // Check every minute
    public void checkInactiveUsers() {
        LocalDateTime now = LocalDateTime.now();
        
        presenceCache.entrySet().removeIf(entry -> {
            PresenceUpdate presence = entry.getValue();
            Duration inactiveDuration = Duration.between(presence.getLastSeen(), now);
            
            // Mark as away after 5 minutes of inactivity
            if (inactiveDuration.compareTo(AWAY_THRESHOLD) > 0 && 
                presence.getStatus() == PresenceUpdate.PresenceStatus.ONLINE) {
                
                presence.setStatus(PresenceUpdate.PresenceStatus.AWAY);
                updateUserPresence(presence.getUserId(), PresenceUpdate.PresenceStatus.AWAY, 
                    presence.getHiveId(), presence.getCurrentActivity());
                return false;
            }
            
            // Mark as offline after 15 minutes of inactivity
            if (inactiveDuration.compareTo(OFFLINE_THRESHOLD) > 0) {
                removeUserPresence(presence.getUserId());
                return true; // Remove from cache
            }
            
            return false;
        });
    }
    
    /**
     * Scheduled task to update focus session remaining time
     */
    @Scheduled(fixedDelay = 60000) // Update every minute
    public void updateFocusSessionTimes() {
        presenceCache.values().stream()
            .filter(p -> p.getStatus() == PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION)
            .filter(p -> p.getFocusMinutesRemaining() != null && p.getFocusMinutesRemaining() > 0)
            .forEach(presence -> {
                presence.setFocusMinutesRemaining(presence.getFocusMinutesRemaining() - 1);
                
                if (presence.getFocusMinutesRemaining() <= 0) {
                    // Focus session ended
                    presence.setStatus(PresenceUpdate.PresenceStatus.ONLINE);
                    presence.setFocusMinutesRemaining(null);
                    presence.setCurrentActivity(null);
                }
                
                // Update in Redis
                String presenceKey = PRESENCE_KEY_PREFIX + presence.getUserId();
                redisTemplate.opsForValue().set(presenceKey, presence, PRESENCE_TTL);
                
                // Broadcast update
                broadcastPresenceUpdate(presence);
            });
    }
    
    // Helper method to update hive presence
    private void updateHivePresence(Long hiveId, Long userId, PresenceUpdate.PresenceStatus status) {
        String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
        
        if (status == PresenceUpdate.PresenceStatus.OFFLINE) {
            redisTemplate.opsForSet().remove(hiveKey, userId);
        } else {
            redisTemplate.opsForSet().add(hiveKey, userId);
            redisTemplate.expire(hiveKey, 1, TimeUnit.HOURS);
        }
    }
    
    // Helper method to remove user from hive presence
    private void removeFromHivePresence(Long hiveId, Long userId) {
        String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
        redisTemplate.opsForSet().remove(hiveKey, userId);
    }
    
    // Helper method to check if presence is still valid
    private boolean isPresenceValid(PresenceUpdate presence) {
        Duration age = Duration.between(presence.getLastSeen(), LocalDateTime.now());
        return age.compareTo(OFFLINE_THRESHOLD) < 0;
    }
    
    // Helper method to broadcast presence update
    private void broadcastPresenceUpdate(PresenceUpdate presence) {
        WebSocketMessage<PresenceUpdate> message = WebSocketMessage.<PresenceUpdate>builder()
            .id(UUID.randomUUID().toString())
            .type(determineMessageType(presence.getStatus()))
            .event("presence.update")
            .payload(presence)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Broadcast to general presence topic
        messagingTemplate.convertAndSend("/topic/presence", message);
        
        // If user is in a hive, also broadcast to hive-specific topic
        if (presence.getHiveId() != null) {
            messagingTemplate.convertAndSend("/topic/hive/" + presence.getHiveId() + "/presence", message);
        }
    }
    
    // Helper method to determine message type based on status
    private WebSocketMessage.MessageType determineMessageType(PresenceUpdate.PresenceStatus status) {
        switch (status) {
            case ONLINE:
            case IN_FOCUS_SESSION:
            case IN_BUDDY_SESSION:
                return WebSocketMessage.MessageType.USER_ONLINE;
            case AWAY:
            case BUSY:
            case DO_NOT_DISTURB:
                return WebSocketMessage.MessageType.USER_AWAY;
            case OFFLINE:
                return WebSocketMessage.MessageType.USER_OFFLINE;
            default:
                return WebSocketMessage.MessageType.USER_ONLINE;
        }
    }

    /**
     * Get count of active WebSocket connections (for health monitoring).
     *
     * @return the number of active connections
     */
    public int getActiveConnectionCount() {
        return presenceCache.size();
    }

    /**
     * Get total number of presence updates processed (for health monitoring).
     *
     * @return the total number of presence updates
     */
    public long getTotalPresenceUpdates() {
        return totalPresenceUpdates;
    }

    /**
     * Handle user connection event
     */
    public void handleUserConnection(Long userId, String sessionId, Long hiveId) {
        log.info("User {} connected with session {} in hive {}", userId, sessionId, hiveId);

        // Track session
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Update connection metrics
        totalConnections++;
        activeConnections++;

        // Store recovery data
        String recoveryKey = "presence:recovery:" + userId;
        PresenceUpdate currentPresence = getUserPresence(userId);
        if (currentPresence != null) {
            redisTemplate.opsForValue().set(recoveryKey, currentPresence, Duration.ofHours(1));
        }

        // Update presence if hive specified
        if (hiveId != null) {
            updateUserPresence(userId, PresenceUpdate.PresenceStatus.ONLINE, hiveId, null);
        }
    }

    /**
     * Handle user disconnection event
     */
    public void handleUserDisconnection(Long userId, String sessionId) {
        log.info("User {} disconnected with session {}", userId, sessionId);

        // Remove session
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                // Only mark offline if no other sessions exist
                removeUserPresence(userId);
            }
        }

        // Update metrics
        activeConnections = Math.max(0, activeConnections - 1);
    }

    /**
     * Update batch presence for multiple users
     */
    public void updateBatchPresence(Map<Long, PresenceUpdate.PresenceStatus> updates, Long hiveId) {
        log.info("Batch updating presence for {} users", updates.size());

        updates.forEach((userId, status) -> {
            updateUserPresence(userId, status, hiveId, null);
        });
    }

    /**
     * Synchronize presence across instances
     */
    public PresenceUpdate synchronizePresence(Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        String syncKey = "presence:sync:" + userId;

        PresenceUpdate presence = (PresenceUpdate) redisTemplate.opsForValue().get(presenceKey);
        if (presence != null) {
            // Store sync state
            redisTemplate.opsForValue().set(syncKey, presence, Duration.ofMinutes(5));
            return presence;
        }

        return null;
    }

    /**
     * Recover presence state after reconnection
     */
    public PresenceUpdate recoverPresenceState(Long userId, String newSessionId) {
        log.info("Recovering presence state for user {} with new session {}", userId, newSessionId);

        String recoveryKey = "presence:recovery:" + userId;
        PresenceUpdate previousState = (PresenceUpdate) redisTemplate.opsForValue().get(recoveryKey);

        if (previousState != null) {
            // Adjust for elapsed time
            if (previousState.getStatus() == PresenceUpdate.PresenceStatus.IN_FOCUS_SESSION
                && previousState.getFocusMinutesRemaining() != null) {

                Duration elapsed = Duration.between(previousState.getLastSeen(), LocalDateTime.now());
                int elapsedMinutes = (int) elapsed.toMinutes();
                int remainingMinutes = Math.max(0, previousState.getFocusMinutesRemaining() - elapsedMinutes);
                previousState.setFocusMinutesRemaining(remainingMinutes);
            }

            // Update with new session
            previousState.setLastSeen(LocalDateTime.now());
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(newSessionId);

            // Restore presence
            String presenceKey = PRESENCE_KEY_PREFIX + userId;
            redisTemplate.opsForValue().set(presenceKey, previousState, PRESENCE_TTL);
            presenceCache.put(userId, previousState);

            // Broadcast recovery
            broadcastPresenceUpdate(previousState);

            return previousState;
        }

        return null;
    }

    /**
     * Subscribe to hive presence updates
     */
    public void subscribeToHivePresence(Long userId, Set<Long> hiveIds) {
        log.debug("User {} subscribing to hives: {}", userId, hiveIds);

        userSubscriptions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).addAll(hiveIds);

        // Add user to hive presence sets
        hiveIds.forEach(hiveId -> {
            String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
            redisTemplate.opsForSet().add(hiveKey, userId);
            redisTemplate.expire(hiveKey, 1, TimeUnit.HOURS);
        });
    }

    /**
     * Unsubscribe from hive presence updates
     */
    public void unsubscribeFromHivePresence(Long userId, Long hiveId) {
        log.debug("User {} unsubscribing from hive {}", userId, hiveId);

        Set<Long> subscriptions = userSubscriptions.get(userId);
        if (subscriptions != null) {
            subscriptions.remove(hiveId);
            if (subscriptions.isEmpty()) {
                userSubscriptions.remove(userId);
            }
        }

        // Remove from hive presence set
        String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
        redisTemplate.opsForSet().remove(hiveKey, userId);
    }

    /**
     * Cleanup user subscriptions
     */
    public void cleanupUserSubscriptions(Long userId) {
        log.info("Cleaning up subscriptions for user {}", userId);

        Set<Long> subscriptions = userSubscriptions.remove(userId);
        if (subscriptions != null) {
            subscriptions.forEach(hiveId -> {
                String hiveKey = HIVE_PRESENCE_KEY_PREFIX + hiveId;
                redisTemplate.opsForSet().remove(hiveKey, userId);
            });
        }
    }

    /**
     * Get presence metrics
     */
    public PresenceMetrics getPresenceMetrics() {
        PresenceMetrics metrics = new PresenceMetrics();
        metrics.setTotalUpdates(totalPresenceUpdates);
        metrics.setActiveUsers(presenceCache.size());
        metrics.setTotalConnections(totalConnections);
        metrics.setActiveConnections(activeConnections);

        // Calculate average session duration
        Duration totalDuration = Duration.ZERO;
        int sessionCount = 0;
        for (PresenceUpdate presence : presenceCache.values()) {
            if (presence.getLastSeen() != null) {
                Duration sessionDuration = Duration.between(presence.getLastSeen(), LocalDateTime.now());
                totalDuration = totalDuration.plus(sessionDuration);
                sessionCount++;
            }
        }

        if (sessionCount > 0) {
            metrics.setAverageSessionDuration(totalDuration.dividedBy(sessionCount));
        }

        return metrics;
    }

    /**
     * Queue presence update with priority
     */
    private final PriorityQueue<PriorityPresenceUpdate> updateQueue = new PriorityQueue<>();

    public void queuePresenceUpdate(Long userId, PresenceUpdate.PresenceStatus status, PresencePriority priority) {
        PriorityPresenceUpdate update = new PriorityPresenceUpdate(userId, status, priority);
        updateQueue.offer(update);
    }

    /**
     * Process queued presence updates
     */
    public void processPresenceQueue() {
        while (!updateQueue.isEmpty()) {
            PriorityPresenceUpdate update = updateQueue.poll();
            updateUserPresence(update.userId, update.status, null, null);
        }
    }

    /**
     * Populate presence cache for testing
     */
    public void populatePresenceCache(Map<Long, LocalDateTime> users) {
        users.forEach((userId, lastSeen) -> {
            PresenceUpdate presence = PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.ONLINE)
                .lastSeen(lastSeen)
                .build();
            presenceCache.put(userId, presence);
        });
    }

    /**
     * Cleanup stale presence data
     */
    public int cleanupStalePresence() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(15);
        int cleanedCount = 0;

        Iterator<Map.Entry<Long, PresenceUpdate>> iterator = presenceCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PresenceUpdate> entry = iterator.next();
            if (entry.getValue().getLastSeen().isBefore(staleThreshold)) {
                Long userId = entry.getKey();
                iterator.remove();

                // Remove from Redis
                String presenceKey = PRESENCE_KEY_PREFIX + userId;
                redisTemplate.delete(presenceKey);

                // Broadcast offline status
                PresenceUpdate offlinePresence = PresenceUpdate.builder()
                    .userId(userId)
                    .status(PresenceUpdate.PresenceStatus.OFFLINE)
                    .lastSeen(LocalDateTime.now())
                    .build();
                broadcastPresenceUpdate(offlinePresence);

                cleanedCount++;
            }
        }

        return cleanedCount;
    }

    /**
     * Get presence snapshot for a hive
     */
    public PresenceSnapshot getPresenceSnapshot(Long hiveId) {
        PresenceSnapshot snapshot = new PresenceSnapshot();
        snapshot.setHiveId(hiveId);
        snapshot.setTimestamp(LocalDateTime.now());

        List<PresenceUpdate> onlineUsers = getHivePresence(hiveId);
        snapshot.setOnlineUsers(onlineUsers);
        snapshot.setTotalUsers(onlineUsers.size());

        return snapshot;
    }

    /**
     * Calculate presence statistics
     */
    public PresenceStatistics calculatePresenceStatistics(Long hiveId, LocalDateTime startTime, LocalDateTime endTime) {
        PresenceStatistics stats = new PresenceStatistics();

        // This would typically query historical data from a time-series database
        // For now, return mock statistics
        stats.setAverageOnlineUsers(5.5);
        stats.setPeakOnlineUsers(10);
        stats.setTotalSessionTime(Duration.ofHours(24));
        stats.setMostActiveHour(14); // 2 PM

        return stats;
    }

    // Helper classes
    public static class PresenceMetrics {
        private long totalUpdates;
        private int activeUsers;
        private int totalConnections;
        private int activeConnections;
        private Duration averageSessionDuration;

        // Getters and setters
        public long getTotalUpdates() { return totalUpdates; }
        public void setTotalUpdates(long totalUpdates) { this.totalUpdates = totalUpdates; }
        public int getActiveUsers() { return activeUsers; }
        public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
        public int getTotalConnections() { return totalConnections; }
        public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        public Duration getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(Duration averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }
    }

    public enum PresencePriority {
        LOW(0), NORMAL(1), HIGH(2), CRITICAL(3);

        private final int value;
        PresencePriority(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    private static class PriorityPresenceUpdate implements Comparable<PriorityPresenceUpdate> {
        private final Long userId;
        private final PresenceUpdate.PresenceStatus status;
        private final PresencePriority priority;

        public PriorityPresenceUpdate(Long userId, PresenceUpdate.PresenceStatus status, PresencePriority priority) {
            this.userId = userId;
            this.status = status;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityPresenceUpdate other) {
            return Integer.compare(other.priority.getValue(), this.priority.getValue());
        }
    }

    public static class PresenceSnapshot {
        private Long hiveId;
        private List<PresenceUpdate> onlineUsers;
        private int totalUsers;
        private LocalDateTime timestamp;

        // Getters and setters
        public Long getHiveId() { return hiveId; }
        public void setHiveId(Long hiveId) { this.hiveId = hiveId; }
        public List<PresenceUpdate> getOnlineUsers() { return onlineUsers; }
        public void setOnlineUsers(List<PresenceUpdate> onlineUsers) { this.onlineUsers = onlineUsers; }
        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class PresenceStatistics {
        private double averageOnlineUsers;
        private int peakOnlineUsers;
        private Duration totalSessionTime;
        private Integer mostActiveHour;

        // Getters and setters
        public double getAverageOnlineUsers() { return averageOnlineUsers; }
        public void setAverageOnlineUsers(double averageOnlineUsers) { this.averageOnlineUsers = averageOnlineUsers; }
        public int getPeakOnlineUsers() { return peakOnlineUsers; }
        public void setPeakOnlineUsers(int peakOnlineUsers) { this.peakOnlineUsers = peakOnlineUsers; }
        public Duration getTotalSessionTime() { return totalSessionTime; }
        public void setTotalSessionTime(Duration totalSessionTime) { this.totalSessionTime = totalSessionTime; }
        public Integer getMostActiveHour() { return mostActiveHour; }
        public void setMostActiveHour(Integer mostActiveHour) { this.mostActiveHour = mostActiveHour; }
    }
}