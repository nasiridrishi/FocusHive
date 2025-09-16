package com.focushive.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.event.TokenRevocationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for publishing and handling token revocation events.
 * Provides both local and distributed event propagation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationEventService {

    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> jsonRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REVOCATION_TOPIC = "token-revocation-events";
    private static final String REVOCATION_CACHE_PREFIX = "revoked:token:";
    private static final long CACHE_TTL_HOURS = 24; // Keep revocation info for 24 hours

    /**
     * Publish token revocation event locally and to distributed cache.
     */
    @Async
    public void publishRevocationEvent(TokenRevocationEvent event) {
        try {
            // Set source instance
            event.setSourceInstance(getInstanceId());

            // Publish to local Spring event bus for immediate processing
            eventPublisher.publishEvent(event);
            log.debug("Published local revocation event: {}", event.getEventId());

            // Publish to Redis for distributed notification
            publishToRedis(event);

            // Cache the revocation for quick lookup
            cacheRevocation(event);

        } catch (Exception e) {
            log.error("Failed to publish revocation event: {}", event, e);
            // Don't throw - revocation should still work even if event publishing fails
        }
    }

    /**
     * Publish access token revocation event.
     */
    public void publishAccessTokenRevocation(String tokenHash, UUID userId,
                                            UUID clientId, String reason) {
        TokenRevocationEvent event = TokenRevocationEvent.accessTokenRevoked(
            tokenHash, userId, clientId, reason);
        publishRevocationEvent(event);
    }

    /**
     * Publish refresh token revocation event.
     */
    public void publishRefreshTokenRevocation(String tokenHash, UUID userId,
                                             UUID clientId, String reason) {
        TokenRevocationEvent event = TokenRevocationEvent.refreshTokenRevoked(
            tokenHash, userId, clientId, reason);
        publishRevocationEvent(event);
    }

    /**
     * Publish event to revoke all tokens for a user/client.
     */
    public void publishAllTokensRevocation(UUID userId, UUID clientId, String reason) {
        TokenRevocationEvent event = TokenRevocationEvent.allTokensRevoked(
            userId, clientId, reason);
        publishRevocationEvent(event);
    }

    /**
     * Check if a token has been revoked in the distributed cache.
     */
    public boolean isTokenRevoked(String tokenHash) {
        try {
            String key = REVOCATION_CACHE_PREFIX + tokenHash;
            Boolean exists = jsonRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check token revocation status: {}", tokenHash, e);
            // On error, assume not revoked to avoid blocking legitimate requests
            return false;
        }
    }

    /**
     * Publish event to Redis pub/sub for distributed propagation.
     */
    private void publishToRedis(TokenRevocationEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            jsonRedisTemplate.convertAndSend(REVOCATION_TOPIC, message);
            log.debug("Published revocation event to Redis: {}", event.getEventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize revocation event: {}", event, e);
        }
    }

    /**
     * Cache the revocation in Redis for quick lookup.
     */
    private void cacheRevocation(TokenRevocationEvent event) {
        try {
            if (event.getTokenHash() != null) {
                String key = REVOCATION_CACHE_PREFIX + event.getTokenHash();
                String value = objectMapper.writeValueAsString(event);
                jsonRedisTemplate.opsForValue().set(key, value, CACHE_TTL_HOURS, TimeUnit.HOURS);
                log.debug("Cached token revocation: {}", event.getTokenHash());
            }

            // For ALL_TOKENS revocation, we'd need to track by user/client
            // This would require a more complex caching strategy
            if (event.getTokenType() == TokenRevocationEvent.TokenType.ALL_TOKENS) {
                String userKey = String.format("%suser:%s:client:%s",
                    REVOCATION_CACHE_PREFIX, event.getUserId(), event.getClientId());
                String value = objectMapper.writeValueAsString(event);
                jsonRedisTemplate.opsForValue().set(userKey, value, CACHE_TTL_HOURS, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("Failed to cache revocation: {}", event, e);
        }
    }

    /**
     * Clear revocation cache for a token (used when token expires naturally).
     */
    public void clearRevocationCache(String tokenHash) {
        try {
            String key = REVOCATION_CACHE_PREFIX + tokenHash;
            jsonRedisTemplate.delete(key);
            log.debug("Cleared revocation cache for token: {}", tokenHash);
        } catch (Exception e) {
            log.error("Failed to clear revocation cache: {}", tokenHash, e);
        }
    }

    /**
     * Get the current service instance ID.
     */
    private String getInstanceId() {
        // In production, this could be from environment variable or service discovery
        return System.getProperty("instance.id", "identity-service-" + UUID.randomUUID().toString().substring(0, 8));
    }
}