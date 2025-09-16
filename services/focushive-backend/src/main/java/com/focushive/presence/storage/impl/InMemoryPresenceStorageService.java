package com.focushive.presence.storage.impl;

import com.focushive.presence.dto.FocusSession;
import com.focushive.presence.dto.UserPresence;
import com.focushive.presence.storage.PresenceStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of presence storage for test environments.
 * This avoids Redis dependency in test profiles.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.features.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryPresenceStorageService implements PresenceStorageService {

    private final Map<String, UserPresence> userPresenceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> heartbeatMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> hiveUsersMap = new ConcurrentHashMap<>();
    private final Map<String, FocusSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> hiveSessionsMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> expiryMap = new ConcurrentHashMap<>();

    @Override
    public void storeUserPresence(String userId, UserPresence presence, Duration ttl) {
        userPresenceMap.put(userId, presence);
        if (ttl != null) {
            expiryMap.put("presence:" + userId, Instant.now().plus(ttl));
        }
        log.debug("Stored user presence for {} in memory", userId);
    }

    @Override
    public UserPresence getUserPresence(String userId) {
        // Check if expired
        Instant expiry = expiryMap.get("presence:" + userId);
        if (expiry != null && Instant.now().isAfter(expiry)) {
            userPresenceMap.remove(userId);
            expiryMap.remove("presence:" + userId);
            return null;
        }
        return userPresenceMap.get(userId);
    }

    @Override
    public void storeHeartbeat(String userId, long timestamp, Duration ttl) {
        heartbeatMap.put(userId, timestamp);
        if (ttl != null) {
            expiryMap.put("heartbeat:" + userId, Instant.now().plus(ttl));
        }
    }

    @Override
    public Long getHeartbeat(String userId) {
        // Check if expired
        Instant expiry = expiryMap.get("heartbeat:" + userId);
        if (expiry != null && Instant.now().isAfter(expiry)) {
            heartbeatMap.remove(userId);
            expiryMap.remove("heartbeat:" + userId);
            return null;
        }
        return heartbeatMap.get(userId);
    }

    @Override
    public void addUserToHive(String hiveId, String userId) {
        hiveUsersMap.computeIfAbsent(hiveId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        log.debug("Added user {} to hive {} in memory", userId, hiveId);
    }

    @Override
    public void removeUserFromHive(String hiveId, String userId) {
        Set<String> users = hiveUsersMap.get(hiveId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                hiveUsersMap.remove(hiveId);
            }
        }
        log.debug("Removed user {} from hive {} in memory", userId, hiveId);
    }

    @Override
    public Set<String> getHiveUsers(String hiveId) {
        return new HashSet<>(hiveUsersMap.getOrDefault(hiveId, Collections.emptySet()));
    }

    @Override
    public void storeFocusSession(String sessionId, FocusSession session, Duration ttl) {
        sessionMap.put(sessionId, session);
        if (ttl != null) {
            expiryMap.put("session:" + sessionId, Instant.now().plus(ttl));
        }
        log.debug("Stored focus session {} in memory", sessionId);
    }

    @Override
    public FocusSession getFocusSession(String sessionId) {
        // Check if expired
        Instant expiry = expiryMap.get("session:" + sessionId);
        if (expiry != null && Instant.now().isAfter(expiry)) {
            sessionMap.remove(sessionId);
            expiryMap.remove("session:" + sessionId);
            return null;
        }
        return sessionMap.get(sessionId);
    }

    @Override
    public void mapUserToSession(String userId, String sessionId, Duration ttl) {
        userSessionMap.put(userId, sessionId);
        if (ttl != null) {
            expiryMap.put("userSession:" + userId, Instant.now().plus(ttl));
        }
    }

    @Override
    public String getUserSessionId(String userId) {
        // Check if expired
        Instant expiry = expiryMap.get("userSession:" + userId);
        if (expiry != null && Instant.now().isAfter(expiry)) {
            userSessionMap.remove(userId);
            expiryMap.remove("userSession:" + userId);
            return null;
        }
        return userSessionMap.get(userId);
    }

    @Override
    public void removeUserSessionMapping(String userId) {
        userSessionMap.remove(userId);
        expiryMap.remove("userSession:" + userId);
    }

    @Override
    public void addSessionToHive(String hiveId, String sessionId) {
        hiveSessionsMap.computeIfAbsent(hiveId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    @Override
    public void removeSessionFromHive(String hiveId, String sessionId) {
        Set<String> sessions = hiveSessionsMap.get(hiveId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                hiveSessionsMap.remove(hiveId);
            }
        }
    }

    @Override
    public Set<String> getHiveSessions(String hiveId) {
        return new HashSet<>(hiveSessionsMap.getOrDefault(hiveId, Collections.emptySet()));
    }

    @Override
    public Set<String> getAllUserPresenceKeys() {
        // Clean expired entries first
        cleanExpiredEntries();

        return userPresenceMap.keySet().stream()
                .map(userId -> "presence:user:" + userId)
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteUserPresence(String userId) {
        userPresenceMap.remove(userId);
        expiryMap.remove("presence:" + userId);
        log.debug("Deleted user presence for {} from memory", userId);
    }

    @Override
    public void publishPresenceEvent(String channel, Map<String, Object> event) {
        // In-memory implementation doesn't support pub/sub
        // Just log the event for debugging
        log.debug("Presence event published to channel {}: {}", channel, event);
    }

    @Override
    public boolean hasKey(String key) {
        if (key.startsWith("presence:heartbeat:")) {
            String userId = key.substring("presence:heartbeat:".length());
            return getHeartbeat(userId) != null;
        }
        return false;
    }

    /**
     * Clean up expired entries periodically
     */
    private void cleanExpiredEntries() {
        Instant now = Instant.now();
        Set<String> expiredKeys = new HashSet<>();

        expiryMap.forEach((key, expiry) -> {
            if (now.isAfter(expiry)) {
                expiredKeys.add(key);
            }
        });

        for (String key : expiredKeys) {
            expiryMap.remove(key);

            if (key.startsWith("presence:")) {
                String userId = key.substring("presence:".length());
                userPresenceMap.remove(userId);
            } else if (key.startsWith("heartbeat:")) {
                String userId = key.substring("heartbeat:".length());
                heartbeatMap.remove(userId);
            } else if (key.startsWith("session:")) {
                String sessionId = key.substring("session:".length());
                sessionMap.remove(sessionId);
            } else if (key.startsWith("userSession:")) {
                String userId = key.substring("userSession:".length());
                userSessionMap.remove(userId);
            }
        }
    }
}