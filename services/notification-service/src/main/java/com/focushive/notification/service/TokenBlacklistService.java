package com.focushive.notification.service;

import com.focushive.notification.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing blacklisted JWT tokens.
 * Provides functionality for logout by blacklisting tokens until their expiration.
 * Uses Redis for distributed storage and automatic expiration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";
    private static final String BLACKLIST_USER_PREFIX = "blacklist:user:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final SecurityAuditService securityAuditService;

    /**
     * Blacklists a JWT token, effectively logging out the user.
     * The token will remain blacklisted until its natural expiration time.
     *
     * @param token The JWT token to blacklist
     * @param reason The reason for blacklisting (e.g., "user_logout", "security_breach")
     */
    public void blacklistToken(Jwt token, String reason) {
        if (!isBlacklistingEnabled()) {
            log.debug("Token blacklisting is disabled");
            return;
        }

        String jti = token.getId();
        if (jti == null) {
            // Fall back to using the entire token claim as ID if no JTI
            jti = token.getTokenValue().substring(0, Math.min(token.getTokenValue().length(), 50));
        }

        String key = BLACKLIST_KEY_PREFIX + jti;

        // Calculate TTL based on token expiration
        Duration ttl = calculateTtl(token);

        // Store blacklist entry with metadata
        BlacklistEntry entry = BlacklistEntry.builder()
                .tokenId(jti)
                .subject(token.getSubject())
                .blacklistedAt(Instant.now())
                .expiresAt(token.getExpiresAt())
                .reason(reason)
                .clientInfo(extractClientInfo(token))
                .build();

        try {
            redisTemplate.opsForValue().set(key, entry, ttl);

            // Also maintain a user-specific blacklist for tracking
            String userKey = BLACKLIST_USER_PREFIX + token.getSubject();
            redisTemplate.opsForList().leftPush(userKey, jti);
            redisTemplate.expire(userKey, ttl);

            log.info("Token blacklisted for user {}: reason={}, expires={}",
                    token.getSubject(), reason, token.getExpiresAt());

            // Audit the logout/blacklist event
            securityAuditService.logLogoutEvent(token.getSubject(), reason, jti);

        } catch (Exception e) {
            log.error("Failed to blacklist token for user {}: {}",
                    token.getSubject(), e.getMessage(), e);
            throw new TokenBlacklistException("Failed to blacklist token", e);
        }
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(Jwt token) {
        if (!isBlacklistingEnabled()) {
            return false;
        }

        String jti = token.getId();
        if (jti == null) {
            jti = token.getTokenValue().substring(0, Math.min(token.getTokenValue().length(), 50));
        }

        String key = BLACKLIST_KEY_PREFIX + jti;

        try {
            BlacklistEntry entry = (BlacklistEntry) redisTemplate.opsForValue().get(key);

            if (entry != null) {
                log.debug("Token is blacklisted for user {}: reason={}",
                        token.getSubject(), entry.getReason());
                return true;
            }

            return false;

        } catch (Exception e) {
            // In case of Redis failure, we err on the side of security
            log.error("Failed to check blacklist status for token: {}", e.getMessage());
            return securityProperties.getJwt().isBlacklistingEnabled();
        }
    }

    /**
     * Blacklists all tokens for a specific user.
     * Useful for security incidents or forced logout of all sessions.
     *
     * @param username The username whose tokens should be blacklisted
     * @param reason The reason for blacklisting
     */
    public void blacklistAllUserTokens(String username, String reason) {
        if (!isBlacklistingEnabled()) {
            log.debug("Token blacklisting is disabled");
            return;
        }

        try {
            // This would require maintaining a mapping of users to their active tokens
            // For now, we'll mark the user as requiring re-authentication
            String userBlacklistKey = "blacklist:user:all:" + username;

            redisTemplate.opsForValue().set(
                userBlacklistKey,
                Instant.now(),
                securityProperties.getJwt().getBlacklistTtl()
            );

            log.info("All tokens blacklisted for user {}: reason={}", username, reason);
            securityAuditService.logSecurityEvent("ALL_TOKENS_BLACKLISTED", username, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist all tokens for user {}: {}",
                    username, e.getMessage(), e);
            throw new TokenBlacklistException("Failed to blacklist user tokens", e);
        }
    }

    /**
     * Checks if a user has been globally blacklisted (all tokens invalidated).
     *
     * @param username The username to check
     * @return true if the user is globally blacklisted, false otherwise
     */
    public boolean isUserBlacklisted(String username) {
        if (!isBlacklistingEnabled()) {
            return false;
        }

        String userBlacklistKey = "blacklist:user:all:" + username;

        try {
            return redisTemplate.hasKey(userBlacklistKey);
        } catch (Exception e) {
            log.error("Failed to check user blacklist status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Removes a token from the blacklist.
     * Should be used with caution and only for administrative purposes.
     *
     * @param tokenId The token ID to remove from blacklist
     */
    public void removeFromBlacklist(String tokenId) {
        if (!isBlacklistingEnabled()) {
            return;
        }

        String key = BLACKLIST_KEY_PREFIX + tokenId;

        try {
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("Token removed from blacklist: {}", tokenId);
                securityAuditService.logAdminAction("TOKEN_UNBLACKLISTED", tokenId, Map.of("action", "manual_removal"));
            }

        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the count of blacklisted tokens for a user.
     *
     * @param username The username to check
     * @return The number of blacklisted tokens
     */
    public long getBlacklistedTokenCount(String username) {
        if (!isBlacklistingEnabled()) {
            return 0;
        }

        String userKey = BLACKLIST_USER_PREFIX + username;

        try {
            Long size = redisTemplate.opsForList().size(userKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Failed to get blacklisted token count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Cleans up expired blacklist entries.
     * This is typically handled automatically by Redis TTL, but this method
     * can be used for manual cleanup if needed.
     */
    public void cleanupExpiredEntries() {
        // Redis handles expiration automatically via TTL
        // This method is here for potential future manual cleanup needs
        log.debug("Cleanup requested - Redis handles expiration automatically");
    }

    /**
     * Calculates the TTL for a blacklisted token based on its expiration time.
     */
    private Duration calculateTtl(Jwt token) {
        Instant expiresAt = token.getExpiresAt();
        Duration configuredTtl = securityProperties.getJwt().getBlacklistTtl();

        if (expiresAt == null) {
            return configuredTtl;
        }

        Duration tokenTtl = Duration.between(Instant.now(), expiresAt);

        // Use the longer of the two TTLs to ensure the token stays blacklisted
        // until after it would naturally expire
        return tokenTtl.compareTo(configuredTtl) > 0 ? tokenTtl : configuredTtl;
    }

    /**
     * Extracts client information from the token for audit purposes.
     */
    private String extractClientInfo(Jwt token) {
        StringBuilder info = new StringBuilder();

        Object clientId = token.getClaim("client_id");
        if (clientId != null) {
            info.append("client=").append(clientId).append(";");
        }

        Object scope = token.getClaim("scope");
        if (scope != null) {
            info.append("scope=").append(scope).append(";");
        }

        Object aud = token.getClaim("aud");
        if (aud != null) {
            info.append("aud=").append(aud);
        }

        return info.toString();
    }

    /**
     * Checks if blacklisting is enabled in configuration.
     */
    private boolean isBlacklistingEnabled() {
        return securityProperties.getJwt().isBlacklistingEnabled();
    }

    /**
     * Internal class representing a blacklisted token entry.
     */
    @lombok.Data
    @lombok.Builder
    private static class BlacklistEntry {
        private String tokenId;
        private String subject;
        private Instant blacklistedAt;
        private Instant expiresAt;
        private String reason;
        private String clientInfo;
    }

    /**
     * Exception thrown when token blacklist operations fail.
     */
    public static class TokenBlacklistException extends RuntimeException {
        public TokenBlacklistException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}