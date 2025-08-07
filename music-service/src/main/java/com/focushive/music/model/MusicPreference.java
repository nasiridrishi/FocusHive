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
import java.util.List;

/**
 * Entity representing a user's music preferences and streaming service connections.
 * 
 * This entity stores information about:
 * - User's preferred music genres and artists
 * - Energy level and mood preferences for recommendations
 * - Spotify integration details and tokens
 * - Last listening activity tracking
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_music_preferences", schema = "music")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MusicPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique identifier of the user from the identity service.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /**
     * List of preferred music genres (e.g., "rock", "jazz", "electronic").
     */
    @ElementCollection
    @CollectionTable(
        name = "user_preferred_genres", 
        schema = "music",
        joinColumns = @JoinColumn(name = "preference_id")
    )
    @Column(name = "genre")
    private List<String> preferredGenres;

    /**
     * List of preferred artists.
     */
    @ElementCollection
    @CollectionTable(
        name = "user_preferred_artists",
        schema = "music", 
        joinColumns = @JoinColumn(name = "preference_id")
    )
    @Column(name = "artist")
    private List<String> preferredArtists;

    /**
     * Preferred energy level for music recommendations (1-10 scale).
     * 1 = Very calm/ambient, 10 = Very energetic/intense
     */
    @Column(name = "preferred_energy_level")
    private Integer preferredEnergyLevel;

    /**
     * Preferred mood for music (e.g., "focus", "relaxed", "energetic", "creative").
     */
    @Column(name = "preferred_mood", length = 50)
    private String preferredMood;

    /**
     * Whether the user has connected their Spotify account.
     */
    @Column(name = "spotify_connected")
    @Builder.Default
    private Boolean spotifyConnected = false;

    /**
     * Encrypted Spotify access token for API calls.
     * Should be encrypted at application level before storing.
     */
    @Column(name = "spotify_access_token", columnDefinition = "TEXT")
    private String spotifyAccessToken;

    /**
     * Encrypted Spotify refresh token for token renewal.
     * Should be encrypted at application level before storing.
     */
    @Column(name = "spotify_refresh_token", columnDefinition = "TEXT")
    private String spotifyRefreshToken;

    /**
     * Expiration timestamp for the Spotify access token.
     */
    @Column(name = "spotify_expires_at")
    private LocalDateTime spotifyExpiresAt;

    /**
     * Timestamp of the user's last music listening activity.
     * Used for recommendation personalization and analytics.
     */
    @Column(name = "last_listening_activity")
    private LocalDateTime lastListeningActivity;

    /**
     * Timestamp when the preference record was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the preference record was last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if the Spotify access token is expired or about to expire.
     * 
     * @return true if token is expired or expires within the next 5 minutes
     */
    public boolean isSpotifyTokenExpired() {
        if (spotifyExpiresAt == null) {
            return true;
        }
        return spotifyExpiresAt.isBefore(LocalDateTime.now().plusMinutes(5));
    }

    /**
     * Updates the last listening activity to the current timestamp.
     */
    public void updateLastListeningActivity() {
        this.lastListeningActivity = LocalDateTime.now();
    }

    /**
     * Updates Spotify token information.
     * 
     * @param accessToken The new access token
     * @param refreshToken The new refresh token (optional)
     * @param expiresAt The expiration timestamp
     */
    public void updateSpotifyTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.spotifyAccessToken = accessToken;
        if (refreshToken != null) {
            this.spotifyRefreshToken = refreshToken;
        }
        this.spotifyExpiresAt = expiresAt;
        this.spotifyConnected = true;
    }
}