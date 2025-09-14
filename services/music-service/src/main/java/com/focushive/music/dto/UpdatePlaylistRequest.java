package com.focushive.music.dto;

import com.focushive.music.entity.Playlist;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for updating playlist metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlaylistRequest {
    private String userId;
    private String name;
    private String description;
    private Playlist.FocusMode focusMode;
    private Boolean isPublic;
}