package com.focushive.music.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for adding a track to a playlist.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTrackRequest {
    private String userId;
    private String spotifyTrackId;
}