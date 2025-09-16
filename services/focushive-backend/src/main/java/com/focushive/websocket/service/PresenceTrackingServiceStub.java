package com.focushive.websocket.service;

import com.focushive.websocket.dto.PresenceUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Stub implementation of PresenceTrackingService for test profile
 */
@Service("presenceTrackingService")
@Slf4j
@Profile("test")
public class PresenceTrackingServiceStub extends PresenceTrackingService {

    // Constructor to bypass dependencies
    public PresenceTrackingServiceStub() {
        super(null, null);
    }

    @Override
    public void updateUserPresence(Long userId, PresenceUpdate.PresenceStatus status, Long hiveId, String activity) {
        log.debug("Stub: updateUserPresence called for user {}", userId);
    }

    @Override
    public void updateUserActivity(Long userId) {
        log.debug("Stub: updateUserActivity called for user {}", userId);
    }

    @Override
    public void startFocusSession(Long userId, Long hiveId, Integer focusMinutes) {
        log.debug("Stub: startFocusSession called for user {} for {} minutes", userId, focusMinutes);
    }

    @Override
    public void startBuddySession(Long userId, Long buddyId) {
        log.debug("Stub: startBuddySession called for user {} with buddy {}", userId, buddyId);
    }

    @Override
    public PresenceUpdate getUserPresence(Long userId) {
        log.debug("Stub: getUserPresence called for user {}", userId);
        return PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.ONLINE)
                .lastSeen(LocalDateTime.now())
                .build();
    }

    @Override
    public List<PresenceUpdate> getHivePresence(Long hiveId) {
        log.debug("Stub: getHivePresence called for hive {}", hiveId);
        return Collections.emptyList();
    }

    @Override
    public long getHiveOnlineCount(Long hiveId) {
        log.debug("Stub: getHiveOnlineCount called for hive {}", hiveId);
        return 0;
    }

    @Override
    public void removeUserPresence(Long userId) {
        log.debug("Stub: removeUserPresence called for user {}", userId);
    }

    @Override
    public PresenceUpdate recoverPresenceState(Long userId, String sessionId) {
        log.debug("Stub: recoverPresenceState called for user {} with session {}", userId, sessionId);
        return PresenceUpdate.builder()
                .userId(userId)
                .status(PresenceUpdate.PresenceStatus.ONLINE)
                .lastSeen(LocalDateTime.now())
                .build();
    }

    @Override
    public void handleUserConnection(Long userId, String sessionId, Long hiveId) {
        log.debug("Stub: handleUserConnection called for user {}", userId);
    }

    @Override
    public void handleUserDisconnection(Long userId, String sessionId) {
        log.debug("Stub: handleUserDisconnection called for user {}", userId);
    }

    @Override
    public void subscribeToHivePresence(Long userId, Set<Long> hiveIds) {
        log.debug("Stub: subscribeToHivePresence called for user {}", userId);
    }

    @Override
    public void unsubscribeFromHivePresence(Long userId, Long hiveId) {
        log.debug("Stub: unsubscribeFromHivePresence called for user {}", userId);
    }

    @Override
    public void cleanupUserSubscriptions(Long userId) {
        log.debug("Stub: cleanupUserSubscriptions called for user {}", userId);
    }
}