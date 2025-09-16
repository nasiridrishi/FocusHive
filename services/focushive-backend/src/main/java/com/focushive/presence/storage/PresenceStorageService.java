package com.focushive.presence.storage;

import com.focushive.presence.dto.*;
import java.time.Duration;
import java.util.*;

/**
 * Abstraction layer for presence storage operations to support multiple storage implementations.
 * This allows for Redis in production and simple in-memory storage for tests.
 */
public interface PresenceStorageService {

    /**
     * Store user presence with TTL
     */
    void storeUserPresence(String userId, UserPresence presence, Duration ttl);

    /**
     * Get user presence
     */
    UserPresence getUserPresence(String userId);

    /**
     * Store heartbeat for a user
     */
    void storeHeartbeat(String userId, long timestamp, Duration ttl);

    /**
     * Get heartbeat timestamp for a user
     */
    Long getHeartbeat(String userId);

    /**
     * Add user to hive presence set
     */
    void addUserToHive(String hiveId, String userId);

    /**
     * Remove user from hive presence set
     */
    void removeUserFromHive(String hiveId, String userId);

    /**
     * Get all users in a hive
     */
    Set<String> getHiveUsers(String hiveId);

    /**
     * Store focus session
     */
    void storeFocusSession(String sessionId, FocusSession session, Duration ttl);

    /**
     * Get focus session
     */
    FocusSession getFocusSession(String sessionId);

    /**
     * Map user to active session
     */
    void mapUserToSession(String userId, String sessionId, Duration ttl);

    /**
     * Get user's active session ID
     */
    String getUserSessionId(String userId);

    /**
     * Remove user session mapping
     */
    void removeUserSessionMapping(String userId);

    /**
     * Add session to hive sessions set
     */
    void addSessionToHive(String hiveId, String sessionId);

    /**
     * Remove session from hive sessions set
     */
    void removeSessionFromHive(String hiveId, String sessionId);

    /**
     * Get all sessions in a hive
     */
    Set<String> getHiveSessions(String hiveId);

    /**
     * Get all user presence keys
     */
    Set<String> getAllUserPresenceKeys();

    /**
     * Delete user presence
     */
    void deleteUserPresence(String userId);

    /**
     * Publish presence event (for distributed messaging)
     */
    void publishPresenceEvent(String channel, Map<String, Object> event);

    /**
     * Check if key exists
     */
    boolean hasKey(String key);
}