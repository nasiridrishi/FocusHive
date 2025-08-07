package com.focushive.music.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event records for music service events.
 * 
 * Contains all event types used for internal event handling, WebSocket
 * communication, and inter-service event publishing.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
public final class MusicEvents {

    // Session Events
    
    /**
     * Event triggered when a session starts with music capabilities.
     */
    public record SessionStartEvent(
        UUID sessionId,
        UUID userId,
        UUID hiveId,
        String sessionType,
        boolean musicEnabled,
        Map<String, Object> preferences,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a session ends.
     */
    public record SessionEndEvent(
        UUID sessionId,
        UUID userId,
        UUID hiveId,
        boolean musicWasEnabled,
        long duration,
        Map<String, Object> metrics,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a session is updated.
     */
    public record SessionUpdateEvent(
        UUID sessionId,
        UUID userId,
        UUID hiveId,
        boolean musicEnabled,
        boolean musicPreferencesChanged,
        Map<String, Object> newMusicPreferences,
        LocalDateTime timestamp
    ) {}

    // Hive Events
    
    /**
     * Event triggered when a member joins a hive.
     */
    public record HiveMemberJoinEvent(
        UUID hiveId,
        UUID userId,
        String username,
        Map<String, Object> userMusicPreferences,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a member leaves a hive.
     */
    public record HiveMemberLeaveEvent(
        UUID hiveId,
        UUID userId,
        String username,
        LocalDateTime timestamp
    ) {}

    // Music Events
    
    /**
     * Event triggered when a music session starts.
     */
    public record MusicSessionStartedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        boolean musicEnabled,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a music session ends.
     */
    public record MusicSessionEndedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        long duration,
        Map<String, Object> metrics,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is played.
     */
    public record TrackPlayedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String trackId,
        String trackName,
        String artistId,
        String artistName,
        UUID playlistId,
        String source,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is skipped.
     */
    public record TrackSkippedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String trackId,
        String reason,
        long playDuration,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is liked.
     */
    public record TrackLikedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String trackId,
        String artistId,
        UUID playlistId,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is disliked.
     */
    public record TrackDislikedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String trackId,
        String artistId,
        String reason,
        LocalDateTime timestamp
    ) {}

    // Playlist Events
    
    /**
     * Event triggered when a playlist is created.
     */
    public record PlaylistCreatedEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        String name,
        String description,
        boolean isCollaborative,
        boolean isPublic,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a playlist is updated.
     */
    public record PlaylistUpdatedEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        String updateType,
        Map<String, Object> changes,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a playlist is deleted.
     */
    public record PlaylistDeletedEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        String name,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is added to a playlist.
     */
    public record TrackAddedToPlaylistEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        String trackId,
        String artistId,
        int position,
        String addedBy,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a track is removed from a playlist.
     */
    public record TrackRemovedFromPlaylistEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        String trackId,
        int position,
        String removedBy,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when playlist tracks are reordered.
     */
    public record PlaylistReorderedEvent(
        UUID playlistId,
        UUID userId,
        UUID hiveId,
        Map<String, Integer> newOrder,
        String reorderedBy,
        LocalDateTime timestamp
    ) {}

    // Collaborative Events
    
    /**
     * Event triggered when a collaborator is added to a playlist.
     */
    public record CollaboratorAddedEvent(
        UUID playlistId,
        UUID hiveId,
        UUID collaboratorId,
        String collaboratorName,
        String role,
        UUID addedBy,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a collaborator is removed from a playlist.
     */
    public record CollaboratorRemovedEvent(
        UUID playlistId,
        UUID hiveId,
        UUID collaboratorId,
        String collaboratorName,
        UUID removedBy,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when collaborative voting occurs on a track.
     */
    public record CollaborativeVoteEvent(
        UUID playlistId,
        UUID hiveId,
        UUID userId,
        String trackId,
        String voteType, // "upvote", "downvote", "skip_request"
        int newScore,
        LocalDateTime timestamp
    ) {}

    // Streaming Service Events
    
    /**
     * Event triggered when a streaming service is connected.
     */
    public record StreamingServiceConnectedEvent(
        UUID userId,
        String serviceName,
        String serviceUserId,
        Map<String, Object> connectionDetails,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a streaming service is disconnected.
     */
    public record StreamingServiceDisconnectedEvent(
        UUID userId,
        String serviceName,
        String reason,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when streaming service data is synchronized.
     */
    public record StreamingServiceSyncEvent(
        UUID userId,
        String serviceName,
        String syncType, // "playlists", "liked_tracks", "preferences"
        int itemsSync,
        LocalDateTime timestamp
    ) {}

    // Recommendation Events
    
    /**
     * Event triggered when music recommendations are generated.
     */
    public record RecommendationsGeneratedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String recommendationType,
        int recommendationCount,
        Map<String, Object> criteria,
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a recommendation is accepted.
     */
    public record RecommendationAcceptedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String recommendationId,
        String trackId,
        String artistId,
        String acceptanceType, // "played", "added_to_playlist", "liked"
        LocalDateTime timestamp
    ) {}

    /**
     * Event triggered when a recommendation is rejected.
     */
    public record RecommendationRejectedEvent(
        UUID userId,
        UUID sessionId,
        UUID hiveId,
        String recommendationId,
        String trackId,
        String artistId,
        String rejectionReason,
        LocalDateTime timestamp
    ) {}
}