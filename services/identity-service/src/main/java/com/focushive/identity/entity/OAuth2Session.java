package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an OAuth2 session.
 * Tracks active user sessions for OAuth2 flows and provides session management capabilities.
 */
@Entity
@Table(name = "oauth2_sessions", indexes = {
    @Index(name = "idx_oauth2_session_token", columnList = "session_token"),
    @Index(name = "idx_oauth2_session_user", columnList = "user_id"),
    @Index(name = "idx_oauth2_session_client", columnList = "client_id"),
    @Index(name = "idx_oauth2_session_expires", columnList = "expires_at"),
    @Index(name = "idx_oauth2_session_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2Session {

    /**
     * Unique identifier for the session.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The user associated with this session.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The OAuth2 client associated with this session.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private OAuthClient client;

    /**
     * Unique session token for session validation.
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    private String sessionToken;

    /**
     * The authentication method used (password, social, mfa, etc.).
     */
    @Column(name = "auth_method", length = 50)
    private String authMethod;

    /**
     * The authentication level (basic, elevated, mfa).
     */
    @Column(name = "auth_level", length = 50)
    @Builder.Default
    private String authLevel = "basic";

    /**
     * IP address from which the session was created.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent of the client that created the session.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Device fingerprint for enhanced security.
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * When the session was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the session was last accessed.
     */
    @Column(name = "last_accessed_at", nullable = false)
    @Builder.Default
    private Instant lastAccessedAt = Instant.now();

    /**
     * When the session expires.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Whether the session is currently active.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * The reason for session termination if not active.
     */
    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    /**
     * When the session was terminated.
     */
    @Column(name = "terminated_at")
    private Instant terminatedAt;

    /**
     * Additional session metadata in JSON format.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * The OAuth2 scopes granted in this session.
     */
    @Column(name = "granted_scopes", length = 1000)
    private String grantedScopes;

    /**
     * The ID token issued for this session (if OIDC).
     */
    @Column(name = "id_token", columnDefinition = "TEXT")
    private String idToken;

    /**
     * The refresh token associated with this session.
     */
    @OneToOne(mappedBy = "session", fetch = FetchType.LAZY)
    private OAuthRefreshToken refreshToken;

    /**
     * Check if the session is still valid.
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Terminate the session.
     */
    public void terminate(String reason) {
        this.active = false;
        this.terminationReason = reason;
        this.terminatedAt = Instant.now();
    }

    /**
     * Update the last accessed time.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = Instant.now();
    }

    /**
     * Check if session requires re-authentication based on idle time.
     */
    public boolean requiresReauthentication(long maxIdleSeconds) {
        if (!active || expiresAt.isBefore(Instant.now())) {
            return true;
        }

        Instant idleThreshold = Instant.now().minusSeconds(maxIdleSeconds);
        return lastAccessedAt.isBefore(idleThreshold);
    }

    /**
     * Check if session has elevated authentication level.
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
}