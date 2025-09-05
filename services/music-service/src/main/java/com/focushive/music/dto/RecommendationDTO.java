package com.focushive.music.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for music recommendations.
 */
@Data
@Builder
public class RecommendationDTO {
    
    private String spotifyTrackId;
    private String title;
    private String artist;
    private String album;
    private String albumArt;
    private Integer durationMs;
    private Double energy;
    private Double valence;
    private Double danceability;
    private Double tempo;
    private List<String> genres;
    private String previewUrl;
    private String reason;
    private Double confidenceScore;
    private Map<String, Object> metadata;
}