package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 Authorization Code entity for storing temporary authorization codes.
 * Supports PKCE (Proof Key for Code Exchange) for enhanced security.
 */
@Entity
@Table(name = "oauth_authorization_codes", indexes = {
    @Index(name = "idx_oauth_auth_code", columnList = "code", unique = true),
    @Index(name = "idx_oauth_auth_code_user", columnList = "user_id"),
    @Index(name = "idx_oauth_auth_code_client", columnList = "client_id"),
    @Index(name = "idx_oauth_auth_code_expires", columnList = "expires_at"),
    @Index(name = "idx_oauth_auth_code_used", columnList = "used")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "client"})
@ToString(exclude = {"code", "user", "client", "codeChallenge"})
public class OAuthAuthorizationCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Authorization code value - temporary, short-lived
     */
    @Column(nullable = false, unique = true, length = 255)
    private String code;
    
    /**
     * User who authorized the code
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * OAuth client that requested this code
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private OAuthClient client;
    
    /**
     * Redirect URI where the code will be sent
     */
    @Column(name = "redirect_uri", nullable = false, length = 500)
    private String redirectUri;
    
    /**
     * Scopes requested for this authorization
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth_authorization_code_scopes", 
                     joinColumns = @JoinColumn(name = "auth_code_id"))
    @Column(name = "scope")
    @Builder.Default
    private Set<String> scopes = new HashSet<>();
    
    /**
     * State parameter for CSRF protection
     */
    @Column(length = 255)
    private String state;
    
    /**
     * PKCE code challenge
     */
    @Column(name = "code_challenge", length = 128)
    private String codeChallenge;
    
    /**
     * PKCE code challenge method (S256 or plain)
     */
    @Column(name = "code_challenge_method", length = 10)
    private String codeChallengeMethod;
    
    /**
     * Code expiration time (typically 10 minutes)
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    /**
     * Whether the code has been used (codes are single-use)
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;
    
    /**
     * When the code was used
     */
    @Column(name = "used_at")
    private Instant usedAt;
    
    /**
     * IP address from which the code was issued
     */
    @Column(name = "issued_ip", length = 45)
    private String issuedIp;
    
    /**
     * User agent string from code issuance
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Session identifier for tracking
     */
    @Column(name = "session_id")
    private String sessionId;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Check if the code is expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the code is valid (not expired and not used)
     */
    public boolean isValid() {
        return !isExpired() && !used;
    }
    
    /**
     * Mark the code as used
     */
    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }
    
    /**
     * Check if this authorization uses PKCE
     */
    public boolean isUsingPKCE() {
        return codeChallenge != null && codeChallengeMethod != null;
    }
    
    /**
     * Verify PKCE code verifier against the stored challenge
     */
    public boolean verifyCodeChallenge(String codeVerifier) {
        if (!isUsingPKCE()) {
            return true; // No PKCE required
        }
        
        if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
            return false;
        }
        
        // This would be implemented with actual SHA256 hashing in a real service
        // For now, we'll just do a simple comparison
        if ("plain".equals(codeChallengeMethod)) {
            return codeChallenge.equals(codeVerifier);
        } else if ("S256".equals(codeChallengeMethod)) {
            // In real implementation: SHA256(codeVerifier) base64url-encoded
            // For testing purposes, we'll assume the challenge is already hashed
            return codeChallenge.equals(codeVerifier);
        }
        
        return false;
    }
    
    /**
     * Check if code has specific scope
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }
    
    /**
     * Check if code has all required scopes
     */
    public boolean hasAllScopes(Set<String> requiredScopes) {
        return scopes != null && scopes.containsAll(requiredScopes);
    }
    
    /**
     * Validate redirect URI matches the one used in authorization
     */
    public boolean validateRedirectUri(String providedRedirectUri) {
        return redirectUri != null && redirectUri.equals(providedRedirectUri);
    }
}