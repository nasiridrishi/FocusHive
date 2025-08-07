package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for music recommendations.
 * 
 * Contains recommendation request parameters and response data
 * for personalized music suggestions based on user preferences,
 * context, and collaborative filtering.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
public class RecommendationDTO {

    /**
     * Request DTO for getting music recommendations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Request {
        
        /**
         * Preferred genres for recommendations.
         */
        private List<@NotBlank String> genres;
        
        /**
         * Preferred mood for recommendations.
         */
        @Size(max = 50, message = "Mood must be 50 characters or less")
        private String mood;
        
        /**
         * Preferred energy level (1-10 scale).
         */
        @Min(value = 1, message = "Energy level must be between 1 and 10")
        @Max(value = 10, message = "Energy level must be between 1 and 10")
        private Integer energyLevel;
        
        /**
         * Seed artists for recommendations.
         */
        @Size(max = 5, message = "Maximum 5 seed artists allowed")
        private List<@NotBlank String> seedArtists;
        
        /**
         * Seed tracks for recommendations.
         */
        @Size(max = 5, message = "Maximum 5 seed tracks allowed")
        private List<@NotBlank String> seedTracks;
        
        /**
         * Maximum number of recommendations to return.
         */
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit cannot exceed 50")
        @Builder.Default
        private Integer limit = 20;
        
        /**
         * Context for recommendations (e.g., "focus", "break", "workout").
         */
        @Size(max = 100, message = "Context must be 100 characters or less")
        private String context;
        
        /**
         * Hive ID for collaborative recommendations.
         */
        private String hiveId;
        
        /**
         * Whether to include explicit content.
         */
        @Builder.Default
        private Boolean includeExplicit = true;
        
        /**
         * Target duration in minutes for the recommendation set.
         */
        @Min(value = 5, message = "Target duration must be at least 5 minutes")
        @Max(value = 480, message = "Target duration cannot exceed 480 minutes")
        private Integer targetDurationMinutes;
        
        /**
         * Minimum popularity score (0-100).
         */
        @Min(value = 0, message = "Min popularity must be between 0 and 100")
        @Max(value = 100, message = "Min popularity must be between 0 and 100")
        private Integer minPopularity;
        
        /**
         * Maximum popularity score (0-100).
         */
        @Min(value = 0, message = "Max popularity must be between 0 and 100")
        @Max(value = 100, message = "Max popularity must be between 0 and 100")
        private Integer maxPopularity;
    }
    
    /**
     * Response DTO containing music recommendations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        
        /**
         * List of recommended tracks.
         */
        @NotNull
        @Valid
        private List<TrackRecommendation> tracks;
        
        /**
         * The recommendation context/reason.
         */
        private String reason;
        
        /**
         * Total duration of recommended tracks in milliseconds.
         */
        private Long totalDurationMs;
        
        /**
         * Whether the recommendations are cached.
         */
        private Boolean cached;
        
        /**
         * Timestamp when recommendations were generated.
         */
        private String generatedAt;
        
        /**
         * Confidence score for the recommendation set (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
        private Double confidence;
        
        /**
         * Seeds used for generating recommendations.
         */
        private RecommendationSeeds seeds;
    }
    
    /**
     * DTO representing a single track recommendation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackRecommendation {
        
        /**
         * Spotify track ID.
         */
        @NotBlank(message = "Spotify track ID is required")
        private String spotifyTrackId;
        
        /**
         * Track name.
         */
        @NotBlank(message = "Track name is required")
        @Size(max = 500, message = "Track name cannot exceed 500 characters")
        private String name;
        
        /**
         * Primary artist name.
         */
        @NotBlank(message = "Artist name is required")
        @Size(max = 500, message = "Artist name cannot exceed 500 characters")
        private String artist;
        
        /**
         * Album name.
         */
        @Size(max = 500, message = "Album name cannot exceed 500 characters")
        private String album;
        
        /**
         * Track duration in milliseconds.
         */
        @Min(value = 1000, message = "Duration must be at least 1000ms")
        private Integer durationMs;
        
        /**
         * Track popularity score (0-100).
         */
        @Min(value = 0, message = "Popularity must be between 0 and 100")
        @Max(value = 100, message = "Popularity must be between 0 and 100")
        private Integer popularity;
        
        /**
         * Track energy level (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Energy must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Energy must be between 0.0 and 1.0")
        private Double energy;
        
        /**
         * Track valence/mood (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Valence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Valence must be between 0.0 and 1.0")
        private Double valence;
        
        /**
         * Track danceability (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Danceability must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Danceability must be between 0.0 and 1.0")
        private Double danceability;
        
        /**
         * Track URL for a 30-second preview.
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
         * Whether the track contains explicit content.
         */
        private Boolean explicit;
        
        /**
         * Genres associated with the track.
         */
        private List<String> genres;
        
        /**
         * Recommendation score/relevance (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Score must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Score must be between 0.0 and 1.0")
        private Double score;
        
        /**
         * Reason why this track was recommended.
         */
        private String reason;
    }
    
    /**
     * DTO representing the seeds used for recommendations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecommendationSeeds {
        
        /**
         * Artist seeds used.
         */
        private List<String> artists;
        
        /**
         * Track seeds used.
         */
        private List<String> tracks;
        
        /**
         * Genre seeds used.
         */
        private List<String> genres;
    }
    
    /**
     * DTO for recommendation analytics and insights.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Analytics {
        
        /**
         * Total number of recommendations generated.
         */
        private Long totalRecommendations;
        
        /**
         * Number of recommendations accepted/added to playlists.
         */
        private Long acceptedRecommendations;
        
        /**
         * Acceptance rate (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Acceptance rate must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Acceptance rate must be between 0.0 and 1.0")
        private Double acceptanceRate;
        
        /**
         * Most recommended genres.
         */
        private List<String> topGenres;
        
        /**
         * Most recommended artists.
         */
        private List<String> topArtists;
        
        /**
         * Average energy level of recommendations.
         */
        @DecimalMin(value = "0.0", message = "Average energy must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Average energy must be between 0.0 and 1.0")
        private Double averageEnergy;
        
        /**
         * Average valence/mood of recommendations.
         */
        @DecimalMin(value = "0.0", message = "Average valence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Average valence must be between 0.0 and 1.0")
        private Double averageValence;
    }
}