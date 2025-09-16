package com.focushive.presence.service.impl;

import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import com.focushive.presence.storage.PresenceStorageService;
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
@Primary
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
    
    private static final String USER_PRESENCE_KEY = "presence:user:";
    private static final String HIVE_PRESENCE_KEY = "presence:hive:";
    private static final String HIVE_PRESENCE_SET_KEY = "presence:hive:members:";
    private static final String SESSION_KEY = "presence:session:";
    private static final String USER_SESSION_KEY = "presence:user:";
    private static final String HIVE_SESSIONS_KEY = "presence:hive:sessions:";
    private static final String HEARTBEAT_KEY = "presence:heartbeat:";
    private static final String PRESENCE_CHANNEL = "presence:updates";
    private static final String SESSION_CHANNEL = "presence:sessions";
    
    private final PresenceStorageService storageService;
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
        
        // Store presence with expiration
        storageService.storeUserPresence(userId, presence, Duration.ofSeconds(heartbeatTimeoutSeconds * 2));
        
        // Update heartbeat
        recordHeartbeat(userId);
        
        // Broadcast to relevant channels
        broadcastPresenceUpdate(presence, update.hiveId());
        
        return presence;
    }
    
    @Override
    public UserPresence getUserPresence(String userId) {
        return storageService.getUserPresence(userId);
    }
    
    @Override
    public void recordHeartbeat(String userId) {
        // Update heartbeat timestamp using separate key for efficient tracking
        storageService.storeHeartbeat(userId, Instant.now().toEpochMilli(),
                Duration.ofSeconds(heartbeatTimeoutSeconds));
        
        // Update presence last seen if exists
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setLastSeen(Instant.now());
            storageService.storeUserPresence(userId, presence, Duration.ofSeconds(heartbeatTimeoutSeconds * 2));
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
        storageService.addUserToHive(hiveId, userId);
        
        // Publish join event to Redis pub/sub
        publishPresenceEvent("JOIN", hiveId, userId);
        
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
        storageService.removeUserFromHive(hiveId, userId);
        
        // Publish leave event to Redis pub/sub
        publishPresenceEvent("LEAVE", hiveId, userId);
        
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
        storageService.storeFocusSession(session.getSessionId(), session, Duration.ofMinutes(durationMinutes * 2));

        // Map user to session
        storageService.mapUserToSession(userId, session.getSessionId(), Duration.ofMinutes(durationMinutes * 2));

        // Add session to hive sessions set
        storageService.addSessionToHive(hiveId, session.getSessionId());
        
        // Publish session start event
        publishSessionEvent("START", session);
        
        // Update user presence to indicate focus session
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setInFocusSession(true);
            storageService.storeUserPresence(userId, presence, Duration.ofSeconds(heartbeatTimeoutSeconds * 2));
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
        FocusSession session = storageService.getFocusSession(sessionId);
        
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
        storageService.storeFocusSession(sessionId, session, Duration.ofHours(1));

        // Remove user session mapping
        storageService.removeUserSessionMapping(userId);

        // Remove session from hive sessions set
        if (session.getHiveId() != null) {
            storageService.removeSessionFromHive(session.getHiveId(), sessionId);
        }
        
        // Publish session end event
        publishSessionEvent("END", session);
        
        // Update user presence
        UserPresence presence = getUserPresence(userId);
        if (presence != null) {
            presence.setInFocusSession(false);
            storageService.storeUserPresence(userId, presence, Duration.ofSeconds(heartbeatTimeoutSeconds * 2));
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
        String sessionId = storageService.getUserSessionId(userId);

        if (sessionId == null) {
            return null;
        }

        return storageService.getFocusSession(sessionId);
    }
    
    @Override
    public List<FocusSession> getHiveFocusSessions(String hiveId) {
        List<FocusSession> sessions = new ArrayList<>();
        
        // Use hive sessions set for more efficient retrieval
        Set<String> sessionIds = storageService.getHiveSessions(hiveId);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return sessions;
        }

        // Get session details for each ID
        for (String sessionId : sessionIds) {
            FocusSession session = storageService.getFocusSession(sessionId);
            if (session != null) {
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
        Set<String> userKeys = storageService.getAllUserPresenceKeys();
        if (userKeys == null || userKeys.isEmpty()) {
            return;
        }
        
        Instant staleThreshold = Instant.now().minusSeconds(heartbeatTimeoutSeconds);
        
        for (String key : userKeys) {
            String userId = key.replace(USER_PRESENCE_KEY, "");
            UserPresence presence = storageService.getUserPresence(userId);
            if (presence != null && presence.getLastSeen().isBefore(staleThreshold)) {
                log.info("Removing stale presence for user {}", presence.getUserId());
                storageService.deleteUserPresence(userId);
                
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
            storageService.storeUserPresence(userId, presence, Duration.ofSeconds(10));
            
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
            storageService.deleteUserPresence(userId);
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
        return storageService.getHiveUsers(hiveId);
    }
    
    /**
     * Publishes presence events to Redis pub/sub for distributed messaging.
     */
    private void publishPresenceEvent(String eventType, String hiveId, String userId) {
        Map<String, Object> event = Map.of(
            "type", eventType,
            "hiveId", hiveId,
            "userId", userId,
            "timestamp", Instant.now().toEpochMilli()
        );
        storageService.publishPresenceEvent(PRESENCE_CHANNEL, event);
    }
    
    /**
     * Publishes session events to Redis pub/sub for distributed messaging.
     */
    private void publishSessionEvent(String eventType, FocusSession session) {
        Map<String, Object> event = Map.of(
            "type", eventType,
            "sessionId", session.getSessionId(),
            "userId", session.getUserId(),
            "hiveId", session.getHiveId() != null ? session.getHiveId() : "",
            "timestamp", Instant.now().toEpochMilli()
        );
        storageService.publishPresenceEvent(SESSION_CHANNEL, event);
    }
    
    /**
     * Checks if a user's heartbeat is still active.
     */
    private boolean isHeartbeatActive(String userId) {
        String heartbeatKey = HEARTBEAT_KEY + userId;
        return storageService.hasKey(heartbeatKey);
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