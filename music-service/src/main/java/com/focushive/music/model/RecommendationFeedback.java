package com.focushive.music.model;

import com.focushive.music.dto.RecommendationFeedbackDTO;
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
 * Entity for storing user feedback on music recommendations.
 * Captures explicit and implicit feedback to improve future recommendations.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "recommendation_feedback", schema = "music", indexes = {
    @Index(name = "idx_rec_feedback_user_id", columnList = "userId"),
    @Index(name = "idx_rec_feedback_track_id", columnList = "trackId"),
    @Index(name = "idx_rec_feedback_recommendation_id", columnList = "recommendationId"),
    @Index(name = "idx_rec_feedback_created_at", columnList = "feedbackAt"),
    @Index(name = "idx_rec_feedback_rating", columnList = "overallRating"),
    @Index(name = "idx_rec_feedback_type", columnList = "feedbackType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID feedbackId;

    /**
     * User who provided the feedback.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * ID of the recommendation set.
     */
    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    /**
     * ID of the specific track recommendation.
     */
    @Column(name = "recommendation_track_id")
    private UUID recommendationTrackId;

    /**
     * Spotify track ID.
     */
    @Column(name = "track_id", nullable = false, length = 100)
    private String trackId;

    /**
     * Session ID if feedback is session-specific.
     */
    @Column(name = "session_id")
    private UUID sessionId;

    /**
     * Type of feedback provided.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private RecommendationFeedbackDTO.FeedbackType feedbackType;

    /**
     * Overall rating (1-5 stars).
     */
    @Column(name = "overall_rating")
    private Integer overallRating;

    /**
     * Simple like/dislike indicator.
     */
    @Column(name = "liked")
    private Boolean liked;

    /**
     * Type of user interaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type")
    private RecommendationFeedbackDTO.InteractionType interactionType;

    /**
     * Productivity impact rating (1-10).
     */
    @Column(name = "productivity_impact")
    private Integer productivityImpact;

    /**
     * Focus enhancement rating (1-10).
     */
    @Column(name = "focus_enhancement")
    private Integer focusEnhancement;

    /**
     * Mood appropriateness rating (1-10).
     */
    @Column(name = "mood_appropriateness")
    private Integer moodAppropriateness;

    /**
     * Task suitability rating (1-10).
     */
    @Column(name = "task_suitability")
    private Integer taskSuitability;

    /**
     * Reason for negative feedback.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "negative_reason")
    private RecommendationFeedbackDTO.NegativeFeedbackReason negativeReason;

    /**
     * Reason for skipping.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason")
    private RecommendationFeedbackDTO.SkipReason skipReason;

    /**
     * Free-form feedback text.
     */
    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    /**
     * Context when feedback was provided.
     */
    @Embedded
    private FeedbackContext context;

    /**
     * Listening behavior data.
     */
    @Embedded
    private ListeningBehavior listeningBehavior;

    /**
     * When feedback was provided.
     */
    @Column(name = "feedback_at", nullable = false)
    private LocalDateTime feedbackAt;

    /**
     * Whether this feedback should influence future recommendations.
     */
    @Column(name = "influence_future")
    @Builder.Default
    private Boolean influenceFuture = true;

    /**
     * User's confidence in their feedback (1-5).
     */
    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    /**
     * Tags associated with this feedback.
     */
    @ElementCollection
    @CollectionTable(
        name = "feedback_tags", 
        schema = "music",
        joinColumns = @JoinColumn(name = "feedback_id")
    )
    @Column(name = "tag", length = 50)
    private List<String> tags;

    /**
     * Additional metadata as key-value pairs.
     */
    @ElementCollection
    @CollectionTable(
        name = "feedback_metadata", 
        schema = "music",
        joinColumns = @JoinColumn(name = "feedback_id")
    )
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 500)
    private Map<String, String> additionalData;

    /**
     * Timestamp when record was created.
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
     * Embedded class for feedback context.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackContext {
        
        @Column(name = "context_task_type", length = 50)
        private String currentTaskType;

        @Column(name = "context_mood", length = 50)
        private String currentMood;

        @Column(name = "context_time")
        private LocalDateTime contextTime;

        @Column(name = "context_session_duration")
        private Integer sessionDurationMinutes;

        @Column(name = "context_environment", length = 50)
        private String environment;

        @Column(name = "context_device", length = 50)
        private String playbackDevice;

        @Column(name = "context_volume")
        private Integer volumeLevel;

        @Column(name = "context_solo_listening")
        private Boolean soloListening;

        @Column(name = "context_collaborator_count")
        private Integer collaboratorCount;
    }

    /**
     * Embedded class for listening behavior.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListeningBehavior {
        
        @Column(name = "listen_duration_seconds")
        private Integer listenDurationSeconds;

        @Column(name = "completion_percentage")
        private Double completionPercentage;

        @Column(name = "repeat_count")
        private Integer repeatCount;

        @Column(name = "time_to_skip_seconds")
        private Integer timeToSkipSeconds;

        @Column(name = "volume_adjusted")
        private Boolean volumeAdjusted;

        @Column(name = "paused_during_play")
        private Boolean pausedDuringPlay;

        @Column(name = "pause_count")
        private Integer pauseCount;

        @Column(name = "total_pause_duration_seconds")
        private Integer totalPauseDurationSeconds;

        @Column(name = "user_seeked")
        private Boolean userSeeked;

        @Column(name = "seek_count")
        private Integer seekCount;

        /**
         * Volume changes during playback (stored as JSON).
         */
        @ElementCollection
        @CollectionTable(
            name = "feedback_volume_changes", 
            schema = "music",
            joinColumns = @JoinColumn(name = "feedback_id")
        )
        private List<VolumeChange> volumeChanges;

        /**
         * Seek events during playback.
         */
        @ElementCollection
        @CollectionTable(
            name = "feedback_seek_events", 
            schema = "music",
            joinColumns = @JoinColumn(name = "feedback_id")
        )
        private List<SeekEvent> seekEvents;
    }

    /**
     * Embeddable class for volume change events.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumeChange {
        
        @Column(name = "time_in_track_seconds")
        private Integer timeInTrackSeconds;

        @Column(name = "from_volume")
        private Integer fromVolume;

        @Column(name = "to_volume")
        private Integer toVolume;
    }

    /**
     * Embeddable class for seek events.
     */
    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeekEvent {
        
        @Column(name = "from_time_seconds")
        private Integer fromTimeSeconds;

        @Column(name = "to_time_seconds")
        private Integer toTimeSeconds;

        @Column(name = "seek_direction")
        @Enumerated(EnumType.STRING)
        private RecommendationFeedbackDTO.SeekDirection direction;
    }

    /**
     * Calculates implicit satisfaction score based on behavior.
     */
    public Double calculateImplicitSatisfactionScore() {
        if (listeningBehavior == null) {
            return null;
        }

        double score = 0.0;
        double maxScore = 10.0;

        // Completion percentage contributes heavily
        if (listeningBehavior.getCompletionPercentage() != null) {
            score += listeningBehavior.getCompletionPercentage() * 4.0;
        }

        // Repeat count indicates enjoyment
        if (listeningBehavior.getRepeatCount() != null) {
            score += Math.min(listeningBehavior.getRepeatCount() * 2.0, 3.0);
        }

        // Quick skips are negative indicators
        if (listeningBehavior.getTimeToSkipSeconds() != null) {
            if (listeningBehavior.getTimeToSkipSeconds() < 30) {
                score -= 3.0; // Heavily penalize immediate skips
            } else if (listeningBehavior.getTimeToSkipSeconds() < 60) {
                score -= 1.0;
            }
        }

        // Excessive pausing might indicate disengagement
        if (listeningBehavior.getPauseCount() != null && listeningBehavior.getPauseCount() > 2) {
            score -= 1.0;
        }

        // Volume increases might indicate enjoyment
        if (listeningBehavior.getVolumeAdjusted() != null && 
            listeningBehavior.getVolumeChanges() != null) {
            
            boolean volumeIncreased = listeningBehavior.getVolumeChanges().stream()
                .anyMatch(change -> change.getToVolume() > change.getFromVolume());
            
            if (volumeIncreased) {
                score += 1.0;
            }
        }

        return Math.max(0.0, Math.min(maxScore, score)) / maxScore;
    }

    /**
     * Determines if this feedback indicates positive user experience.
     */
    public Boolean isPositiveFeedback() {
        // Explicit positive indicators
        if (liked != null && liked) return true;
        if (overallRating != null && overallRating >= 4) return true;
        if (interactionType == RecommendationFeedbackDTO.InteractionType.ADDED_TO_PLAYLIST || 
            interactionType == RecommendationFeedbackDTO.InteractionType.SAVED ||
            interactionType == RecommendationFeedbackDTO.InteractionType.SHARED) return true;

        // Implicit positive indicators
        if (listeningBehavior != null) {
            if (listeningBehavior.getCompletionPercentage() != null && 
                listeningBehavior.getCompletionPercentage() > 0.8) return true;
            if (listeningBehavior.getRepeatCount() != null && 
                listeningBehavior.getRepeatCount() > 0) return true;
        }

        // Explicit negative indicators
        if (liked != null && !liked) return false;
        if (overallRating != null && overallRating <= 2) return false;
        if (interactionType == RecommendationFeedbackDTO.InteractionType.SKIPPED_IMMEDIATELY ||
            interactionType == RecommendationFeedbackDTO.InteractionType.BLOCKED ||
            interactionType == RecommendationFeedbackDTO.InteractionType.REMOVED) return false;

        // Implicit negative indicators
        if (listeningBehavior != null) {
            if (listeningBehavior.getTimeToSkipSeconds() != null && 
                listeningBehavior.getTimeToSkipSeconds() < 30) return false;
            if (listeningBehavior.getCompletionPercentage() != null && 
                listeningBehavior.getCompletionPercentage() < 0.2) return false;
        }

        return null; // Neutral/unknown
    }

    /**
     * Gets feedback influence weight for machine learning (0.0-1.0).
     */
    public Double getFeedbackInfluenceWeight() {
        if (!influenceFuture) {
            return 0.0;
        }

        double weight = 0.5; // Base weight

        // Explicit feedback has higher weight
        if (feedbackType == RecommendationFeedbackDTO.FeedbackType.EXPLICIT_RATING) {
            weight += 0.3;
        }

        // Confidence level influences weight
        if (confidenceLevel != null) {
            weight += (confidenceLevel - 3) * 0.1; // -0.2 to +0.2
        }

        // Recent feedback has higher weight
        if (feedbackAt != null) {
            long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(feedbackAt, LocalDateTime.now());
            if (daysAgo < 7) {
                weight += 0.1;
            } else if (daysAgo > 30) {
                weight -= 0.1;
            }
        }

        return Math.max(0.0, Math.min(1.0, weight));
    }

    /**
     * Checks if this feedback represents a strong signal for preferences.
     */
    public boolean isStrongPreferenceSignal() {
        // Strong positive signals
        if (overallRating != null && overallRating >= 5) return true;
        if (liked != null && liked && 
            (interactionType == RecommendationFeedbackDTO.InteractionType.ADDED_TO_PLAYLIST ||
             interactionType == RecommendationFeedbackDTO.InteractionType.SHARED)) return true;

        // Strong negative signals  
        if (overallRating != null && overallRating == 1) return true;
        if (interactionType == RecommendationFeedbackDTO.InteractionType.BLOCKED) return true;
        if (negativeReason == RecommendationFeedbackDTO.NegativeFeedbackReason.PERSONAL_DISLIKE) return true;

        // Behavioral strong signals
        if (listeningBehavior != null) {
            if (listeningBehavior.getRepeatCount() != null && listeningBehavior.getRepeatCount() >= 3) return true;
            if (listeningBehavior.getTimeToSkipSeconds() != null && 
                listeningBehavior.getTimeToSkipSeconds() < 15) return true;
        }

        return false;
    }

    /**
     * Gets the learning value of this feedback for the recommendation system.
     */
    public Double getLearningValue() {
        double value = 0.5; // Base learning value

        // Explicit feedback is more valuable
        if (feedbackType == RecommendationFeedbackDTO.FeedbackType.EXPLICIT_RATING) {
            value += 0.3;
        }

        // Detailed feedback is more valuable
        if (feedbackText != null && !feedbackText.trim().isEmpty()) {
            value += 0.2;
        }

        // Context-rich feedback is more valuable
        if (context != null && context.getCurrentTaskType() != null) {
            value += 0.1;
        }

        // Behavioral data adds value
        if (listeningBehavior != null) {
            value += 0.2;
        }

        // Strong signals are more valuable
        if (isStrongPreferenceSignal()) {
            value += 0.2;
        }

        return Math.min(1.0, value);
    }
}