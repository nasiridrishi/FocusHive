package com.focushive.identity.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a token is revoked.
 * Used for distributed cache invalidation across service instances.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationEvent {

    /**
     * Type of token being revoked
     */
    public enum TokenType {
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        ALL_TOKENS
    }

    /**
     * Unique event ID for idempotency
     */
    private String eventId;

    /**
     * Type of token being revoked
     */
    private TokenType tokenType;

    /**
     * Token hash (not the actual token for security)
     */
    private String tokenHash;

    /**
     * User ID associated with the token
     */
    private UUID userId;

    /**
     * Client ID that issued the token
     */
    private UUID clientId;

    /**
     * Reason for revocation
     */
    private String revocationReason;

    /**
     * Timestamp when revocation occurred
     */
    private Instant revokedAt;

    /**
     * Service instance that initiated the revocation
     */
    private String sourceInstance;

    /**
     * Whether to cascade revocation to related tokens
     */
    private boolean cascadeRevocation;

    /**
     * Create event for access token revocation
     */
    public static TokenRevocationEvent accessTokenRevoked(String tokenHash, UUID userId,
                                                          UUID clientId, String reason) {
        return TokenRevocationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tokenType(TokenType.ACCESS_TOKEN)
            .tokenHash(tokenHash)
            .userId(userId)
            .clientId(clientId)
            .revocationReason(reason)
            .revokedAt(Instant.now())
            .cascadeRevocation(false)
            .build();
    }

    /**
     * Create event for refresh token revocation with cascade
     */
    public static TokenRevocationEvent refreshTokenRevoked(String tokenHash, UUID userId,
                                                           UUID clientId, String reason) {
        return TokenRevocationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tokenType(TokenType.REFRESH_TOKEN)
            .tokenHash(tokenHash)
            .userId(userId)
            .clientId(clientId)
            .revocationReason(reason)
            .revokedAt(Instant.now())
            .cascadeRevocation(true) // Refresh token revocation cascades
            .build();
    }

    /**
     * Create event for revoking all tokens for a user/client combination
     */
    public static TokenRevocationEvent allTokensRevoked(UUID userId, UUID clientId, String reason) {
        return TokenRevocationEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .tokenType(TokenType.ALL_TOKENS)
            .userId(userId)
            .clientId(clientId)
            .revocationReason(reason)
            .revokedAt(Instant.now())
            .cascadeRevocation(true)
            .build();
    }
}