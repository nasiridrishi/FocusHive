package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for playlist operations.
 * 
 * Contains request and response DTOs for playlist management,
 * including creation, updates, collaboration, and track management.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
public class PlaylistDTO {

    /**
     * Request DTO for creating a new playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateRequest {
        
        /**
         * The playlist name.
         */
        @NotBlank(message = "Playlist name is required")
        @Size(min = 1, max = 255, message = "Playlist name must be between 1 and 255 characters")
        private String name;
        
        /**
         * Optional playlist description.
         */
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
        
        /**
         * Hive ID for hive playlists, null for personal playlists.
         */
        private String hiveId;
        
        /**
         * Whether the playlist is collaborative.
         */
        @Builder.Default
        private Boolean isCollaborative = false;
        
        /**
         * Whether the playlist is public.
         */
        @Builder.Default
        private Boolean isPublic = false;
        
        /**
         * URL to playlist cover image.
         */
        private String imageUrl;
        
        /**
         * Initial tracks to add to the playlist.
         */
        @Valid
        private List<AddTrackRequest> initialTracks;
    }
    
    /**
     * Request DTO for updating a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateRequest {
        
        /**
         * Updated playlist name.
         */
        @Size(min = 1, max = 255, message = "Playlist name must be between 1 and 255 characters")
        private String name;
        
        /**
         * Updated description.
         */
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
        
        /**
         * Updated public status.
         */
        private Boolean isPublic;
        
        /**
         * Updated image URL.
         */
        private String imageUrl;
    }
    
    /**
     * Response DTO for playlist information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        
        /**
         * Playlist unique identifier.
         */
        private Long id;
        
        /**
         * Playlist name.
         */
        private String name;
        
        /**
         * Playlist description.
         */
        private String description;
        
        /**
         * User ID of the creator.
         */
        private String createdBy;
        
        /**
         * Hive ID if this is a hive playlist.
         */
        private String hiveId;
        
        /**
         * Whether the playlist is collaborative.
         */
        private Boolean isCollaborative;
        
        /**
         * Whether the playlist is public.
         */
        private Boolean isPublic;
        
        /**
         * Spotify playlist ID if synced.
         */
        private String spotifyPlaylistId;
        
        /**
         * Total number of tracks.
         */
        private Integer totalTracks;
        
        /**
         * Total duration in milliseconds.
         */
        private Long totalDurationMs;
        
        /**
         * Playlist cover image URL.
         */
        private String imageUrl;
        
        /**
         * Creation timestamp.
         */
        private LocalDateTime createdAt;
        
        /**
         * Last update timestamp.
         */
        private LocalDateTime updatedAt;
        
        /**
         * List of tracks in the playlist.
         */
        @Valid
        private List<TrackInfo> tracks;
        
        /**
         * List of collaborators.
         */
        @Valid
        private List<CollaboratorInfo> collaborators;
        
        /**
         * User's permission level for this playlist.
         */
        private String userPermission;
        
        /**
         * Whether the current user can modify the playlist.
         */
        private Boolean canModify;
        
        /**
         * Formatted duration string.
         */
        private String formattedDuration;
    }
    
    /**
     * Request DTO for adding a track to a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddTrackRequest {
        
        /**
         * Spotify track ID.
         */
        @NotBlank(message = "Spotify track ID is required")
        private String spotifyTrackId;
        
        /**
         * Position to insert the track (optional, appends if null).
         */
        @Min(value = 0, message = "Position must be non-negative")
        private Integer position;
        
        /**
         * Track name (optional, fetched from Spotify if not provided).
         */
        private String trackName;
        
        /**
         * Artist name (optional, fetched from Spotify if not provided).
         */
        private String artistName;
        
        /**
         * Album name (optional, fetched from Spotify if not provided).
         */
        private String albumName;
        
        /**
         * Track duration in milliseconds (optional).
         */
        private Integer durationMs;
        
        /**
         * Track image URL (optional).
         */
        private String imageUrl;
        
        /**
         * Preview URL (optional).
         */
        private String previewUrl;
        
        /**
         * External URL (optional).
         */
        private String externalUrl;
    }
    
    /**
     * DTO for track information within a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackInfo {
        
        /**
         * Track database ID.
         */
        private Long id;
        
        /**
         * Spotify track ID.
         */
        private String spotifyTrackId;
        
        /**
         * Track name.
         */
        private String trackName;
        
        /**
         * Artist name.
         */
        private String artistName;
        
        /**
         * Album name.
         */
        private String albumName;
        
        /**
         * Duration in milliseconds.
         */
        private Integer durationMs;
        
        /**
         * Preview URL.
         */
        private String previewUrl;
        
        /**
         * External Spotify URL.
         */
        private String externalUrl;
        
        /**
         * Album artwork URL.
         */
        private String imageUrl;
        
        /**
         * Position in playlist.
         */
        private Integer positionInPlaylist;
        
        /**
         * User who added the track.
         */
        private String addedBy;
        
        /**
         * When the track was added.
         */
        private LocalDateTime addedAt;
        
        /**
         * Formatted duration string.
         */
        private String formattedDuration;
        
        /**
         * Display name (Artist - Track).
         */
        private String displayName;
    }
    
    /**
     * DTO for playlist collaborator information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CollaboratorInfo {
        
        /**
         * Collaborator user ID.
         */
        private String userId;
        
        /**
         * Permission level.
         */
        private String permissionLevel;
        
        /**
         * Specific permissions.
         */
        private Boolean canAddTracks;
        private Boolean canRemoveTracks;
        private Boolean canReorderTracks;
        private Boolean canEditPlaylist;
        private Boolean canInviteOthers;
        
        /**
         * Who added this collaborator.
         */
        private String addedBy;
        
        /**
         * When the collaborator was added.
         */
        private LocalDateTime addedAt;
    }
    
    /**
     * Request DTO for adding a collaborator.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddCollaboratorRequest {
        
        /**
         * User ID to add as collaborator.
         */
        @NotBlank(message = "User ID is required")
        private String userId;
        
        /**
         * Permission level to grant.
         */
        @NotBlank(message = "Permission level is required")
        private String permissionLevel;
        
        /**
         * Custom permissions (override defaults).
         */
        private Boolean canAddTracks;
        private Boolean canRemoveTracks;
        private Boolean canReorderTracks;
        private Boolean canEditPlaylist;
        private Boolean canInviteOthers;
    }
    
    /**
     * Response DTO for playlist search results.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchResult {
        
        /**
         * Search results.
         */
        @Valid
        private List<Response> playlists;
        
        /**
         * Total number of results.
         */
        private Long totalResults;
        
        /**
         * Current page number.
         */
        private Integer currentPage;
        
        /**
         * Total number of pages.
         */
        private Integer totalPages;
        
        /**
         * Search query used.
         */
        private String query;
    }
    
    /**
     * Request DTO for reordering tracks in a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackReorderRequest {
        
        /**
         * Map of track ID to new position.
         */
        @NotEmpty(message = "Track orders cannot be empty")
        private Map<Long, Integer> trackOrders;
    }
    
    /**
     * Request DTO for creating smart playlists with criteria.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SmartPlaylistCriteriaRequest {
        
        /**
         * Smart playlist name.
         */
        @NotBlank(message = "Smart playlist name is required")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        private String name;
        
        /**
         * Optional description.
         */
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
        
        /**
         * Energy level filter (LOW, MEDIUM, HIGH).
         */
        private String energyLevel;
        
        /**
         * Minimum track duration in milliseconds.
         */
        @Min(value = 0, message = "Duration must be non-negative")
        private Integer minDurationMs;
        
        /**
         * Maximum track duration in milliseconds.
         */
        @Min(value = 0, message = "Duration must be non-negative")
        private Integer maxDurationMs;
        
        /**
         * List of preferred genres.
         */
        private List<String> genres;
        
        /**
         * Task type for productivity-based playlists.
         */
        private String taskType;
        
        /**
         * Whether to include only instrumental tracks.
         */
        private Boolean instrumentalOnly;
        
        /**
         * Minimum productivity score for tracks.
         */
        @DecimalMin(value = "0.0", message = "Productivity score must be non-negative")
        @DecimalMax(value = "100.0", message = "Productivity score cannot exceed 100")
        private Double minProductivityScore;
        
        /**
         * Time of day preference (MORNING, AFTERNOON, EVENING, NIGHT).
         */
        private String timeOfDay;
        
        /**
         * Maximum number of tracks in the playlist.
         */
        @Min(value = 1, message = "Max tracks must be at least 1")
        @Max(value = 500, message = "Max tracks cannot exceed 500")
        private Integer maxTracks;
        
        /**
         * Auto-update frequency in hours (0 = no auto-update).
         */
        @Min(value = 0, message = "Update frequency must be non-negative")
        private Integer autoUpdateFrequencyHours;
        
        /**
         * Minimum energy level (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double minEnergy;
        
        /**
         * Maximum energy level (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double maxEnergy;
        
        /**
         * Minimum danceability (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double minDanceability;
        
        /**
         * Maximum danceability (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double maxDanceability;
        
        /**
         * Minimum valence/positivity (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double minValence;
        
        /**
         * Maximum valence/positivity (0.0 - 1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double maxValence;
        
        /**
         * Release date start filter.
         */
        private java.time.LocalDate releaseDateStart;
        
        /**
         * Release date end filter.
         */
        private java.time.LocalDate releaseDateEnd;
        
        /**
         * List of preferred artists.
         */
        private List<String> artists;
        
        /**
         * Hive ID if creating a hive smart playlist.
         */
        private String hiveId;
        
        /**
         * Whether to make the playlist public.
         */
        private Boolean isPublic;
        
        /**
         * Whether to make the playlist collaborative.
         */
        private Boolean isCollaborative;
        
        /**
         * Playlist image URL.
         */
        private String imageUrl;
    }
    
    /**
     * Request DTO for sharing playlists with hive members.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SharePlaylistRequest {
        
        /**
         * Hive ID to share with.
         */
        @NotBlank(message = "Hive ID is required")
        private String hiveId;
        
        /**
         * Permission level for shared access.
         */
        @NotBlank(message = "Permission level is required")
        private String permissionLevel;
        
        /**
         * Optional message to include with the share.
         */
        @Size(max = 500, message = "Message cannot exceed 500 characters")
        private String message;
        
        /**
         * Whether to allow hive members to invite others.
         */
        private Boolean allowInvites;
    }
    
    /**
     * Request DTO for exporting playlists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExportRequest {
        
        /**
         * Export format (JSON, M3U, XSPF).
         */
        @NotBlank(message = "Export format is required")
        private String format;
        
        /**
         * Whether to include track metadata.
         */
        private Boolean includeMetadata;
        
        /**
         * Whether to include playlist statistics.
         */
        private Boolean includeStatistics;
    }
    
    /**
     * Response DTO for playlist export.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExportResponse {
        
        /**
         * Exported playlist name.
         */
        private String playlistName;
        
        /**
         * Export format used.
         */
        private String format;
        
        /**
         * Exported data content.
         */
        private String content;
        
        /**
         * Export timestamp.
         */
        private LocalDateTime exportedAt;
        
        /**
         * Total tracks exported.
         */
        private Integer totalTracks;
        
        /**
         * File size in bytes.
         */
        private Long fileSizeBytes;
    }
    
    /**
     * Request DTO for importing playlists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImportRequest {
        
        /**
         * Import source (SPOTIFY, YOUTUBE, FILE).
         */
        @NotBlank(message = "Import source is required")
        private String source;
        
        /**
         * External playlist ID or file content.
         */
        @NotBlank(message = "External playlist ID or content is required")
        private String externalPlaylistId;
        
        /**
         * Whether to import tracks or just metadata.
         */
        private Boolean importTracks;
        
        /**
         * New playlist name (optional).
         */
        @Size(max = 255, message = "Playlist name cannot exceed 255 characters")
        private String newPlaylistName;
        
        /**
         * Whether to make the imported playlist public.
         */
        private Boolean makePublic;
    }
    
    /**
     * Request DTO for duplicating playlists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DuplicateRequest {
        
        /**
         * New playlist name.
         */
        @NotBlank(message = "New playlist name is required")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        private String newName;
        
        /**
         * Optional new description.
         */
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String newDescription;
        
        /**
         * Whether to include collaborators in the duplicate.
         */
        private Boolean includeCollaborators;
        
        /**
         * Whether to make the duplicate public.
         */
        private Boolean makePublic;
        
        /**
         * Target hive ID if creating a hive playlist.
         */
        private String targetHiveId;
    }
    
    /**
     * Response DTO for playlist statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatisticsResponse {
        
        /**
         * Total number of plays.
         */
        private Long totalPlays;
        
        /**
         * Number of unique listeners.
         */
        private Integer uniqueListeners;
        
        /**
         * Average rating.
         */
        private Double averageRating;
        
        /**
         * Most played track.
         */
        private TrackInfo mostPlayedTrack;
        
        /**
         * Total duration listened in milliseconds.
         */
        private Long totalDurationListened;
        
        /**
         * Listening statistics by time of day.
         */
        private Map<String, Integer> listeningByTimeOfDay;
        
        /**
         * Genre distribution.
         */
        private Map<String, Integer> genreDistribution;
    }
    
    // ===============================
    // BATCH OPERATIONS DTOs
    // ===============================
    
    /**
     * Request DTO for batch adding tracks to a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchAddTracksRequest {
        
        @NotEmpty(message = "Track list cannot be empty")
        @Valid
        private List<AddTrackRequest> tracks;
        
        /**
         * Whether to skip tracks that are already in the playlist.
         */
        private Boolean skipDuplicates;
    }
    
    /**
     * Request DTO for batch removing tracks from a playlist.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchRemoveTracksRequest {
        
        @NotEmpty(message = "Track ID list cannot be empty")
        private List<Long> trackIds;
    }
    
    /**
     * Response DTO for batch track operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BatchTrackResponse {
        
        private Long playlistId;
        private Integer totalRequested;
        private Integer successCount;
        private Integer skipCount;
        private List<TrackInfo> addedTracks;
        private List<Long> removedTrackIds;
        private List<FailedTrackInfo> failedTracks;
        
        public static class Builder {
            private final BatchTrackResponse response = new BatchTrackResponse();
            
            public Builder playlistId(Long playlistId) {
                response.setPlaylistId(playlistId);
                return this;
            }
            
            public Builder totalRequested(Integer total) {
                response.setTotalRequested(total);
                return this;
            }
            
            public Builder successCount(Integer count) {
                response.setSuccessCount(count);
                return this;
            }
            
            public Builder skipCount(Integer count) {
                response.setSkipCount(count);
                return this;
            }
            
            public Builder addedTracks(List<TrackInfo> tracks) {
                response.setAddedTracks(tracks);
                return this;
            }
            
            public Builder addedTrack(TrackInfo track) {
                if (response.getAddedTracks() == null) {
                    response.setAddedTracks(new java.util.ArrayList<>());
                }
                response.getAddedTracks().add(track);
                return this;
            }
            
            public Builder removedTrackIds(List<Long> trackIds) {
                response.setRemovedTrackIds(trackIds);
                return this;
            }
            
            public Builder removedTrackId(Long trackId) {
                if (response.getRemovedTrackIds() == null) {
                    response.setRemovedTrackIds(new java.util.ArrayList<>());
                }
                response.getRemovedTrackIds().add(trackId);
                return this;
            }
            
            public Builder failedTrack(FailedTrackInfo failedTrack) {
                if (response.getFailedTracks() == null) {
                    response.setFailedTracks(new java.util.ArrayList<>());
                }
                response.getFailedTracks().add(failedTrack);
                return this;
            }
            
            public BatchTrackResponse build() {
                return response;
            }
        }
    }
    
    /**
     * DTO for failed track operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FailedTrackInfo {
        private String spotifyTrackId;
        private Long trackId;
        private String reason;
    }
    
    // ===============================
    // SHARING & COLLABORATION DTOs
    // ===============================
    
    /**
     * Response DTO for sharing operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SharingResponse {
        
        private Long playlistId;
        private String hiveId;
        private Integer totalHiveMembers;
        private Integer collaboratorsAdded;
        private Integer usersSkipped;
        private List<String> addedCollaboratorIds;
        private List<String> skippedUserIds;
        private String permissionLevel;
        private LocalDateTime sharedAt;
    }
    
    /**
     * Request DTO for updating collaborator permissions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateCollaboratorRequest {
        
        private String permissionLevel;
        private Boolean canAddTracks;
        private Boolean canRemoveTracks;
        private Boolean canReorderTracks;
        private Boolean canEditPlaylist;
        private Boolean canInviteOthers;
    }
    
    /**
     * Response DTO for collaboration statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CollaborationStats {
        
        private Long playlistId;
        private Integer totalCollaborators;
        private Integer totalContributions;
        private Map<String, Long> contributionsByUser;
        private String mostActiveContributor;
        private LocalDateTime lastActivity;
        private Boolean isCollaborative;
    }
    
    // ===============================
    // SMART PLAYLIST DTOs
    // ===============================
    
    /**
     * Response DTO for smart playlist statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SmartPlaylistStats {
        
        private Long playlistId;
        private Integer totalTracks;
        private LocalDateTime lastRefresh;
        private Boolean criteriaSet;
        private Integer genreCount;
        private Long averageTrackLength;
        private String dateRange;
        private String energyLevel;
    }
}