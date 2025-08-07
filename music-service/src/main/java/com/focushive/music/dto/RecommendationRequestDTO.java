package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for music recommendation requests with comprehensive context information.
 * Supports various recommendation scenarios including task-based, mood-based,
 * and time-contextual recommendations.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationRequestDTO {

    /**
     * Session ID for tracking recommendations within a focus session.
     */
    private UUID sessionId;

    /**
     * Hive ID for collaborative recommendations.
     */
    private UUID hiveId;

    /**
     * Type of task being performed.
     */
    private TaskType taskType;

    /**
     * Current mood or emotional state.
     */
    private MoodType mood;

    /**
     * Energy level preference (1-10 scale).
     */
    @Min(value = 1, message = "Energy level must be between 1 and 10")
    @Max(value = 10, message = "Energy level must be between 1 and 10")
    private Integer energyLevel;

    /**
     * Preferred genres for recommendations.
     */
    @Size(max = 10, message = "Maximum 10 genres allowed")
    private List<@NotBlank String> preferredGenres;

    /**
     * Genres to avoid in recommendations.
     */
    private List<@NotBlank String> excludedGenres;

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
     * Expected session duration in minutes.
     */
    @Min(value = 5, message = "Session duration must be at least 5 minutes")
    @Max(value = 480, message = "Session duration cannot exceed 8 hours")
    private Integer expectedDurationMinutes;

    /**
     * Time of day for context-aware recommendations.
     */
    private LocalTime timeOfDay;

    /**
     * Whether to include explicit content.
     */
    @Builder.Default
    private Boolean includeExplicit = false;

    /**
     * Maximum number of recommendations to return.
     */
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit cannot exceed 50")
    @Builder.Default
    private Integer limit = 20;

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

    /**
     * Additional context metadata.
     */
    private Map<String, Object> additionalContext;

    /**
     * Whether to prioritize productivity-proven tracks.
     */
    @Builder.Default
    private Boolean prioritizeProductivity = true;

    /**
     * Whether to include diversity in recommendations.
     */
    @Builder.Default
    private Boolean includeDiversity = true;

    /**
     * Minimum track duration in seconds.
     */
    @Min(value = 30, message = "Minimum track duration is 30 seconds")
    private Integer minDurationSeconds;

    /**
     * Maximum track duration in seconds.
     */
    @Max(value = 1800, message = "Maximum track duration is 30 minutes")
    private Integer maxDurationSeconds;

    /**
     * Audio feature preferences.
     */
    private AudioFeaturePreferences audioFeatures;

    /**
     * Task-specific configuration.
     */
    private TaskConfiguration taskConfiguration;

    /**
     * Enumeration of supported task types with their characteristics.
     */
    public enum TaskType {
        DEEP_WORK("Deep focused work requiring high concentration", 0.2, 0.4, 0.8),
        CREATIVE("Creative tasks requiring inspiration and flow", 0.5, 0.7, 0.3),
        ADMINISTRATIVE("Administrative tasks requiring steady focus", 0.6, 0.5, 0.2),
        CASUAL("Light tasks or background activity", 0.7, 0.8, 0.1),
        BRAINSTORMING("Collaborative ideation and discussion", 0.6, 0.8, 0.2),
        CODING("Programming and development work", 0.4, 0.5, 0.7),
        STUDYING("Learning and knowledge absorption", 0.3, 0.5, 0.6),
        RESEARCH("Information gathering and analysis", 0.4, 0.6, 0.5);

        private final String description;
        private final double targetEnergy;
        private final double targetValence;
        private final double targetInstrumentalness;

        TaskType(String description, double targetEnergy, double targetValence, double targetInstrumentalness) {
            this.description = description;
            this.targetEnergy = targetEnergy;
            this.targetValence = targetValence;
            this.targetInstrumentalness = targetInstrumentalness;
        }

        public String getDescription() { return description; }
        public double getTargetEnergy() { return targetEnergy; }
        public double getTargetValence() { return targetValence; }
        public double getTargetInstrumentalness() { return targetInstrumentalness; }
    }

    /**
     * Enumeration of mood types for emotion-aware recommendations.
     */
    public enum MoodType {
        ENERGETIC("High energy and motivation", 0.8, 0.8),
        FOCUSED("Concentrated and attentive", 0.4, 0.5),
        RELAXED("Calm and peaceful", 0.3, 0.7),
        STRESSED("Anxious or overwhelmed", 0.2, 0.6),
        CREATIVE("Inspired and imaginative", 0.6, 0.7),
        MELANCHOLIC("Reflective and introspective", 0.3, 0.3),
        HAPPY("Joyful and upbeat", 0.7, 0.9),
        NEUTRAL("Balanced emotional state", 0.5, 0.5),
        TIRED("Low energy but need to stay alert", 0.6, 0.4),
        ANXIOUS("Worried or restless", 0.3, 0.5);

        private final String description;
        private final double targetEnergy;
        private final double targetValence;

        MoodType(String description, double targetEnergy, double targetValence) {
            this.description = description;
            this.targetEnergy = targetEnergy;
            this.targetValence = targetValence;
        }

        public String getDescription() { return description; }
        public double getTargetEnergy() { return targetEnergy; }
        public double getTargetValence() { return targetValence; }
    }

    /**
     * Audio feature preferences for fine-tuning recommendations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AudioFeaturePreferences {
        
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetEnergy;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetValence;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetDanceability;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetInstrumentalness;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetAcousticness;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetLiveness;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double targetSpeechiness;

        @Min(value = 50)
        @Max(value = 200)
        private Integer targetTempo;

        @Min(value = -60)
        @Max(value = 0)
        private Integer targetLoudness;

        // Tolerance ranges for audio features
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "0.5")
        private Double energyTolerance;

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "0.5")
        private Double valenceTolerance;

        @Min(value = 10)
        @Max(value = 50)
        private Integer tempoTolerance;
    }

    /**
     * Task-specific configuration parameters.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TaskConfiguration {
        
        /**
         * Whether vocals are acceptable for this task.
         */
        @Builder.Default
        private Boolean allowVocals = true;

        /**
         * Maximum acceptable speechiness level.
         */
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        private Double maxSpeechiness;

        /**
         * Preferred tempo range for the task.
         */
        private TempoRange tempoRange;

        /**
         * Whether to prioritize familiar tracks.
         */
        @Builder.Default
        private Boolean prioritizeFamiliar = false;

        /**
         * Whether to include background ambient sounds.
         */
        @Builder.Default
        private Boolean includeAmbient = false;

        /**
         * Complexity level of music (simple to complex).
         */
        @Min(value = 1)
        @Max(value = 5)
        private Integer complexityLevel;

        /**
         * Whether the task requires synchronized music for team work.
         */
        @Builder.Default
        private Boolean requiresSynchronization = false;
    }

    /**
     * Tempo range specification.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempoRange {
        @Min(value = 50)
        private Integer minTempo;

        @Max(value = 200)
        private Integer maxTempo;
    }

    /**
     * Validates the recommendation request for consistency.
     */
    public boolean isValid() {
        // Basic validation logic
        if (minPopularity != null && maxPopularity != null) {
            if (minPopularity > maxPopularity) {
                return false;
            }
        }

        if (minDurationSeconds != null && maxDurationSeconds != null) {
            if (minDurationSeconds > maxDurationSeconds) {
                return false;
            }
        }

        // Audio features validation
        if (audioFeatures != null) {
            if (audioFeatures.getTargetTempo() != null && audioFeatures.getTempoTolerance() != null) {
                int minTempo = audioFeatures.getTargetTempo() - audioFeatures.getTempoTolerance();
                int maxTempo = audioFeatures.getTargetTempo() + audioFeatures.getTempoTolerance();
                if (minTempo < 50 || maxTempo > 200) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets the effective energy level based on task type and mood.
     */
    public Double getEffectiveEnergyLevel() {
        double baseEnergy = energyLevel != null ? energyLevel / 10.0 : 0.5;
        
        if (taskType != null) {
            baseEnergy = (baseEnergy + taskType.getTargetEnergy()) / 2.0;
        }
        
        if (mood != null) {
            baseEnergy = (baseEnergy + mood.getTargetEnergy()) / 2.0;
        }
        
        return Math.max(0.0, Math.min(1.0, baseEnergy));
    }

    /**
     * Gets the effective valence based on mood and preferences.
     */
    public Double getEffectiveValence() {
        double baseValence = 0.5; // Neutral default
        
        if (taskType != null) {
            baseValence = taskType.getTargetValence();
        }
        
        if (mood != null) {
            baseValence = (baseValence + mood.getTargetValence()) / 2.0;
        }
        
        return Math.max(0.0, Math.min(1.0, baseValence));
    }
}