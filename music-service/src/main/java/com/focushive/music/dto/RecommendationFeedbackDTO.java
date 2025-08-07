package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 * DTO for capturing and processing user feedback on music recommendations.
 * Enables the system to learn from user preferences and improve future recommendations.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationFeedbackDTO {

    /**
     * Unique identifier for this feedback entry.
     */
    private UUID feedbackId;

    /**
     * ID of the user providing feedback.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * ID of the recommendation set being evaluated.
     */
    @NotNull(message = "Recommendation ID is required")
    private UUID recommendationId;

    /**
     * ID of the specific track being rated (if track-specific feedback).
     */
    private UUID recommendationTrackId;

    /**
     * Spotify track ID for the track being rated.
     */
    @NotBlank(message = "Track ID is required")
    private String trackId;

    /**
     * Session ID if feedback is session-specific.
     */
    private UUID sessionId;

    /**
     * Type of feedback being provided.
     */
    @NotNull(message = "Feedback type is required")
    private FeedbackType feedbackType;

    /**
     * Overall rating for the track/recommendation (1-5 stars).
     */
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer overallRating;

    /**
     * Whether the user liked the track.
     */
    private Boolean liked;

    /**
     * Specific interaction type.
     */
    private InteractionType interactionType;

    /**
     * Productivity impact assessment (1-10 scale).
     */
    @Min(value = 1, message = "Productivity impact must be between 1 and 10")
    @Max(value = 10, message = "Productivity impact must be between 1 and 10")
    private Integer productivityImpact;

    /**
     * Focus enhancement rating (1-10 scale).
     */
    @Min(value = 1, message = "Focus enhancement must be between 1 and 10")
    @Max(value = 10, message = "Focus enhancement must be between 1 and 10")
    private Integer focusEnhancement;

    /**
     * Mood appropriateness rating (1-10 scale).
     */
    @Min(value = 1, message = "Mood appropriateness must be between 1 and 10")
    @Max(value = 10, message = "Mood appropriateness must be between 1 and 10")
    private Integer moodAppropriateness;

    /**
     * Task suitability rating (1-10 scale).
     */
    @Min(value = 1, message = "Task suitability must be between 1 and 10")
    @Max(value = 10, message = "Task suitability must be between 1 and 10")
    private Integer taskSuitability;

    /**
     * Reason for negative feedback.
     */
    private NegativeFeedbackReason negativeReason;

    /**
     * Reason for skipping the track.
     */
    private SkipReason skipReason;

    /**
     * Custom feedback text from user.
     */
    @Size(max = 1000, message = "Feedback text cannot exceed 1000 characters")
    private String feedbackText;

    /**
     * Context when feedback was provided.
     */
    private FeedbackContext context;

    /**
     * Listening behavior data.
     */
    private ListeningBehavior listeningBehavior;

    /**
     * Timestamp when feedback was provided.
     */
    private LocalDateTime feedbackAt;

    /**
     * Additional metadata for feedback.
     */
    private Map<String, Object> additionalData;

    /**
     * Whether this feedback should influence future recommendations.
     */
    @Builder.Default
    private Boolean influenceFuture = true;

    /**
     * Confidence level of the user in their feedback (1-5).
     */
    @Min(value = 1, message = "Confidence level must be between 1 and 5")
    @Max(value = 5, message = "Confidence level must be between 1 and 5")
    private Integer confidenceLevel;

    /**
     * Tags associated with this feedback.
     */
    private List<String> tags;

    /**
     * Enumeration of feedback types.
     */
    public enum FeedbackType {
        EXPLICIT_RATING("User explicitly rated the track"),
        IMPLICIT_LIKE("User added to playlist or saved track"),
        IMPLICIT_DISLIKE("User skipped or removed track"),
        BEHAVIORAL("Derived from listening behavior"),
        CONTEXTUAL("Based on usage context and session data"),
        COMPARATIVE("Feedback relative to other recommendations");

        private final String description;

        FeedbackType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Types of user interactions with tracks.
     */
    public enum InteractionType {
        PLAYED_COMPLETE("Played track to completion"),
        PLAYED_PARTIAL("Played part of the track"),
        SKIPPED_IMMEDIATELY("Skipped within first 30 seconds"),
        SKIPPED_MIDDLE("Skipped during middle portion"),
        REPEATED("Replayed the track"),
        ADDED_TO_PLAYLIST("Added to user playlist"),
        SHARED("Shared with others"),
        SAVED("Saved to library"),
        REMOVED("Removed from playlist"),
        BLOCKED("Blocked/never play again");

        private final String description;

        InteractionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Reasons for negative feedback.
     */
    public enum NegativeFeedbackReason {
        TOO_ENERGETIC("Music was too energetic for the context"),
        TOO_CALM("Music was too calm/boring"),
        WRONG_GENRE("Not the preferred genre"),
        TOO_DISTRACTING("Music was distracting from work"),
        POOR_QUALITY("Poor audio quality"),
        INAPPROPRIATE_LYRICS("Lyrics were inappropriate for context"),
        TOO_FAMILIAR("Already heard this track too often"),
        TOO_UNFAMILIAR("Track was too unfamiliar/jarring"),
        WRONG_MOOD("Didn't match current mood"),
        WRONG_TEMPO("Tempo didn't suit the task"),
        TECHNICAL_ISSUES("Technical playback issues"),
        PERSONAL_DISLIKE("Simply don't like this track/artist");

        private final String description;

        NegativeFeedbackReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Specific reasons for skipping tracks.
     */
    public enum SkipReason {
        NOT_IN_MOOD("Not in the mood for this type of music"),
        HEARD_RECENTLY("Heard this track recently"),
        TOO_LONG("Track is too long for current session"),
        WRONG_ENERGY("Energy level doesn't match needs"),
        PREFER_INSTRUMENTAL("Prefer instrumental for current task"),
        VOLUME_ISSUES("Volume/dynamics not suitable"),
        TRANSITIONAL("Skipped for better flow between tracks"),
        EXTERNAL_INTERRUPTION("Skipped due to external interruption"),
        TASK_CHANGE("Changed tasks, need different music"),
        ACCIDENTAL("Accidentally skipped");

        private final String description;

        SkipReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Context information when feedback was provided.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeedbackContext {
        
        /**
         * Task type during feedback.
         */
        private RecommendationRequestDTO.TaskType currentTaskType;

        /**
         * Mood during feedback.
         */
        private RecommendationRequestDTO.MoodType currentMood;

        /**
         * Time of day when feedback was given.
         */
        private LocalDateTime contextTime;

        /**
         * Duration of session when feedback was provided.
         */
        private Integer sessionDurationMinutes;

        /**
         * Other tracks played in the same session.
         */
        private List<String> sessionTracks;

        /**
         * Environment where feedback was given.
         */
        private String environment;

        /**
         * Device used for playback.
         */
        private String playbackDevice;

        /**
         * Volume level during playback (0-100).
         */
        @Min(value = 0)
        @Max(value = 100)
        private Integer volumeLevel;

        /**
         * Whether user was alone or with others.
         */
        private Boolean soloListening;

        /**
         * Number of people in collaborative session.
         */
        private Integer collaboratorCount;
    }

    /**
     * Listening behavior data for implicit feedback.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ListeningBehavior {
        
        /**
         * Total time listened to the track in seconds.
         */
        @Min(value = 0)
        private Integer listenDurationSeconds;

        /**
         * Percentage of track completed (0.0-1.0).
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double completionPercentage;

        /**
         * Number of times track was repeated.
         */
        @Min(value = 0)
        private Integer repeatCount;

        /**
         * Time to skip in seconds (if skipped).
         */
        @Min(value = 0)
        private Integer timeToSkipSeconds;

        /**
         * Whether user adjusted volume during playback.
         */
        private Boolean volumeAdjusted;

        /**
         * Volume changes during playback.
         */
        private List<VolumeChange> volumeChanges;

        /**
         * Whether user paused during playback.
         */
        private Boolean pausedDuringPlay;

        /**
         * Number of pauses during playback.
         */
        @Min(value = 0)
        private Integer pauseCount;

        /**
         * Total pause duration in seconds.
         */
        @Min(value = 0)
        private Integer totalPauseDurationSeconds;

        /**
         * Whether user sought (scrubbed) through the track.
         */
        private Boolean userSeeked;

        /**
         * Seek events during playback.
         */
        private List<SeekEvent> seekEvents;
    }

    /**
     * Volume change event.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VolumeChange {
        
        /**
         * Time in track when volume was changed (seconds).
         */
        @Min(value = 0)
        private Integer timeInTrackSeconds;

        /**
         * Volume before change (0-100).
         */
        @Min(value = 0)
        @Max(value = 100)
        private Integer fromVolume;

        /**
         * Volume after change (0-100).
         */
        @Min(value = 0)
        @Max(value = 100)
        private Integer toVolume;
    }

    /**
     * Seek event during playback.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SeekEvent {
        
        /**
         * Time in track where seek started (seconds).
         */
        @Min(value = 0)
        private Integer fromTimeSeconds;

        /**
         * Time in track where seek ended (seconds).
         */
        @Min(value = 0)
        private Integer toTimeSeconds;

        /**
         * Direction of seek.
         */
        private SeekDirection direction;
    }

    /**
     * Direction of seek operation.
     */
    public enum SeekDirection {
        FORWARD, BACKWARD
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
     * Determines if this feedback indicates a positive user experience.
     */
    public Boolean isPositiveFeedback() {
        // Explicit positive indicators
        if (liked != null && liked) return true;
        if (overallRating != null && overallRating >= 4) return true;
        if (interactionType == InteractionType.ADDED_TO_PLAYLIST || 
            interactionType == InteractionType.SAVED ||
            interactionType == InteractionType.SHARED) return true;

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
        if (interactionType == InteractionType.SKIPPED_IMMEDIATELY ||
            interactionType == InteractionType.BLOCKED ||
            interactionType == InteractionType.REMOVED) return false;

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
     * Gets feedback influence weight for future recommendations (0.0-1.0).
     */
    public Double getFeedbackInfluenceWeight() {
        if (!influenceFuture) {
            return 0.0;
        }

        double weight = 0.5; // Base weight

        // Explicit feedback has higher weight
        if (feedbackType == FeedbackType.EXPLICIT_RATING) {
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
}