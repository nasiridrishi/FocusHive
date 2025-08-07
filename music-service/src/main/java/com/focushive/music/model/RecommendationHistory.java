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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing the history of music recommendations generated for users.
 * Tracks recommendation performance, user acceptance, and analytics data.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "recommendation_history", schema = "music", indexes = {
    @Index(name = "idx_rec_history_user_id", columnList = "userId"),
    @Index(name = "idx_rec_history_session_id", columnList = "sessionId"),
    @Index(name = "idx_rec_history_hive_id", columnList = "hiveId"),
    @Index(name = "idx_rec_history_created_at", columnList = "createdAt"),
    @Index(name = "idx_rec_history_task_type", columnList = "taskType"),
    @Index(name = "idx_rec_history_rating", columnList = "averageRating")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecommendationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID recommendationId;

    /**
     * User ID for whom recommendations were generated.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Session ID if recommendations were session-specific.
     */
    @Column(name = "session_id")
    private UUID sessionId;

    /**
     * Hive ID for collaborative recommendations.
     */
    @Column(name = "hive_id")
    private UUID hiveId;

    /**
     * Type of task these recommendations were for.
     */
    @Column(name = "task_type", length = 50)
    private String taskType;

    /**
     * Mood context for the recommendations.
     */
    @Column(name = "mood", length = 50)
    private String mood;

    /**
     * Algorithm version used for generation.
     */
    @Column(name = "algorithm_version", length = 20)
    private String algorithmVersion;

    /**
     * List of Spotify track IDs that were recommended.
     */
    @ElementCollection
    @CollectionTable(
        name = "recommendation_track_ids", 
        schema = "music",
        joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @Column(name = "track_id", length = 100)
    private List<String> trackIds;

    /**
     * Distribution of genres in the recommendation set.
     */
    @ElementCollection
    @CollectionTable(
        name = "recommendation_genre_distribution", 
        schema = "music",
        joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @MapKeyColumn(name = "genre")
    @Column(name = "count")
    private Map<String, Integer> genreDistribution;

    /**
     * Total number of tracks recommended.
     */
    @Column(name = "total_tracks")
    private Integer totalTracks;

    /**
     * Number of tracks that received positive feedback.
     */
    @Column(name = "accepted_tracks")
    private Integer acceptedTracks;

    /**
     * Number of tracks that received negative feedback.
     */
    @Column(name = "rejected_tracks")
    private Integer rejectedTracks;

    /**
     * Acceptance rate (accepted / total).
     */
    @Column(name = "acceptance_rate")
    private Double acceptanceRate;

    /**
     * Average user rating for the recommendations.
     */
    @Column(name = "average_rating")
    private Double averageRating;

    /**
     * Diversity score of the recommendation set.
     */
    @Column(name = "diversity_score")
    private Double diversityScore;

    /**
     * Novelty score (how new/unexpected recommendations were).
     */
    @Column(name = "novelty_score")
    private Double noveltyScore;

    /**
     * Serendipity score (pleasant surprises).
     */
    @Column(name = "serendipity_score")
    private Double serendipityScore;

    /**
     * Overall productivity score achieved during the session.
     */
    @Column(name = "productivity_score")
    private Double productivityScore;

    /**
     * Focus enhancement score reported by users.
     */
    @Column(name = "focus_score")
    private Double focusScore;

    /**
     * Time taken to generate recommendations (milliseconds).
     */
    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    /**
     * Whether recommendations were served from cache.
     */
    @Column(name = "served_from_cache")
    private Boolean servedFromCache;

    /**
     * Cache key used (if applicable).
     */
    @Column(name = "cache_key", length = 200)
    private String cacheKey;

    /**
     * Data sources used for recommendations.
     */
    @ElementCollection
    @CollectionTable(
        name = "recommendation_data_sources", 
        schema = "music",
        joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @Column(name = "data_source", length = 50)
    private List<String> dataSources;

    /**
     * Seeds used for generating recommendations.
     */
    @Embedded
    private RecommendationSeeds seeds;

    /**
     * Context information when recommendations were generated.
     */
    @Embedded
    private RecommendationContext context;

    /**
     * Performance metrics for this recommendation set.
     */
    @Embedded
    private RecommendationMetrics metrics;

    /**
     * Number of feedback entries received.
     */
    @Column(name = "feedback_count")
    private Integer feedbackCount;

    /**
     * Average completion rate of recommended tracks.
     */
    @Column(name = "avg_completion_rate")
    private Double avgCompletionRate;

    /**
     * Average skip time for skipped tracks (seconds).
     */
    @Column(name = "avg_skip_time_seconds")
    private Integer avgSkipTimeSeconds;

    /**
     * Whether this recommendation set performed above user's average.
     */
    @Column(name = "above_average_performance")
    private Boolean aboveAveragePerformance;

    /**
     * A/B test variant if applicable.
     */
    @Column(name = "ab_test_variant", length = 50)
    private String abTestVariant;

    /**
     * Additional metadata as JSON.
     */
    @ElementCollection
    @CollectionTable(
        name = "recommendation_metadata", 
        schema = "music",
        joinColumns = @JoinColumn(name = "recommendation_id")
    )
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 500)
    private Map<String, String> additionalMetadata;

    /**
     * Timestamp when recommendations were created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when record was last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Embedded class for recommendation seeds.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationSeeds {
        
        @ElementCollection
        @CollectionTable(
            name = "recommendation_seed_artists", 
            schema = "music",
            joinColumns = @JoinColumn(name = "recommendation_id")
        )
        @Column(name = "artist_id", length = 100)
        private List<String> seedArtists;

        @ElementCollection
        @CollectionTable(
            name = "recommendation_seed_tracks", 
            schema = "music",
            joinColumns = @JoinColumn(name = "recommendation_id")
        )
        @Column(name = "track_id", length = 100)
        private List<String> seedTracks;

        @ElementCollection
        @CollectionTable(
            name = "recommendation_seed_genres", 
            schema = "music",
            joinColumns = @JoinColumn(name = "recommendation_id")
        )
        @Column(name = "genre", length = 50)
        private List<String> seedGenres;
    }

    /**
     * Embedded class for recommendation context.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationContext {
        
        @Column(name = "context_time_of_day")
        private LocalDateTime timeOfDay;

        @Column(name = "context_expected_duration")
        private Integer expectedDurationMinutes;

        @Column(name = "context_environment", length = 50)
        private String environment;

        @Column(name = "context_device_type", length = 50)
        private String deviceType;

        @Column(name = "context_collaborative")
        private Boolean collaborative;

        @Column(name = "context_hive_size")
        private Integer hiveSize;

        @Column(name = "context_user_energy_level")
        private Integer userEnergyLevel;
    }

    /**
     * Embedded class for recommendation performance metrics.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationMetrics {
        
        @Column(name = "metrics_confidence_score")
        private Double confidenceScore;

        @Column(name = "metrics_predicted_satisfaction")
        private Double predictedSatisfaction;

        @Column(name = "metrics_actual_satisfaction")
        private Double actualSatisfaction;

        @Column(name = "metrics_engagement_score")
        private Double engagementScore;

        @Column(name = "metrics_retention_rate")
        private Double retentionRate;

        @Column(name = "metrics_surprise_factor")
        private Double surpriseFactor;

        @Column(name = "metrics_familiarity_score")
        private Double familiarityScore;
    }

    /**
     * Calculates and updates the acceptance rate based on feedback.
     */
    public void updateAcceptanceRate() {
        if (totalTracks != null && totalTracks > 0) {
            int positiveCount = acceptedTracks != null ? acceptedTracks : 0;
            this.acceptanceRate = (double) positiveCount / totalTracks;
        }
    }

    /**
     * Checks if this recommendation set performed well.
     */
    public boolean isSuccessfulRecommendation() {
        if (averageRating != null && averageRating >= 4.0) return true;
        if (acceptanceRate != null && acceptanceRate >= 0.7) return true;
        if (productivityScore != null && productivityScore >= 0.8) return true;
        return false;
    }

    /**
     * Gets overall performance score combining multiple metrics.
     */
    public Double getOverallPerformanceScore() {
        double score = 0.0;
        int components = 0;

        if (averageRating != null) {
            score += averageRating / 5.0; // Normalize to 0-1
            components++;
        }

        if (acceptanceRate != null) {
            score += acceptanceRate;
            components++;
        }

        if (productivityScore != null) {
            score += productivityScore;
            components++;
        }

        if (diversityScore != null) {
            score += diversityScore;
            components++;
        }

        return components > 0 ? score / components : null;
    }

    /**
     * Checks if this recommendation needs more feedback data.
     */
    public boolean needsMoreFeedback() {
        if (feedbackCount == null || totalTracks == null) return true;
        double feedbackRatio = (double) feedbackCount / totalTracks;
        return feedbackRatio < 0.5; // Need feedback on at least 50% of tracks
    }

    /**
     * Gets the recommendation efficiency (performance per generation time).
     */
    public Double getRecommendationEfficiency() {
        if (generationTimeMs == null || generationTimeMs == 0) return null;
        Double performance = getOverallPerformanceScore();
        if (performance == null) return null;
        
        // Performance per second of generation time
        return performance / (generationTimeMs / 1000.0);
    }

    /**
     * Determines if this recommendation set represents user preference drift.
     */
    public boolean indicatesPreferenceDrift() {
        return averageRating != null && averageRating < 2.5 && 
               acceptanceRate != null && acceptanceRate < 0.3;
    }
}