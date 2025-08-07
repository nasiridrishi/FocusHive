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
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a music playlist.
 * 
 * Playlists can be:
 * - Personal: Created by individual users for personal use
 * - Hive: Associated with a specific hive for group listening
 * - Collaborative: Multiple users can contribute tracks
 * - Public: Visible to other users in the system
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "playlists", schema = "music", indexes = {
    @Index(name = "idx_playlists_created_by", columnList = "createdBy"),
    @Index(name = "idx_playlists_hive_id", columnList = "hiveId"),
    @Index(name = "idx_playlists_collaborative", columnList = "isCollaborative")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The display name of the playlist.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Optional description of the playlist content or purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * User ID of the playlist creator.
     */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /**
     * Hive ID if this is a hive-specific playlist, null for personal playlists.
     */
    @Column(name = "hive_id")
    private String hiveId;

    /**
     * Whether multiple users can add/edit tracks in this playlist.
     */
    @Column(name = "is_collaborative")
    @Builder.Default
    private Boolean isCollaborative = false;

    /**
     * Whether this playlist is visible to other users.
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Spotify playlist ID if synchronized with Spotify.
     */
    @Column(name = "spotify_playlist_id")
    private String spotifyPlaylistId;

    /**
     * Total number of tracks in the playlist.
     */
    @Column(name = "total_tracks")
    @Builder.Default
    private Integer totalTracks = 0;

    /**
     * Total duration of all tracks in milliseconds.
     */
    @Column(name = "total_duration_ms")
    @Builder.Default
    private Long totalDurationMs = 0L;

    /**
     * URL to the playlist cover image.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Whether this is a smart playlist with auto-updating criteria.
     */
    @Column(name = "is_smart_playlist")
    @Builder.Default
    private Boolean isSmartPlaylist = false;

    /**
     * Smart playlist criteria stored as JSON.
     */
    @Column(name = "smart_criteria", columnDefinition = "TEXT")
    private String smartCriteria;

    /**
     * Last time the smart playlist was auto-updated.
     */
    @Column(name = "last_auto_update")
    private LocalDateTime lastAutoUpdate;

    /**
     * Whether this playlist is currently active.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Session ID for hive session playlists.
     */
    @Column(name = "session_id")
    private String sessionId;

    /**
     * Additional metadata stored as JSON.
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Tags for categorization and search.
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /**
     * Average productivity score for this playlist.
     */
    @Column(name = "avg_productivity_score")
    private Double avgProductivityScore;

    /**
     * Number of times this playlist has been played.
     */
    @Column(name = "play_count")
    @Builder.Default
    private Long playCount = 0L;

    /**
     * Tracks contained in this playlist.
     */
    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("positionInPlaylist ASC")
    @Builder.Default
    private List<PlaylistTrack> tracks = new ArrayList<>();

    /**
     * Collaborators who have access to modify this playlist.
     */
    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PlaylistCollaborator> collaborators = new ArrayList<>();

    /**
     * Timestamp when the playlist was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the playlist was last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if this is a personal playlist (not associated with a hive).
     * 
     * @return true if this is a personal playlist
     */
    public boolean isPersonal() {
        return hiveId == null;
    }

    /**
     * Checks if this is a hive playlist.
     * 
     * @return true if this playlist belongs to a hive
     */
    public boolean isHivePlaylist() {
        return hiveId != null;
    }

    /**
     * Adds a new track to the playlist and updates totals.
     * 
     * @param track The track to add
     */
    public void addTrack(PlaylistTrack track) {
        track.setPlaylist(this);
        track.setPositionInPlaylist(tracks.size());
        tracks.add(track);
        updateTotals();
    }

    /**
     * Removes a track from the playlist and updates positions/totals.
     * 
     * @param track The track to remove
     */
    public void removeTrack(PlaylistTrack track) {
        if (tracks.remove(track)) {
            reorderTracks();
            updateTotals();
        }
    }

    /**
     * Reorders tracks after a removal to maintain sequential positions.
     */
    private void reorderTracks() {
        for (int i = 0; i < tracks.size(); i++) {
            tracks.get(i).setPositionInPlaylist(i);
        }
    }

    /**
     * Updates the total tracks count and duration.
     */
    private void updateTotals() {
        this.totalTracks = tracks.size();
        this.totalDurationMs = tracks.stream()
            .mapToLong(PlaylistTrack::getDurationMs)
            .sum();
    }

    /**
     * Adds a collaborator to this playlist.
     * 
     * @param collaborator The collaborator to add
     */
    public void addCollaborator(PlaylistCollaborator collaborator) {
        collaborator.setPlaylist(this);
        collaborators.add(collaborator);
    }

    /**
     * Checks if a user has permission to modify this playlist.
     * 
     * @param userId The user ID to check
     * @return true if the user can modify the playlist
     */
    public boolean canUserModify(String userId) {
        // Owner can always modify
        if (createdBy.equals(userId)) {
            return true;
        }
        
        // Check collaborator permissions
        return collaborators.stream()
            .anyMatch(collab -> collab.getUserId().equals(userId) && 
                     collab.getCanAddTracks());
    }

    /**
     * Checks if this is a smart playlist.
     * 
     * @return true if this is a smart playlist
     */
    public boolean isSmartPlaylist() {
        return Boolean.TRUE.equals(isSmartPlaylist);
    }

    /**
     * Checks if this playlist is active.
     * 
     * @return true if this playlist is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Increments the play count.
     */
    public void incrementPlayCount() {
        this.playCount = (this.playCount != null ? this.playCount : 0L) + 1;
    }

    /**
     * Gets the formatted duration as HH:MM:SS.
     * 
     * @return formatted duration string
     */
    public String getFormattedDuration() {
        if (totalDurationMs == null || totalDurationMs == 0) {
            return "0:00";
        }
        
        long totalSeconds = totalDurationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Updates the smart criteria JSON.
     * 
     * @param criteria The criteria object to store
     */
    public void updateSmartCriteria(Object criteria) {
        // Implementation would convert criteria object to JSON
        // For now, storing as string representation
        this.smartCriteria = criteria != null ? criteria.toString() : null;
        this.lastAutoUpdate = LocalDateTime.now();
    }

    /**
     * Updates the metadata JSON.
     * 
     * @param metadataMap The metadata to store
     */
    public void updateMetadata(java.util.Map<String, Object> metadataMap) {
        // Implementation would convert map to JSON
        // For now, storing as string representation
        this.metadata = metadataMap != null ? metadataMap.toString() : null;
    }

    /**
     * Gets metadata as Map.
     * 
     * @return Metadata map
     */
    public java.util.Map<String, Object> getMetadata() {
        if (metadata == null || metadata.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(metadata, java.util.Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    /**
     * Sets metadata from Map.
     * 
     * @param metadataMap The metadata to set
     */
    public void setMetadata(java.util.Map<String, Object> metadataMap) {
        if (metadataMap == null || metadataMap.isEmpty()) {
            this.metadata = null;
            return;
        }
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            this.metadata = mapper.writeValueAsString(metadataMap);
        } catch (Exception e) {
            this.metadata = "{}";
        }
    }

    /**
     * Gets the track at a specific position.
     * 
     * @param position The position to get
     * @return The track at the position, or null if not found
     */
    public PlaylistTrack getTrackAtPosition(int position) {
        return tracks.stream()
            .filter(track -> track.getPositionInPlaylist() != null && 
                           track.getPositionInPlaylist() == position)
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if a track with the given Spotify ID exists in this playlist.
     * 
     * @param spotifyTrackId The Spotify track ID
     * @return true if the track exists
     */
    public boolean containsTrack(String spotifyTrackId) {
        return tracks.stream()
            .anyMatch(track -> spotifyTrackId.equals(track.getSpotifyTrackId()));
    }
}