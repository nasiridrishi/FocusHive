package com.focushive.identity.service;

import com.focushive.identity.audit.OAuth2AuditEvent;
import com.focushive.identity.dto.TokenRotationResult;
import com.focushive.identity.entity.OAuthRefreshToken;
import com.focushive.identity.entity.OAuthAccessToken;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.exception.OAuth2AuthorizationException;
import com.focushive.identity.repository.OAuthRefreshTokenRepository;
import com.focushive.identity.repository.OAuthAccessTokenRepository;
import com.focushive.identity.metrics.OAuth2MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Service for handling OAuth2 token rotation security.
 * Implements refresh token rotation to prevent replay attacks and enhance security.
 *
 * Token rotation strategy:
 * 1. Each refresh token can only be used once
 * 2. On use, a new refresh token is issued and the old one is invalidated
 * 3. Token families are tracked to detect reuse of old tokens
 * 4. If an old token is reused, the entire token family is revoked (breach detection)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenRotationService {

    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final OAuthAccessTokenRepository accessTokenRepository;
    private final OAuth2AuditService auditService;
    private final OAuth2MetricsService metricsService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Configuration for token rotation policies
    @Value("${oauth2.token.rotation.enabled:true}")
    private boolean rotationEnabled;

    @Value("${oauth2.token.rotation.grace-period-seconds:10}")
    private int gracePeriodSeconds;

    @Value("${oauth2.token.rotation.max-chain-depth:100}")
    private int maxChainDepth;

    @Value("${oauth2.token.rotation.detect-reuse:true}")
    private boolean detectReuse;

    @Value("${oauth2.token.rotation.revoke-family-on-reuse:true}")
    private boolean revokeFamilyOnReuse;

    /**
     * Rotate a refresh token when it's used to get a new access token.
     *
     * @param oldRefreshToken The refresh token being used
     * @param newAccessToken The new access token being issued
     * @param clientId The client requesting the rotation
     * @param scopes The scopes for the new token
     * @param ipAddress The IP address of the request
     * @param userAgent The user agent string
     * @return The new refresh token
     */
    @Transactional
    public TokenRotationResult rotateRefreshToken(
            OAuthRefreshToken oldRefreshToken,
            OAuthAccessToken newAccessToken,
            String clientId,
            Set<String> scopes,
            String ipAddress,
            String userAgent) {

        log.debug("Rotating refresh token for client: {} user: {}", clientId, oldRefreshToken.getUserId());

        // Check if rotation is enabled
        if (!rotationEnabled) {
            log.debug("Token rotation is disabled, reusing existing refresh token");
            oldRefreshToken.markAsUsed();
            refreshTokenRepository.save(oldRefreshToken);
            // When rotation is disabled, we don't have the original token value
            // Client should continue using their existing refresh token
            return TokenRotationResult.builder()
                .refreshToken(oldRefreshToken)
                .tokenValue(null) // Signal to keep existing token
                .build();
        }

        // Check for potential token reuse attack
        if (oldRefreshToken.isRevoked()) {
            handleTokenReuseDetected(oldRefreshToken, clientId, ipAddress);
            throw new OAuth2AuthorizationException("Refresh token has been revoked");
        }

        // Check chain depth to prevent infinite chains
        int chainDepth = getTokenChainDepth(oldRefreshToken);
        if (chainDepth >= maxChainDepth) {
            log.warn("Token chain depth exceeded for token: {}, forcing new chain", oldRefreshToken.getId());
            return createNewTokenChain(oldRefreshToken, newAccessToken, clientId, scopes, ipAddress, userAgent);
        }

        // Generate new refresh token
        String newTokenValue = generateSecureToken();
        String newTokenHash = hashToken(newTokenValue);

        // Create new refresh token entity
        OAuthRefreshToken newRefreshToken = OAuthRefreshToken.builder()
                .tokenHash(newTokenHash)
                .userId(oldRefreshToken.getUserId())
                .clientId(oldRefreshToken.getClientId())
                .accessToken(newAccessToken)
                .scopes(new LinkedHashSet<>(scopes))
                .expiresAt(calculateRefreshTokenExpiry(oldRefreshToken.getClient()))
                .issuedIp(ipAddress)
                .userAgent(userAgent)
                .replacedToken(oldRefreshToken)
                .sessionId(oldRefreshToken.getSessionId())
                .build();

        // Mark old token as replaced (with grace period)
        markTokenAsRotated(oldRefreshToken);

        // Save new token
        newRefreshToken = refreshTokenRepository.save(newRefreshToken);

        // Record metrics
        metricsService.recordTokenRotation(clientId);

        // Audit log
        auditService.logTokenRotation(
                clientId,
                oldRefreshToken.getUserId(),
                oldRefreshToken.getId(),
                newRefreshToken.getId(),
                ipAddress
        );

        log.info("Successfully rotated refresh token for client: {} user: {}",
                clientId, oldRefreshToken.getUserId());

        return TokenRotationResult.builder()
            .refreshToken(newRefreshToken)
            .tokenValue(newTokenValue)
            .build();
    }

    /**
     * Handle detection of token reuse (potential security breach).
     */
    private void handleTokenReuseDetected(OAuthRefreshToken reusedToken, String clientId, String ipAddress) {
        log.error("SECURITY ALERT: Refresh token reuse detected for client: {} token: {}",
                clientId, reusedToken.getId());

        if (detectReuse && revokeFamilyOnReuse) {
            // Revoke entire token family
            revokeTokenFamily(reusedToken);

            // Record security event
            auditService.logSecurityAlert(
                    OAuth2AuditEvent.EventType.TOKEN_REUSE_DETECTED,
                    "Refresh token reuse detected - entire family revoked",
                    reusedToken.getUserId(),
                    clientId,
                    Map.of(
                        "reusedTokenId", reusedToken.getId(),
                        "ipAddress", ipAddress,
                        "sessionId", reusedToken.getSessionId()
                    )
            );

            // Record metrics
            metricsService.recordSecurityEvent("token_reuse_detected", clientId);
        }
    }

    /**
     * Revoke an entire token family when reuse is detected.
     */
    @Transactional
    public void revokeTokenFamily(OAuthRefreshToken token) {
        String sessionId = token.getSessionId();
        if (sessionId == null) {
            // No session ID, just revoke this token
            token.revoke("token_reuse_detected");
            refreshTokenRepository.save(token);
            return;
        }

        log.warn("Revoking entire token family for session: {}", sessionId);

        // Find all tokens in the family
        List<OAuthRefreshToken> familyTokens = refreshTokenRepository.findBySessionId(sessionId);

        // Revoke all tokens in the family
        for (OAuthRefreshToken familyToken : familyTokens) {
            if (!familyToken.isRevoked()) {
                familyToken.revoke("family_revoked_security_breach");

                // Also revoke associated access tokens
                if (familyToken.getAccessToken() != null) {
                    OAuthAccessToken accessToken = familyToken.getAccessToken();
                    accessToken.setRevoked(true);
                    accessToken.setRevokedAt(Instant.now());
                    accessToken.setRevocationReason("family_revoked_security_breach");
                    accessTokenRepository.save(accessToken);
                }
            }
        }

        refreshTokenRepository.saveAll(familyTokens);

        log.info("Revoked {} tokens in family for session: {}", familyTokens.size(), sessionId);
    }

    /**
     * Mark a token as rotated with a grace period.
     */
    private void markTokenAsRotated(OAuthRefreshToken token) {
        // Allow a grace period for race conditions in distributed systems
        Instant revokeTime = Instant.now().plusSeconds(gracePeriodSeconds);

        // Schedule the token for revocation after grace period
        token.setExpiresAt(revokeTime);
        token.markAsUsed();

        // Save the updated token
        refreshTokenRepository.save(token);

        log.debug("Marked token {} for rotation, will expire at {}", token.getId(), revokeTime);
    }

    /**
     * Create a new token chain when the current chain is too deep.
     */
    private TokenRotationResult createNewTokenChain(
            OAuthRefreshToken oldToken,
            OAuthAccessToken newAccessToken,
            String clientId,
            Set<String> scopes,
            String ipAddress,
            String userAgent) {

        // Generate new session ID for the new chain
        String newSessionId = UUID.randomUUID().toString();

        // Revoke the old chain
        oldToken.revoke("chain_depth_exceeded");
        refreshTokenRepository.save(oldToken);

        // Generate new refresh token
        String newTokenValue = generateSecureToken();
        String newTokenHash = hashToken(newTokenValue);

        // Create new refresh token with new session
        OAuthRefreshToken newRefreshToken = OAuthRefreshToken.builder()
                .tokenHash(newTokenHash)
                .userId(oldToken.getUserId())
                .clientId(oldToken.getClientId())
                .accessToken(newAccessToken)
                .scopes(new LinkedHashSet<>(scopes))
                .expiresAt(calculateRefreshTokenExpiry(oldToken.getClient()))
                .issuedIp(ipAddress)
                .userAgent(userAgent)
                .sessionId(newSessionId)
                .build();

        newRefreshToken = refreshTokenRepository.save(newRefreshToken);

        return TokenRotationResult.builder()
            .refreshToken(newRefreshToken)
            .tokenValue(newTokenValue)
            .build();
    }

    /**
     * Get the depth of a token chain.
     */
    private int getTokenChainDepth(OAuthRefreshToken token) {
        int depth = 0;
        OAuthRefreshToken current = token;

        while (current.getReplacedToken() != null && depth < maxChainDepth) {
            current = current.getReplacedToken();
            depth++;
        }

        return depth;
    }

    /**
     * Validate a refresh token for rotation eligibility.
     */
    public void validateTokenForRotation(OAuthRefreshToken token) {
        if (token == null) {
            throw new OAuth2AuthorizationException("Refresh token not found");
        }

        // Check if token is valid
        if (!token.isValid()) {
            if (token.isRevoked()) {
                throw new OAuth2AuthorizationException("Refresh token has been revoked");
            } else if (token.isExpired()) {
                throw new OAuth2AuthorizationException("Refresh token has expired");
            }
            throw new OAuth2AuthorizationException("Invalid or expired refresh token");
        }

        // Check if token has already been rotated (outside grace period)
        if (token.getExpiresAt() != null &&
            token.getExpiresAt().isBefore(Instant.now().minusSeconds(gracePeriodSeconds))) {

            // Token has been rotated and grace period has expired
            if (detectReuse) {
                handleTokenReuseDetected(token, token.getClient().getClientId(), "unknown");
            }
            throw new OAuth2AuthorizationException("Refresh token has already been used");
        }
    }

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hash a token value for storage.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Calculate refresh token expiry based on client configuration.
     */
    private Instant calculateRefreshTokenExpiry(OAuthClient client) {
        if (client == null || client.getRefreshTokenValiditySeconds() == null) {
            // Default to 30 days
            return Instant.now().plus(30, ChronoUnit.DAYS);
        }
        return Instant.now().plusSeconds(client.getRefreshTokenValiditySeconds());
    }

    /**
     * Get rotation statistics for monitoring.
     */
    public Map<String, Object> getRotationStatistics(String clientId) {
        Map<String, Object> stats = new HashMap<>();

        // Get rotation metrics from the metrics service
        stats.put("rotationEnabled", rotationEnabled);
        stats.put("gracePeriodSeconds", gracePeriodSeconds);
        stats.put("maxChainDepth", maxChainDepth);
        stats.put("detectReuse", detectReuse);
        stats.put("revokeFamilyOnReuse", revokeFamilyOnReuse);

        // Add client-specific stats if needed
        if (clientId != null) {
            Long rotationCount = refreshTokenRepository.countByClientId(UUID.fromString(clientId));
            stats.put("clientTokenCount", rotationCount);
        }

        return stats;
    }
}