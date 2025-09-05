package com.focushive.music.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Playlist.
 */
@Data
@Builder
public class PlaylistDTO {
    
    private UUID id;
    private String name;
    private String description;
    private String userId;
    private String type;
    private String focusMode;
    private Boolean isPublic;
    private String spotifyPlaylistId;
    private List<TrackDTO> tracks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class TrackDTO {
        private UUID id;
        private String spotifyTrackId;
        private String title;
        private String artist;
        private String album;
        private Integer durationMs;
        private Integer order;
        private String addedBy;
        private LocalDateTime createdAt;
    }
}