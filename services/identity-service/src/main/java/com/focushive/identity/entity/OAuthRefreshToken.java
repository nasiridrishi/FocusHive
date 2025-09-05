package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 Refresh Token entity for storing issued refresh tokens.
 * Supports token rotation and long-lived authentication.
 */
@Entity
@Table(name = "oauth_refresh_tokens", indexes = {
    @Index(name = "idx_oauth_refresh_token_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_oauth_refresh_token_user", columnList = "user_id"),
    @Index(name = "idx_oauth_refresh_token_client", columnList = "client_id"),
    @Index(name = "idx_oauth_refresh_token_expires", columnList = "expires_at"),
    @Index(name = "idx_oauth_refresh_token_revoked", columnList = "revoked"),
    @Index(name = "idx_oauth_refresh_token_access", columnList = "access_token_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "client", "accessToken", "replacedToken"})
@ToString(exclude = {"tokenHash", "user", "client", "accessToken"})
public class OAuthRefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Hashed token value for security - never store actual token
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    
    /**
     * User ID who owns this token
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    /**
     * User who owns this token
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;
    
    /**
     * OAuth client ID that requested this token
     */
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    /**
     * OAuth client that requested this token
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, insertable = false, updatable = false)
    private OAuthClient client;
    
    /**
     * Associated access token (optional - for tracking)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_token_id")
    private OAuthAccessToken accessToken;
    
    /**
     * Scopes associated with this refresh token
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth_refresh_token_scopes", 
                     joinColumns = @JoinColumn(name = "token_id"))
    @Column(name = "scope")
    @Builder.Default
    private Set<String> scopes = new HashSet<>();
    
    /**
     * Token expiration time (can be null for non-expiring tokens)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    /**
     * Whether the token has been revoked
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
    
    /**
     * When the token was revoked (if applicable)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    /**
     * Reason for revocation
     */
    @Column(name = "revocation_reason")
    private String revocationReason;
    
    /**
     * IP address from which the token was issued
     */
    @Column(name = "issued_ip", length = 45)
    private String issuedIp;
    
    /**
     * User agent string from token issuance
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * When the token was last used
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    /**
     * Usage count for analytics
     */
    @Column(name = "usage_count")
    @Builder.Default
    private Long usageCount = 0L;
    
    /**
     * Previous refresh token that this token replaces (for rotation)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_token_id")
    private OAuthRefreshToken replacedToken;
    
    /**
     * Session identifier for tracking related tokens
     */
    @Column(name = "session_id")
    private String sessionId;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Check if the token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the token is valid (not expired and not revoked)
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }
    
    /**
     * Mark the token as used
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
        this.usageCount = (this.usageCount != null ? this.usageCount : 0L) + 1;
    }
    
    /**
     * Revoke the token
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
    
    /**
     * Check if token has specific scope
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
    
    /**
     * Check if token has all required scopes
     */
    public boolean hasAllScopes(Set<String> requiredScopes) {
        return scopes != null && scopes.containsAll(requiredScopes);
    }
    
    /**
     * Create a rotated token (mark this one as replaced)
     */
    public void markAsReplaced() {
        this.revoke("token_rotation");
    }
    
    /**
     * Check if this token is part of a rotation chain
     */
    public boolean isRotated() {
        return replacedToken != null;
    }
}