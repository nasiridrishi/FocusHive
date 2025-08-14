package com.focushive.presence.service.impl;

import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of PresenceService using Redis for storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
    
    private static final String USER_PRESENCE_KEY = "presence:user:";
    private static final String HIVE_PRESENCE_KEY = "presence:hive:";
    private static final String SESSION_KEY = "presence:session:";
    private static final String USER_SESSION_KEY = "presence:user:";
    private static final String HIVE_SESSIONS_KEY = "presence:hive:sessions:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final HiveMemberRepository hiveMemberRepository;
    
    @Value("${presence.heartbeat.timeout-seconds:30}")
    private int heartbeatTimeoutSeconds;
    
    @Override
    public UserPresence updateUserPresence(String userId, PresenceUpdate update) {
        // Removed debug log to avoid logging user presence updates frequently
        
        // Create user presence
        UserPresence presence = UserPresence.builder()
                .userId(userId)
                .status(update.status())
                .activity(update.activity() != null ? update.activity() : "Available")
                .lastSeen(Instant.now())
                .currentHiveId(update.hiveId())
                .build();
        
        // Store in Redis with expiration
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.opsForValue().set(key, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        
        // Update heartbeat
        recordHeartbeat(userId);
        
        // Broadcast to relevant channels
        broadcastPresenceUpdate(presence, update.hiveId());
        
        return presence;
    }
    
    @Override
    public UserPresence getUserPresence(String userId) {
        String key = USER_PRESENCE_KEY + userId;
        return (UserPresence) redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public void recordHeartbeat(String userId) {
        // Get current presence
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            // Update last seen
            presence.setLastSeen(Instant.now());
            
            // Re-store with extended TTL
            String key = USER_PRESENCE_KEY + userId;
            redisTemplate.opsForValue().set(key, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        }
    }
    
    @Override
    public HivePresenceInfo joinHivePresence(String hiveId, String userId) {
        log.info("User {} joining hive {} presence", userId, hiveId);
        
        // Verify user is member of hive
        if (!hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new IllegalArgumentException("User is not a member of this hive");
        }
        
        // Add user to hive presence set
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> hiveUsers = getHiveUserIds(hiveId);
        hiveUsers.add(userId);
        redisTemplate.opsForValue().set(key, hiveUsers, 1, TimeUnit.HOURS);
        
        // Get updated hive presence info
        HivePresenceInfo info = buildHivePresenceInfo(hiveId);
        
        // Broadcast join event
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/presence",
                info
        );
        
        return info;
    }
    
    @Override
    public HivePresenceInfo leaveHivePresence(String hiveId, String userId) {
        log.info("User {} leaving hive {} presence", userId, hiveId);
        
        // Remove user from hive presence set
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> hiveUsers = getHiveUserIds(hiveId);
        hiveUsers.remove(userId);
        
        if (hiveUsers.isEmpty()) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, hiveUsers, 1, TimeUnit.HOURS);
        }
        
        // Get updated hive presence info
        HivePresenceInfo info = buildHivePresenceInfo(hiveId);
        
        // Broadcast leave event
        messagingTemplate.convertAndSend(
                "/topic/hive/" + hiveId + "/presence",
                info
        );
        
        return info;
    }
    
    @Override
    public List<UserPresence> getHiveActiveUsers(String hiveId) {
        Set<String> userIds = getHiveUserIds(hiveId);
        
        List<UserPresence> activeUsers = new ArrayList<>();
        for (String userId : userIds) {
            UserPresence presence = getUserPresence(userId);
            if (presence != null) {
                activeUsers.add(presence);
            }
        }
        
        return activeUsers;
    }
    
    @Override
    public Map<String, HivePresenceInfo> getHivesPresenceInfo(Set<String> hiveIds) {
        Map<String, HivePresenceInfo> result = new HashMap<>();
        for (String hiveId : hiveIds) {
            result.put(hiveId, buildHivePresenceInfo(hiveId));
        }
        return result;
    }
    
    @Override
    public FocusSession startFocusSession(String userId, String hiveId, int durationMinutes) {
        log.info("Starting focus session for user {} in hive {} for {} minutes", userId, hiveId, durationMinutes);
        
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
        
        // Store session
        String sessionKey = SESSION_KEY + session.getSessionId();
        redisTemplate.opsForValue().set(sessionKey, session, durationMinutes * 2, TimeUnit.MINUTES);
        
        // Map user to session
        String userSessionKey = USER_SESSION_KEY + userId + ":session";
        redisTemplate.opsForValue().set(userSessionKey, session.getSessionId(), durationMinutes * 2, TimeUnit.MINUTES);
        
        // Update user presence to indicate focus session
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setInFocusSession(true);
            String presenceKey = USER_PRESENCE_KEY + userId;
            redisTemplate.opsForValue().set(presenceKey, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        }
        
        // Broadcast session start
        if (hiveId != null) {
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
        log.info("Ending focus session {} for user {}", sessionId, userId);
        
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
        
        // Store updated session for 1 hour for retrieval
        redisTemplate.opsForValue().set(sessionKey, session, 1, TimeUnit.HOURS);
        
        // Remove user session mapping
        String userSessionKey = USER_SESSION_KEY + userId + ":session";
        redisTemplate.delete(userSessionKey);
        
        // Update user presence
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setInFocusSession(false);
            String presenceKey = USER_PRESENCE_KEY + userId;
            redisTemplate.opsForValue().set(presenceKey, presence, heartbeatTimeoutSeconds * 2, TimeUnit.SECONDS);
        }
        
        // Broadcast session end
        if (session.getHiveId() != null) {
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
    public List<FocusSession> getHiveFocusSessions(String hiveId) {
        List<FocusSession> sessions = new ArrayList<>();
        
        // Get all session keys
        Set<String> sessionKeys = redisTemplate.keys(SESSION_KEY + "*");
        if (sessionKeys == null) {
            return sessions;
        }
        
        // Filter sessions by hive
        for (String key : sessionKeys) {
            FocusSession session = (FocusSession) redisTemplate.opsForValue().get(key);
            if (session != null && hiveId.equals(session.getHiveId())) {
                sessions.add(session);
            }
        }
        
        return sessions;
    }
    
    @Override
    @Scheduled(fixedDelay = 30000) // Run every 30 seconds
    public void cleanupStalePresence() {
        // Keep minimal debug log for scheduled cleanup operations
        if (log.isDebugEnabled()) {
            log.debug("Running stale presence cleanup");
        }
        
        // Get all user presence keys
        Set<String> userKeys = redisTemplate.keys(USER_PRESENCE_KEY + "*");
        if (userKeys == null) {
            return;
        }
        
        Instant staleThreshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        
        for (String key : userKeys) {
            UserPresence presence = (UserPresence) redisTemplate.opsForValue().get(key);
            if (presence != null && presence.getLastSeen().isBefore(staleThreshold)) {
                log.info("Removing stale presence for user {}", presence.getUserId());
                redisTemplate.delete(key);
                
                // Remove from any hives
                if (presence.getCurrentHiveId() != null) {
                    leaveHivePresence(presence.getCurrentHiveId(), presence.getUserId());
                }
            }
        }
    }
    
    @Override
    public void markUserOffline(String userId) {
        log.info("Marking user {} as offline", userId);
        
        // Get current presence
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            // Update status to offline
            presence.setStatus(PresenceStatus.OFFLINE);
            presence.setLastSeen(Instant.now());
            
            // Store briefly for any final notifications
            String key = USER_PRESENCE_KEY + userId;
            redisTemplate.opsForValue().set(key, presence, 10, TimeUnit.SECONDS);
            
            // Remove from any hives
            if (presence.getCurrentHiveId() != null) {
                leaveHivePresence(presence.getCurrentHiveId(), userId);
            }
            
            // End any active sessions
            FocusSession activeSession = getActiveFocusSession(userId);
            if (activeSession != null) {
                endFocusSession(userId, activeSession.getSessionId());
            }
            
            // Broadcast offline status
            broadcastPresenceUpdate(presence, presence.getCurrentHiveId());
            
            // Delete presence after broadcast
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
            messagingTemplate.convertAndSend(
                    "/topic/hive/" + hiveId + "/presence",
                    broadcast
            );
        }
        
        // Also send to user's personal channel
        messagingTemplate.convertAndSendToUser(
                presence.getUserId(),
                "/queue/presence",
                broadcast
        );
    }
    
    private Set<String> getHiveUserIds(String hiveId) {
        String key = HIVE_PRESENCE_KEY + hiveId;
        Set<String> userIds = (Set<String>) redisTemplate.opsForValue().get(key);
        return userIds != null ? userIds : new HashSet<>();
    }
    
    private HivePresenceInfo buildHivePresenceInfo(String hiveId) {
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
}