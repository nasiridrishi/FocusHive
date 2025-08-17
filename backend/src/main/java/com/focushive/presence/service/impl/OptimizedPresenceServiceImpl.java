package com.focushive.presence.service.impl;

import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance-optimized implementation of PresenceService with:
 * - Message batching and throttling
 * - Cache-friendly operations
 * - Reduced Redis calls
 * - Background cleanup optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedPresenceServiceImpl implements PresenceService {
    
    private static final String USER_PRESENCE_KEY = "presence:user:";
    private static final String HIVE_PRESENCE_KEY = "presence:hive:";
    private static final String SESSION_KEY = "presence:session:";
    private static final String USER_SESSION_KEY = "presence:user:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final HiveMemberRepository hiveMemberRepository;
    
    // Performance optimization: Batch presence updates
    private final Map<String, Long> lastBroadcastTime = new ConcurrentHashMap<>();
    private final AtomicLong operationCount = new AtomicLong(0);
    
    @Value("${presence.heartbeat.timeout-seconds:30}")
    private int heartbeatTimeoutSeconds;
    
    @Value("${presence.broadcast.throttle-ms:1000}")
    private long broadcastThrottleMs = 1000; // Throttle broadcasts to max 1 per second per user
    
    @Value("${presence.batch.max-size:50}")
    private int batchMaxSize = 50;

    @Override
    public UserPresence updateUserPresence(String userId, PresenceUpdate update) {
        long opCount = operationCount.incrementAndGet();
        
        // Create user presence
        UserPresence presence = UserPresence.builder()
                .userId(userId)
                .status(update.status())
                .activity(update.activity() != null ? update.activity() : "Available")
                .lastSeen(Instant.now())
                .currentHiveId(update.hiveId())
                .build();
        
        // Batch Redis operations
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.opsForValue().set(key, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        
        // Update heartbeat (optimized to avoid duplicate Redis calls)
        recordHeartbeatOptimized(userId, presence);
        
        // Throttled broadcast to avoid spam
        if (shouldBroadcast(userId)) {
            broadcastPresenceUpdate(presence, update.hiveId());
            lastBroadcastTime.put(userId, System.currentTimeMillis());
        }
        
        // Log performance metrics every 100 operations
        if (opCount % 100 == 0) {
            log.debug("Presence operations processed: {}", opCount);
        }
        
        return presence;
    }

    @Override
    @Cacheable(value = "userPresence", key = "#userId", unless = "#result == null")
    public UserPresence getUserPresence(String userId) {
        String key = USER_PRESENCE_KEY + userId;
        return (UserPresence) redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public void recordHeartbeat(String userId) {
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            recordHeartbeatOptimized(userId, presence);
        }
    }
    
    private void recordHeartbeatOptimized(String userId, UserPresence presence) {
        // Update last seen timestamp
        presence.setLastSeen(Instant.now());
        
        // Batch update - only update Redis if significantly stale
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.opsForValue().set(key, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
    }
    
    private boolean shouldBroadcast(String userId) {
        Long lastTime = lastBroadcastTime.get(userId);
        if (lastTime == null) {
            return true;
        }
        return (System.currentTimeMillis() - lastTime) > broadcastThrottleMs;
    }

    @Override
    public HivePresenceInfo joinHivePresence(String hiveId, String userId) {
        log.debug("User {} joining hive {} presence", userId, hiveId);
        
        // Cached verification of hive membership
        if (!isHiveMember(hiveId, userId)) {
            throw new IllegalArgumentException("User is not a member of this hive");
        }
        
        // Optimized Redis operations using pipeline
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> hiveUsers = getHiveUserIds(hiveId);
        hiveUsers.add(userId);
        redisTemplate.opsForValue().set(key, hiveUsers, 1, TimeUnit.HOURS);
        
        // Build presence info with caching
        HivePresenceInfo info = buildHivePresenceInfoOptimized(hiveId);
        
        // Throttled broadcast
        if (shouldBroadcastHive(hiveId)) {
            messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/presence", info);
        }
        
        return info;
    }

    @Override
    public HivePresenceInfo leaveHivePresence(String hiveId, String userId) {
        log.debug("User {} leaving hive {} presence", userId, hiveId);
        
        // Optimized Redis operations
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> hiveUsers = getHiveUserIds(hiveId);
        hiveUsers.remove(userId);
        
        if (hiveUsers.isEmpty()) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, hiveUsers, 1, TimeUnit.HOURS);
        }
        
        // Build presence info with caching
        HivePresenceInfo info = buildHivePresenceInfoOptimized(hiveId);
        
        // Throttled broadcast
        if (shouldBroadcastHive(hiveId)) {
            messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/presence", info);
        }
        
        return info;
    }

    @Override
    @Cacheable(value = "hiveActiveUsers", key = "#hiveId")
    public List<UserPresence> getHiveActiveUsers(String hiveId) {
        Set<String> userIds = getHiveUserIds(hiveId);
        
        // Batch fetch user presences using pipeline
        List<UserPresence> activeUsers = new ArrayList<>();
        List<String> keys = userIds.stream()
                .map(userId -> USER_PRESENCE_KEY + userId)
                .toList();
        
        if (!keys.isEmpty()) {
            List<Object> presences = redisTemplate.opsForValue().multiGet(keys);
            if (presences != null) {
                for (Object presence : presences) {
                    if (presence instanceof UserPresence userPresence) {
                        activeUsers.add(userPresence);
                    }
                }
            }
        }
        
        return activeUsers;
    }

    @Override
    public Map<String, HivePresenceInfo> getHivesPresenceInfo(Set<String> hiveIds) {
        Map<String, HivePresenceInfo> result = new HashMap<>();
        
        // Batch process hive presence info
        for (String hiveId : hiveIds) {
            result.put(hiveId, buildHivePresenceInfoOptimized(hiveId));
        }
        
        return result;
    }

    @Override
    public FocusSession startFocusSession(String userId, String hiveId, int durationMinutes) {
        log.debug("Starting focus session for user {} in hive {} for {} minutes", userId, hiveId, durationMinutes);
        
        // Create new session
        FocusSession session = FocusSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .hiveId(hiveId)
                .startTime(Instant.now())
                .plannedDurationMinutes(durationMinutes)
                .type(FocusSession.SessionType.FOCUS)
                .status(FocusSession.SessionStatus.ACTIVE)
                .build();
        
        // Batch Redis operations
        String sessionKey = SESSION_KEY + session.getSessionId();
        String userSessionKey = USER_SESSION_KEY + userId + ":session";
        
        // Use pipeline for multiple operations
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            redisTemplate.opsForValue().set(sessionKey, session, durationMinutes * 2, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(userSessionKey, session.getSessionId(), durationMinutes * 2, TimeUnit.MINUTES);
            return null;
        });
        
        // Update user presence with session flag
        updateUserPresenceWithSession(userId, true);
        
        // Throttled broadcast
        if (hiveId != null && shouldBroadcastHive(hiveId)) {
            messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/sessions",
                    SessionBroadcast.builder()
                            .sessionId(session.getSessionId())
                            .userId(userId)
                            .hiveId(hiveId)
                            .type(SessionBroadcast.BroadcastType.SESSION_STARTED)
                            .session(session)
                            .build()
            );
        }
        
        return session;
    }

    @Override
    public FocusSession endFocusSession(String userId, String sessionId) {
        log.debug("Ending focus session {} for user {}", sessionId, userId);
        
        // Get session
        String sessionKey = SESSION_KEY + sessionId;
        FocusSession session = (FocusSession) redisTemplate.opsForValue().get(sessionKey);
        
        if (session == null || !session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session not found or not owned by user");
        }
        
        // Update session
        session.setEndTime(Instant.now());
        session.setStatus(FocusSession.SessionStatus.COMPLETED);
        
        // Calculate actual duration
        long actualMinutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        session.setActualDurationMinutes((int) actualMinutes);
        
        // Batch Redis operations
        String userSessionKey = USER_SESSION_KEY + userId + ":session";
        
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            redisTemplate.opsForValue().set(sessionKey, session, 1, TimeUnit.HOURS);
            redisTemplate.delete(userSessionKey);
            return null;
        });
        
        // Update user presence
        updateUserPresenceWithSession(userId, false);
        
        // Throttled broadcast
        if (session.getHiveId() != null && shouldBroadcastHive(session.getHiveId())) {
            messagingTemplate.convertAndSend(
                    "/topic/hive/" + session.getHiveId() + "/sessions",
                    SessionBroadcast.builder()
                            .sessionId(sessionId)
                            .userId(userId)
                            .hiveId(session.getHiveId())
                            .type(SessionBroadcast.BroadcastType.SESSION_ENDED)
                            .session(session)
                            .build()
            );
        }
        
        return session;
    }

    @Override
    @Cacheable(value = "activeFocusSession", key = "#userId")
    public FocusSession getActiveFocusSession(String userId) {
        String userSessionKey = USER_SESSION_KEY + userId + ":session";
        String sessionId = (String) redisTemplate.opsForValue().get(userSessionKey);
        
        if (sessionId == null) {
            return null;
        }
        
        String sessionKey = SESSION_KEY + sessionId;
        return (FocusSession) redisTemplate.opsForValue().get(sessionKey);
    }

    @Override
    @Cacheable(value = "hiveFocusSessions", key = "#hiveId")
    public List<FocusSession> getHiveFocusSessions(String hiveId) {
        List<FocusSession> sessions = new ArrayList<>();
        
        // Optimized: Use scan instead of keys for better performance
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(SESSION_KEY + "*")
                        .count(100)
                        .build())) {
            
            while (cursor.hasNext()) {
                String key = cursor.next();
                FocusSession session = (FocusSession) redisTemplate.opsForValue().get(key);
                if (session != null && hiveId.equals(session.getHiveId())) {
                    sessions.add(session);
                }
            }
        } catch (Exception e) {
            log.warn("Error scanning for focus sessions: {}", e.getMessage());
        }
        
        return sessions;
    }

    @Override
    @Scheduled(fixedDelay = 60000) // Reduced frequency to every 60 seconds
    public void cleanupStalePresence() {
        if (log.isDebugEnabled()) {
            log.debug("Running optimized stale presence cleanup");
        }
        
        Instant staleThreshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        int cleanedCount = 0;
        
        // Use scan for better performance than keys()
        try (var cursor = redisTemplate.scan(
                org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(USER_PRESENCE_KEY + "*")
                        .count(batchMaxSize)
                        .build())) {
            
            List<String> staleKeys = new ArrayList<>();
            List<UserPresence> stalePresences = new ArrayList<>();
            
            while (cursor.hasNext()) {
                String key = cursor.next();
                UserPresence presence = (UserPresence) redisTemplate.opsForValue().get(key);
                
                if (presence != null && presence.getLastSeen().isBefore(staleThreshold)) {
                    staleKeys.add(key);
                    stalePresences.add(presence);
                    
                    // Process in batches
                    if (staleKeys.size() >= batchMaxSize) {
                        processStaleBatch(staleKeys, stalePresences);
                        cleanedCount += staleKeys.size();
                        staleKeys.clear();
                        stalePresences.clear();
                    }
                }
            }
            
            // Process remaining batch
            if (!staleKeys.isEmpty()) {
                processStaleBatch(staleKeys, stalePresences);
                cleanedCount += staleKeys.size();
            }
            
        } catch (Exception e) {
            log.warn("Error during stale presence cleanup: {}", e.getMessage());
        }
        
        if (cleanedCount > 0) {
            log.info("Cleaned up {} stale presence records", cleanedCount);
        }
    }
    
    private void processStaleBatch(List<String> staleKeys, List<UserPresence> stalePresences) {
        // Batch delete stale keys
        redisTemplate.delete(staleKeys);
        
        // Remove from hives
        for (UserPresence presence : stalePresences) {
            if (presence.getCurrentHiveId() != null) {
                leaveHivePresence(presence.getCurrentHiveId(), presence.getUserId());
            }
        }
    }

    @Override
    public void markUserOffline(String userId) {
        log.debug("Marking user {} as offline", userId);
        
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            // Update status to offline
            presence.setStatus(PresenceStatus.OFFLINE);
            presence.setLastSeen(Instant.now());
            
            // Remove from any hives
            if (presence.getCurrentHiveId() != null) {
                leaveHivePresence(presence.getCurrentHiveId(), userId);
            }
            
            // End any active sessions
            FocusSession activeSession = getActiveFocusSession(userId);
            if (activeSession != null) {
                endFocusSession(userId, activeSession.getSessionId());
            }
            
            // Final broadcast and cleanup
            broadcastPresenceUpdate(presence, presence.getCurrentHiveId());
            
            // Delete presence
            String key = USER_PRESENCE_KEY + userId;
            redisTemplate.delete(key);
        }
    }
    
    // Helper methods
    
    private void broadcastPresenceUpdate(UserPresence presence, String hiveId) {
        PresenceBroadcast broadcast = PresenceBroadcast.builder()
                .userId(presence.getUserId())
                .status(presence.getStatus())
                .activity(presence.getActivity())
                .hiveId(hiveId)
                .type(PresenceBroadcast.BroadcastType.STATUS_CHANGED)
                .build();
        
        if (hiveId != null) {
            messagingTemplate.convertAndSend("/topic/hive/" + hiveId + "/presence", broadcast);
        }
        
        // Send to user's personal channel
        messagingTemplate.convertAndSendToUser(presence.getUserId(), "/queue/presence", broadcast);
    }
    
    @Cacheable(value = "hiveUserIds", key = "#hiveId")
    private Set<String> getHiveUserIds(String hiveId) {
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> userIds = (Set<String>) redisTemplate.opsForValue().get(key);
        return userIds != null ? userIds : new HashSet<>();
    }
    
    @Cacheable(value = "hiveMembership", key = "#hiveId + '_' + #userId")
    private boolean isHiveMember(String hiveId, String userId) {
        return hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId);
    }
    
    private boolean shouldBroadcastHive(String hiveId) {
        String key = "hive_broadcast_" + hiveId;
        Long lastTime = lastBroadcastTime.get(key);
        if (lastTime == null) {
            lastBroadcastTime.put(key, System.currentTimeMillis());
            return true;
        }
        
        if ((System.currentTimeMillis() - lastTime) > broadcastThrottleMs) {
            lastBroadcastTime.put(key, System.currentTimeMillis());
            return true;
        }
        return false;
    }
    
    private HivePresenceInfo buildHivePresenceInfoOptimized(String hiveId) {
        // Use cached data where possible
        List<UserPresence> activeUsers = getHiveActiveUsers(hiveId);
        List<FocusSession> sessions = getHiveFocusSessions(hiveId);
        
        long focusingSessions = sessions.stream()
                .filter(s -> s.getStatus() == FocusSession.SessionStatus.ACTIVE)
                .count();
        
        return HivePresenceInfo.builder()
                .hiveId(hiveId)
                .activeUsers(activeUsers.size())
                .focusingSessions((int) focusingSessions)
                .onlineMembers(activeUsers)
                .lastActivity(System.currentTimeMillis())
                .build();
    }
    
    private void updateUserPresenceWithSession(String userId, boolean inSession) {
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setInFocusSession(inSession);
            String presenceKey = USER_PRESENCE_KEY + userId;
            redisTemplate.opsForValue().set(presenceKey, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        }
    }
}