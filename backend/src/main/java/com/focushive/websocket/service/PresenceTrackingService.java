package com.focushive.websocket.service;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        
        // Broadcast presence update
        broadcastPresenceUpdate(presence);
        
        log.debug("Updated presence for user {}: {}", userId, status);
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
                .build();
        } else {
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
}