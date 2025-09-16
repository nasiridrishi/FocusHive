package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for OAuth2 session operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2SessionResponse {

    /**
     * Session ID.
     */
    private UUID sessionId;

    /**
     * Session token for validation.
     */
    private String sessionToken;

    /**
     * User ID associated with the session.
     */
    private UUID userId;

    /**
     * Client ID associated with the session.
     */
    private String clientId;

    /**
     * Authentication method used.
     */
    private String authMethod;

    /**
     * Authentication level.
     */
    private String authLevel;

    /**
     * When the session was created.
     */
    private Instant createdAt;

    /**
     * When the session expires.
     */
    private Instant expiresAt;

    /**
     * When the session was last accessed.
     */
    private Instant lastAccessedAt;

    /**
     * Whether the session is active.
     */
    private boolean active;

    /**
     * Granted scopes for the session.
     */
    private String grantedScopes;

    /**
     * Session metadata.
     */
    private String metadata;

    /**
     * Time remaining until expiration in seconds.
     */
    public long getExpiresIn() {
        if (expiresAt == null) {
            return 0;
        }
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }

    /**
     * Check if session is expired.
     */
    public boolean isExpired() {
        return !active || (expiresAt != null && expiresAt.isBefore(Instant.now()));
    }

    /**
     * Check if session has elevated authentication.
     */
    public boolean hasElevatedAuth() {
        return "elevated".equals(authLevel) || "mfa".equals(authLevel);
    }

    /**
     * Check if session has MFA authentication.
     */
    public boolean hasMfaAuth() {
        return "mfa".equals(authLevel);
    }

    /**
     * Get session age in seconds.
     */
    public long getAgeInSeconds() {
        if (createdAt == null) {
            return 0;
        }
        return Instant.now().getEpochSecond() - createdAt.getEpochSecond();
    }

    /**
     * Get time since last access in seconds.
     */
    public long getIdleTimeInSeconds() {
        if (lastAccessedAt == null) {
            return getAgeInSeconds();
        }
        return Instant.now().getEpochSecond() - lastAccessedAt.getEpochSecond();
    }
}