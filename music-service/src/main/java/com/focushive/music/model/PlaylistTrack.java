package com.focushive.music.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing a track within a playlist.
 * 
 * Contains track metadata retrieved from Spotify and tracks
 * the position within the playlist and who added it.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "playlist_tracks", schema = "music", indexes = {
    @Index(name = "idx_playlist_tracks_playlist_id", columnList = "playlist_id"),
    @Index(name = "idx_playlist_tracks_position", columnList = "playlist_id, positionInPlaylist")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PlaylistTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The playlist this track belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    /**
     * Spotify track ID for API integration.
     */
    @Column(name = "spotify_track_id", nullable = false)
    private String spotifyTrackId;

    /**
     * The track name/title.
     */
    @Column(name = "track_name", nullable = false, length = 500)
    private String trackName;

    /**
     * Primary artist name.
     */
    @Column(name = "artist_name", nullable = false, length = 500)
    private String artistName;

    /**
     * Album name.
     */
    @Column(name = "album_name", length = 500)
    private String albumName;

    /**
     * Track duration in milliseconds.
     */
    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    /**
     * URL to a 30-second preview of the track (from Spotify).
     */
    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    /**
     * External URL to the track on Spotify.
     */
    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    /**
     * URL to the track/album artwork image.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Position of this track within the playlist (0-based index).
     */
    @Column(name = "position_in_playlist", nullable = false)
    private Integer positionInPlaylist;

    /**
     * User ID of who added this track to the playlist.
     */
    @Column(name = "added_by", nullable = false)
    private String addedBy;

    /**
     * Timestamp when the track was added to the playlist.
     */
    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    /**
     * Vote score for collaborative playlists.
     */
    @Column(name = "vote_score")
    @Builder.Default
    private Integer voteScore = 0;

    /**
     * Voting data stored as JSON for collaborative features.
     */
    @Column(name = "voting_data", columnDefinition = "TEXT")
    private String votingData;

    /**
     * Position index for sorting (alternative to positionInPlaylist).
     */
    @Column(name = "position")
    private Integer position;

    /**
     * Gets a human-readable duration string (e.g., "3:45").
     * 
     * @return Duration formatted as "MM:SS"
     */
    public String getFormattedDuration() {
        int totalSeconds = durationMs / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Gets the display name combining artist and track.
     * 
     * @return Formatted string like "Artist Name - Track Name"
     */
    public String getDisplayName() {
        return artistName + " - " + trackName;
    }

    /**
     * Checks if this track has a preview URL available.
     * 
     * @return true if preview is available
     */
    public boolean hasPreview() {
        return previewUrl != null && !previewUrl.trim().isEmpty();
    }

    /**
     * Gets track ID (alias for spotifyTrackId).
     * 
     * @return Spotify track ID
     */
    public String getTrackId() {
        return spotifyTrackId;
    }

    /**
     * Sets track ID (alias for spotifyTrackId).
     * 
     * @param trackId The track ID to set
     */
    public void setTrackId(String trackId) {
        this.spotifyTrackId = trackId;
    }

    /**
     * Gets voting data as Map for collaborative features.
     * 
     * @return Voting data map
     */
    public java.util.Map<String, Object> getVotingData() {
        if (votingData == null || votingData.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(votingData, java.util.Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    /**
     * Sets voting data from Map.
     * 
     * @param votingDataMap The voting data to set
     */
    public void setVotingData(java.util.Map<String, Object> votingDataMap) {
        if (votingDataMap == null || votingDataMap.isEmpty()) {
            this.votingData = null;
            return;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.votingData = mapper.writeValueAsString(votingDataMap);
        } catch (Exception e) {
            this.votingData = "{}";
        }
    }
}