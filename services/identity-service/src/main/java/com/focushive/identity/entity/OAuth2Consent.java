package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 consent entity for managing user consent to client access.
 * Tracks which scopes users have granted to which clients.
 */
@Entity
@Table(name = "oauth2_consents",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "client_id"})
    },
    indexes = {
        @Index(name = "idx_oauth2_consent_user", columnList = "user_id"),
        @Index(name = "idx_oauth2_consent_client", columnList = "client_id"),
        @Index(name = "idx_oauth2_consent_expires", columnList = "expires_at"),
        @Index(name = "idx_oauth2_consent_revoked", columnList = "revoked")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "client"})
@ToString(exclude = {"user", "client"})
public class OAuth2Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * The user who granted the consent.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The client to which consent was granted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private OAuthClient client;

    /**
     * Scopes that the user has consented to.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "oauth2_consent_scopes",
        joinColumns = @JoinColumn(name = "consent_id")
    )
    @Column(name = "scope")
    @OrderBy("scope ASC")
    @Builder.Default
    private Set<String> grantedScopes = new LinkedHashSet<>();

    /**
     * Scopes that were requested but denied by the user.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "oauth2_consent_denied_scopes",
        joinColumns = @JoinColumn(name = "consent_id")
    )
    @Column(name = "scope")
    @OrderBy("scope ASC")
    @Builder.Default
    private Set<String> deniedScopes = new LinkedHashSet<>();

    /**
     * When the consent was first granted.
     */
    @CreationTimestamp
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    /**
     * When the consent was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * When the consent expires (optional).
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Whether the consent has been revoked.
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * When the consent was revoked (if applicable).
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Reason for revocation (if applicable).
     */
    @Column(name = "revocation_reason")
    private String revocationReason;

    /**
     * IP address from which consent was granted.
     */
    @Column(name = "granted_ip", length = 45)
    private String grantedIp;

    /**
     * User agent that granted the consent.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Session ID associated with the consent.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Whether to remember this consent decision (skip prompt next time).
     */
    @Column(name = "remember_consent", nullable = false)
    @Builder.Default
    private boolean rememberConsent = false;

    /**
     * Additional metadata about the consent (stored as JSON).
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Check if the consent is valid (not expired and not revoked).
     */
    public boolean isValid() {
        if (revoked) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Check if a specific scope is granted.
     */
    public boolean hasScope(String scope) {
        return isValid() && grantedScopes.contains(scope);
    }

    /**
     * Check if all specified scopes are granted.
     */
    public boolean hasAllScopes(Set<String> scopes) {
        return isValid() && grantedScopes.containsAll(scopes);
    }

    /**
     * Add a granted scope.
     */
    public void grantScope(String scope) {
        grantedScopes.add(scope);
        deniedScopes.remove(scope);
        updatedAt = Instant.now();
    }

    /**
     * Remove a granted scope.
     */
    public void revokeScope(String scope) {
        grantedScopes.remove(scope);
        deniedScopes.add(scope);
        updatedAt = Instant.now();
    }

    /**
     * Revoke the entire consent.
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }

    /**
     * Extend the consent expiration.
     */
    public void extendExpiration(long additionalSeconds) {
        if (expiresAt == null) {
            expiresAt = Instant.now().plusSeconds(additionalSeconds);
        } else {
            expiresAt = expiresAt.plusSeconds(additionalSeconds);
        }
        updatedAt = Instant.now();
    }
}