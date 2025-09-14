package com.focushive.music.dto;

import lombok.Data;

/**
 * Request DTO for creating a playlist.
 */
@Data
public class CreatePlaylistRequest {
    private String name;
    private String description;
    private String focusMode;
    private Boolean isPublic;
    private String userId;
}