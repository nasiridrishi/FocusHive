package com.focushive.presence.storage.impl;

import com.focushive.presence.dto.FocusSession;
import com.focushive.presence.dto.UserPresence;
import com.focushive.presence.storage.PresenceStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of presence storage for production environments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.features.redis.enabled", havingValue = "true")
public class RedisPresenceStorageService implements PresenceStorageService {

    private static final String USER_PRESENCE_KEY = "presence:user:";
    private static final String HEARTBEAT_KEY = "presence:heartbeat:";
    private static final String HIVE_PRESENCE_SET_KEY = "presence:hive:members:";
    private static final String SESSION_KEY = "presence:session:";
    private static final String USER_SESSION_KEY = "presence:user:session:";
    private static final String HIVE_SESSIONS_KEY = "presence:hive:sessions:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void storeUserPresence(String userId, UserPresence presence, Duration ttl) {
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.opsForValue().set(key, presence, ttl.toSeconds(), TimeUnit.SECONDS);
        log.debug("Stored user presence for {} in Redis", userId);
    }

    @Override
    public UserPresence getUserPresence(String userId) {
        String key = USER_PRESENCE_KEY + userId;
        return (UserPresence) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void storeHeartbeat(String userId, long timestamp, Duration ttl) {
        String key = HEARTBEAT_KEY + userId;
        redisTemplate.opsForValue().set(key, timestamp, ttl.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Long getHeartbeat(String userId) {
        String key = HEARTBEAT_KEY + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        return null;
    }

    @Override
    public void addUserToHive(String hiveId, String userId) {
        String key = HIVE_PRESENCE_SET_KEY + hiveId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
        log.debug("Added user {} to hive {} in Redis", userId, hiveId);
    }

    @Override
    public void removeUserFromHive(String hiveId, String userId) {
        String key = HIVE_PRESENCE_SET_KEY + hiveId;
        redisTemplate.opsForSet().remove(key, userId);

        // Clean up empty sets
        Long setSize = redisTemplate.opsForSet().size(key);
        if (setSize != null && setSize == 0) {
            redisTemplate.delete(key);
        }
        log.debug("Removed user {} from hive {} in Redis", userId, hiveId);
    }

    @Override
    public Set<String> getHiveUsers(String hiveId) {
        String key = HIVE_PRESENCE_SET_KEY + hiveId;
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null) {
            return new HashSet<>();
        }

        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public void storeFocusSession(String sessionId, FocusSession session, Duration ttl) {
        String key = SESSION_KEY + sessionId;
        redisTemplate.opsForValue().set(key, session, ttl.toMinutes(), TimeUnit.MINUTES);
        log.debug("Stored focus session {} in Redis", sessionId);
    }

    @Override
    public FocusSession getFocusSession(String sessionId) {
        String key = SESSION_KEY + sessionId;
        return (FocusSession) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void mapUserToSession(String userId, String sessionId, Duration ttl) {
        String key = USER_SESSION_KEY + userId;
        redisTemplate.opsForValue().set(key, sessionId, ttl.toMinutes(), TimeUnit.MINUTES);
    }

    @Override
    public String getUserSessionId(String userId) {
        String key = USER_SESSION_KEY + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public void removeUserSessionMapping(String userId) {
        String key = USER_SESSION_KEY + userId;
        redisTemplate.delete(key);
    }

    @Override
    public void addSessionToHive(String hiveId, String sessionId) {
        String key = HIVE_SESSIONS_KEY + hiveId;
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, 2, TimeUnit.HOURS);
    }

    @Override
    public void removeSessionFromHive(String hiveId, String sessionId) {
        String key = HIVE_SESSIONS_KEY + hiveId;
        redisTemplate.opsForSet().remove(key, sessionId);

        // Clean up empty sets
        Long setSize = redisTemplate.opsForSet().size(key);
        if (setSize != null && setSize == 0) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public Set<String> getHiveSessions(String hiveId) {
        String key = HIVE_SESSIONS_KEY + hiveId;
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null) {
            return new HashSet<>();
        }

        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllUserPresenceKeys() {
        Set<String> keys = redisTemplate.keys(USER_PRESENCE_KEY + "*");
        return keys != null ? keys : new HashSet<>();
    }

    @Override
    public void deleteUserPresence(String userId) {
        String key = USER_PRESENCE_KEY + userId;
        redisTemplate.delete(key);
        log.debug("Deleted user presence for {} from Redis", userId);
    }

    @Override
    public void publishPresenceEvent(String channel, Map<String, Object> event) {
        try {
            redisTemplate.convertAndSend(channel, event);
        } catch (Exception e) {
            log.warn("Failed to publish presence event to Redis: {}", e.getMessage());
        }
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}