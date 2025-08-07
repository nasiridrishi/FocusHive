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
import java.util.UUID;

/**
 * Comprehensive DTO for music recommendation responses containing 
 * recommended tracks, metadata, and analytics information.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationResponseDTO {

    /**
     * Unique identifier for this recommendation set.
     */
    @NotNull
    private UUID recommendationId;

    /**
     * User ID for whom recommendations were generated.
     */
    @NotNull
    private UUID userId;

    /**
     * Session ID if recommendations are session-specific.
     */
    private UUID sessionId;

    /**
     * Hive ID for collaborative recommendations.
     */
    private UUID hiveId;

    /**
     * List of recommended tracks.
     */
    @NotNull
    @Valid
    @Size(min = 1, max = 50, message = "Must contain between 1 and 50 recommendations")
    private List<TrackRecommendation> recommendations;

    /**
     * Context information for these recommendations.
     */
    @NotNull
    @Valid
    private RecommendationContext context;

    /**
     * Metadata about the recommendation generation process.
     */
    @Valid
    private RecommendationMetadata metadata;

    /**
     * Analytics information for tracking and improvement.
     */
    @Valid
    private RecommendationAnalytics analytics;

    /**
     * Seeds used for generating these recommendations.
     */
    @Valid
    private RecommendationSeeds seeds;

    /**
     * Individual track recommendation with detailed information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackRecommendation {

        /**
         * Unique identifier for this recommendation.
         */
        @NotNull
        private UUID recommendationTrackId;

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
        private String primaryArtist;

        /**
         * All artists involved in the track.
         */
        private List<String> allArtists;

        /**
         * Album name.
         */
        @Size(max = 500, message = "Album name cannot exceed 500 characters")
        private String album;

        /**
         * Album artwork URLs in different sizes.
         */
        private Map<String, String> albumArtwork; // e.g., {"small": "url1", "medium": "url2", "large": "url3"}

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
         * Comprehensive audio features.
         */
        @Valid
        private AudioFeatures audioFeatures;

        /**
         * Recommendation score/relevance (0.0-1.0).
         */
        @DecimalMin(value = "0.0", message = "Score must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Score must be between 0.0 and 1.0")
        private Double score;

        /**
         * Detailed reason why this track was recommended.
         */
        private String reason;

        /**
         * Reason category for analytics.
         */
        private ReasonCategory reasonCategory;

        /**
         * Track URL for a 30-second preview.
         */
        private String previewUrl;

        /**
         * External Spotify URL.
         */
        private String externalUrl;

        /**
         * Whether the track contains explicit content.
         */
        private Boolean explicit;

        /**
         * Release date of the track.
         */
        private String releaseDate;

        /**
         * Genres associated with the track/artist.
         */
        private List<String> genres;

        /**
         * Market availability information.
         */
        private List<String> availableMarkets;

        /**
         * Productivity correlation score if available.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double productivityScore;

        /**
         * User's historical interaction with this track.
         */
        private TrackHistory userHistory;

        /**
         * Position in the recommendation list (1-based).
         */
        @Min(value = 1)
        private Integer position;
    }

    /**
     * Comprehensive audio features for a track.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AudioFeatures {
        
        @DecimalMin(value = "0.0", message = "Energy must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Energy must be between 0.0 and 1.0")
        private Double energy;

        @DecimalMin(value = "0.0", message = "Valence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Valence must be between 0.0 and 1.0")
        private Double valence;

        @DecimalMin(value = "0.0", message = "Danceability must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Danceability must be between 0.0 and 1.0")
        private Double danceability;

        @DecimalMin(value = "0.0", message = "Instrumentalness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Instrumentalness must be between 0.0 and 1.0")
        private Double instrumentalness;

        @DecimalMin(value = "0.0", message = "Acousticness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Acousticness must be between 0.0 and 1.0")
        private Double acousticness;

        @DecimalMin(value = "0.0", message = "Liveness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Liveness must be between 0.0 and 1.0")
        private Double liveness;

        @DecimalMin(value = "0.0", message = "Speechiness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Speechiness must be between 0.0 and 1.0")
        private Double speechiness;

        @Min(value = 50, message = "Tempo must be at least 50 BPM")
        @Max(value = 250, message = "Tempo cannot exceed 250 BPM")
        private Integer tempo;

        @Min(value = -60, message = "Loudness cannot be less than -60 dB")
        @Max(value = 0, message = "Loudness cannot exceed 0 dB")
        private Integer loudness;

        @Min(value = 0, message = "Key must be between 0 and 11")
        @Max(value = 11, message = "Key must be between 0 and 11")
        private Integer key;

        @Min(value = 0, message = "Mode must be 0 (minor) or 1 (major)")
        @Max(value = 1, message = "Mode must be 0 (minor) or 1 (major)")
        private Integer mode;

        @Min(value = 3, message = "Time signature must be at least 3")
        @Max(value = 7, message = "Time signature cannot exceed 7")
        private Integer timeSignature;
    }

    /**
     * Context information for the recommendation set.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecommendationContext {
        
        /**
         * Type of task these recommendations are for.
         */
        private RecommendationRequestDTO.TaskType taskType;

        /**
         * Mood context for recommendations.
         */
        private RecommendationRequestDTO.MoodType mood;

        /**
         * Time of day when recommendations were requested.
         */
        private LocalDateTime requestedAt;

        /**
         * Expected session duration in minutes.
         */
        private Integer expectedDurationMinutes;

        /**
         * Whether this is for a collaborative session.
         */
        private Boolean collaborative;

        /**
         * Number of hive members if collaborative.
         */
        private Integer hiveSize;

        /**
         * Environmental context (office, home, public space).
         */
        private String environment;

        /**
         * Device type used for playback.
         */
        private String deviceType;

        /**
         * Previous session outcomes that influenced this recommendation.
         */
        private List<String> previousSessionInsights;
    }

    /**
     * Metadata about the recommendation generation process.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecommendationMetadata {
        
        /**
         * Algorithm version used for generation.
         */
        private String algorithmVersion;

        /**
         * Time taken to generate recommendations in milliseconds.
         */
        @Min(value = 0)
        private Long generationTimeMs;

        /**
         * Whether recommendations were served from cache.
         */
        private Boolean fromCache;

        /**
         * Cache key used if served from cache.
         */
        private String cacheKey;

        /**
         * Cache TTL in seconds.
         */
        private Long cacheTtlSeconds;

        /**
         * Data sources used for recommendations.
         */
        private List<String> dataSources;

        /**
         * Model confidence scores.
         */
        private Map<String, Double> modelConfidences;

        /**
         * A/B test variant if applicable.
         */
        private String abTestVariant;

        /**
         * Feature flags that influenced recommendations.
         */
        private List<String> activeFeatureFlags;

        /**
         * Total number of candidate tracks considered.
         */
        @Min(value = 0)
        private Integer candidateCount;
    }

    /**
     * Analytics information for tracking recommendation performance.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecommendationAnalytics {
        
        /**
         * Diversity score of the recommendation set.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double diversityScore;

        /**
         * Novelty score (how new/unexpected the recommendations are).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double noveltyScore;

        /**
         * Serendipity score (pleasant surprises).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double serendipityScore;

        /**
         * Overall confidence in the recommendation set.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double confidence;

        /**
         * Predicted user satisfaction score.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double predictedSatisfaction;

        /**
         * Distribution of genres in recommendations.
         */
        private Map<String, Integer> genreDistribution;

        /**
         * Distribution of decades in recommendations.
         */
        private Map<String, Integer> decadeDistribution;

        /**
         * Average audio feature values.
         */
        private AudioFeatures averageFeatures;

        /**
         * Recommendation strategy breakdown.
         */
        private Map<String, Double> strategyWeights;

        /**
         * Expected engagement metrics.
         */
        private Map<String, Double> predictedEngagement;
    }

    /**
     * Seeds used for generating recommendations.
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

        /**
         * User's listening history used as seeds.
         */
        private List<String> historySeeds;

        /**
         * Collaborative filtering seeds from similar users.
         */
        private List<String> collaborativeSeeds;
    }

    /**
     * User's historical interaction with a track.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrackHistory {
        
        /**
         * Number of times user has played this track.
         */
        @Min(value = 0)
        private Integer playCount;

        /**
         * Number of times user has skipped this track.
         */
        @Min(value = 0)
        private Integer skipCount;

        /**
         * Last time user played this track.
         */
        private LocalDateTime lastPlayedAt;

        /**
         * User's rating for this track (1-5).
         */
        @Min(value = 1)
        @Max(value = 5)
        private Integer userRating;

        /**
         * Whether user has added to any playlist.
         */
        private Boolean addedToPlaylist;

        /**
         * Average completion percentage when played.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double avgCompletionRate;

        /**
         * Contexts where user typically plays this track.
         */
        private List<String> playContexts;
    }

    /**
     * Enumeration of recommendation reason categories.
     */
    public enum ReasonCategory {
        CONTENT_BASED("Based on audio features and content analysis"),
        COLLABORATIVE("Based on similar users' preferences"),
        PRODUCTIVITY("Proven to enhance productivity in similar contexts"),
        MOOD_BASED("Matches your current emotional state"),
        TASK_OPTIMIZED("Optimized for your specific task type"),
        TEMPORAL("Considers time of day and context"),
        SOCIAL("Popular within your hive/community"),
        DISCOVERY("New music discovery based on your taste"),
        COMFORT_ZONE("Familiar music you typically enjoy"),
        VARIETY("Added for diversity and exploration");

        private final String description;

        ReasonCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Gets the total duration of all recommendations in milliseconds.
     */
    public long getTotalDurationMs() {
        return recommendations.stream()
            .filter(track -> track.getDurationMs() != null)
            .mapToLong(TrackRecommendation::getDurationMs)
            .sum();
    }

    /**
     * Gets the average score of all recommendations.
     */
    public Double getAverageScore() {
        return recommendations.stream()
            .filter(track -> track.getScore() != null)
            .mapToDouble(TrackRecommendation::getScore)
            .average()
            .orElse(0.0);
    }

    /**
     * Gets count of recommendations by reason category.
     */
    public Map<ReasonCategory, Long> getReasonCategoryDistribution() {
        return recommendations.stream()
            .filter(track -> track.getReasonCategory() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                TrackRecommendation::getReasonCategory,
                java.util.stream.Collectors.counting()
            ));
    }

    /**
     * Checks if the recommendation set meets minimum quality standards.
     */
    public boolean isQualityRecommendationSet() {
        if (recommendations == null || recommendations.isEmpty()) {
            return false;
        }

        // Check if all tracks have required fields
        boolean hasRequiredFields = recommendations.stream()
            .allMatch(track -> 
                track.getSpotifyTrackId() != null &&
                track.getName() != null &&
                track.getPrimaryArtist() != null &&
                track.getScore() != null
            );

        // Check score diversity (shouldn't be all the same score)
        long uniqueScores = recommendations.stream()
            .filter(track -> track.getScore() != null)
            .mapToLong(track -> Math.round(track.getScore() * 10))
            .distinct()
            .count();

        return hasRequiredFields && uniqueScores > 1;
    }
}