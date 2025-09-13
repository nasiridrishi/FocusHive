package com.focushive.websocket.integration;

import com.focushive.websocket.dto.PresenceUpdate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of PresenceTrackingService for testing
 */
@Service
public class MockPresenceTrackingService {
    
    private final Map<Long, PresenceUpdate> userPresenceMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> hiveUsersMap = new ConcurrentHashMap<>();
    
    public void updateUserActivity(Long userId) {
        PresenceUpdate presence = userPresenceMap.get(userId);
        if (presence != null) {
            presence.setLastSeen(LocalDateTime.now());
        } else {
            // Create default presence
            presence = PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.ONLINE)
                .lastSeen(LocalDateTime.now())
                .build();
            userPresenceMap.put(userId, presence);
        }
    }
    
    public void updateUserPresence(Long userId, PresenceUpdate.PresenceStatus status, Long hiveId, String activity) {
        PresenceUpdate presence = PresenceUpdate.builder()
            .userId(userId)
            .status(status)
            .hiveId(hiveId)
            .currentActivity(activity)
            .lastSeen(LocalDateTime.now())
            .build();
        
        userPresenceMap.put(userId, presence);
        
        if (hiveId != null) {
            hiveUsersMap.computeIfAbsent(hiveId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }
    }
    
    public PresenceUpdate getUserPresence(Long userId) {
        return userPresenceMap.get(userId);
    }
    
    public List<PresenceUpdate> getHivePresence(Long hiveId) {
        Set<Long> userIds = hiveUsersMap.get(hiveId);
        if (userIds == null) {
            return Collections.emptyList();
        }
        
        return userIds.stream()
            .map(userPresenceMap::get)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public long getHiveOnlineCount(Long hiveId) {
        return getHivePresence(hiveId).stream()
            .filter(p -> p.getStatus() == PresenceUpdate.PresenceStatus.ONLINE)
            .count();
    }
    
    public void startFocusSession(Long userId, Long hiveId, Integer focusMinutes) {
        updateUserPresence(userId, PresenceUpdate.PresenceStatus.BUSY, hiveId, 
                         "Focus session (" + focusMinutes + " min)");
    }
    
    public void startBuddySession(Long userId, Long buddyId) {
        updateUserPresence(userId, PresenceUpdate.PresenceStatus.BUSY, null, 
                         "Buddy session with user " + buddyId);
    }
    
    public void removeUserPresence(Long userId) {
        PresenceUpdate presence = userPresenceMap.remove(userId);
        if (presence != null && presence.getHiveId() != null) {
            Set<Long> hiveUsers = hiveUsersMap.get(presence.getHiveId());
            if (hiveUsers != null) {
                hiveUsers.remove(userId);
            }
        }
    }
}