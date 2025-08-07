package com.focushive.music.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity for storing encrypted streaming service credentials.
 * 
 * Supports multiple streaming platforms (Spotify, Apple Music, etc.) 
 * and manages OAuth tokens with automatic refresh capabilities.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "streaming_credentials", schema = "music",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_streaming_credentials_user_platform", 
                           columnNames = {"user_id", "platform"})
       },
       indexes = {
           @Index(name = "idx_streaming_credentials_user_id", columnList = "user_id"),
           @Index(name = "idx_streaming_credentials_platform", columnList = "platform")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StreamingCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID from the identity service.
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Streaming platform identifier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private StreamingPlatform platform;

    /**
     * Encrypted OAuth access token.
     * Should be encrypted at application level before storing.
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    /**
     * Encrypted OAuth refresh token.
     * Should be encrypted at application level before storing.
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /**
     * Token expiration timestamp.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * OAuth scope permissions granted.
     */
    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    /**
     * Whether these credentials are currently active/valid.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Timestamp when the credentials were created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the credentials were last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enumeration of supported streaming platforms.
     */
    public enum StreamingPlatform {
        SPOTIFY("Spotify"),
        APPLE_MUSIC("Apple Music"),
        YOUTUBE_MUSIC("YouTube Music"),
        AMAZON_MUSIC("Amazon Music"),
        DEEZER("Deezer"),
        TIDAL("Tidal");

        private final String displayName;

        StreamingPlatform(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Checks if the access token is expired or about to expire.
     * 
     * @return true if token needs refresh
     */
    public boolean isTokenExpired() {
        if (expiresAt == null) {
            return true;
        }
        // Consider expired if less than 5 minutes remaining
        return expiresAt.isBefore(LocalDateTime.now().plusMinutes(5));
    }

    /**
     * Checks if these credentials can be used for API calls.
     * 
     * @return true if credentials are active and not expired
     */
    public boolean isValid() {
        return isActive && accessToken != null && !isTokenExpired();
    }

    /**
     * Updates the access token and expiration.
     * 
     * @param newAccessToken The new access token
     * @param expiresIn Seconds until expiration
     */
    public void updateAccessToken(String newAccessToken, long expiresIn) {
        this.accessToken = newAccessToken;
        this.expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        this.isActive = true;
    }

    /**
     * Updates both access and refresh tokens.
     * 
     * @param newAccessToken The new access token
     * @param newRefreshToken The new refresh token
     * @param expiresIn Seconds until expiration
     */
    public void updateTokens(String newAccessToken, String newRefreshToken, long expiresIn) {
        updateAccessToken(newAccessToken, expiresIn);
        this.refreshToken = newRefreshToken;
    }

    /**
     * Marks these credentials as inactive/revoked.
     */
    public void deactivate() {
        this.isActive = false;
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAt = null;
    }

    /**
     * Gets a masked version of the access token for logging.
     * 
     * @return Partially masked token
     */
    public String getMaskedAccessToken() {
        if (accessToken == null || accessToken.length() < 8) {
            return "***";
        }
        return accessToken.substring(0, 4) + "***" + 
               accessToken.substring(accessToken.length() - 4);
    }

    /**
     * Checks if the user has granted specific scope permissions.
     * 
     * @param requiredScope The scope to check for
     * @return true if the scope is granted
     */
    public boolean hasScope(String requiredScope) {
        if (scope == null) {
            return false;
        }
        return scope.contains(requiredScope);
    }
}