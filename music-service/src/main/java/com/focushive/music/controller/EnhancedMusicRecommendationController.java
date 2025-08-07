package com.focushive.music.controller;

import com.focushive.music.dto.*;
import com.focushive.music.model.RecommendationHistory;
import com.focushive.music.service.EnhancedMusicRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced REST controller for music recommendation endpoints.
 * 
 * Provides comprehensive APIs for:
 * - Session-based recommendations
 * - Task-specific recommendations  
 * - Mood-based recommendations
 * - Feedback collection and analysis
 * - Recommendation history and analytics
 * 
 * @author FocusHive Development Team
 * @version 2.1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/music/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Music Recommendations", description = "Enhanced APIs for music recommendations and personalization")
@SecurityRequirement(name = "Bearer Authentication")
public class EnhancedMusicRecommendationController {

    private final EnhancedMusicRecommendationService recommendationService;

    /**
     * Generates comprehensive music recommendations for a focus session.
     */
    @PostMapping("/sessions")
    @Operation(summary = "Generate session recommendations", 
               description = "Generate sophisticated music recommendations based on task type, mood, context, and user preferences")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationResponseDTO> generateSessionRecommendations(
            @Parameter(description = "Comprehensive recommendation request", required = true)
            @Valid @RequestBody RecommendationRequestDTO request,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Generating session recommendations for user: {}, task: {}, mood: {}", 
            userId, request.getTaskType(), request.getMood());

        // Set user context in request
        request.setSessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID());

        RecommendationResponseDTO response = recommendationService.generateSessionRecommendations(request);
        
        log.info("Generated {} recommendations for user: {} in {}ms", 
            response.getRecommendations().size(), userId, 
            response.getMetadata().getGenerationTimeMs());

        return ResponseEntity.ok(response);
    }

    /**
     * Generates task-specific recommendations.
     */
    @PostMapping("/tasks/{taskType}")
    @Operation(summary = "Get task-based recommendations", 
               description = "Get recommendations optimized for specific task types (deep-work, creative, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task recommendations generated"),
        @ApiResponse(responseCode = "400", description = "Invalid task type"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationResponseDTO> getTaskRecommendations(
            @Parameter(description = "Task type", required = true)
            @PathVariable RecommendationRequestDTO.TaskType taskType,
            
            @Parameter(description = "Additional preferences")
            @RequestBody(required = false) Map<String, Object> preferences,
            
            @Parameter(description = "Number of recommendations")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Generating task recommendations for user: {}, taskType: {}", userId, taskType);

        RecommendationResponseDTO response = recommendationService.generateTaskRecommendations(
            userId, taskType, preferences != null ? preferences : Map.of());

        return ResponseEntity.ok(response);
    }

    /**
     * Generates mood-based recommendations.
     */
    @PostMapping("/moods/{mood}")
    @Operation(summary = "Get mood-based recommendations", 
               description = "Get recommendations that match your current emotional state")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mood recommendations generated"),
        @ApiResponse(responseCode = "400", description = "Invalid mood type"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationResponseDTO> getMoodRecommendations(
            @Parameter(description = "Current mood", required = true)
            @PathVariable RecommendationRequestDTO.MoodType mood,
            
            @Parameter(description = "Additional preferences")
            @RequestBody(required = false) Map<String, Object> preferences,
            
            @Parameter(description = "Number of recommendations")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Generating mood recommendations for user: {}, mood: {}", userId, mood);

        RecommendationResponseDTO response = recommendationService.generateMoodRecommendations(
            userId, mood, preferences != null ? preferences : Map.of());

        return ResponseEntity.ok(response);
    }

    /**
     * Records user feedback on recommendations.
     */
    @PostMapping("/{recommendationId}/feedback")
    @Operation(summary = "Provide recommendation feedback", 
               description = "Submit feedback on recommendations to improve future suggestions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Feedback recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid feedback data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Recommendation not found")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FeedbackResponseDTO> provideFeedback(
            @Parameter(description = "Recommendation ID", required = true)
            @PathVariable UUID recommendationId,
            
            @Parameter(description = "Feedback data", required = true)
            @Valid @RequestBody RecommendationFeedbackDTO feedback,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Recording feedback for user: {}, recommendation: {}, track: {}", 
            userId, recommendationId, feedback.getTrackId());

        recommendationService.recordRecommendationFeedback(
            userId, recommendationId, feedback.getTrackId(), feedback);

        FeedbackResponseDTO response = FeedbackResponseDTO.builder()
            .feedbackId(UUID.randomUUID())
            .userId(userId)
            .recommendationId(recommendationId)
            .trackId(feedback.getTrackId())
            .status("RECORDED")
            .message("Feedback recorded successfully")
            .recordedAt(LocalDateTime.now())
            .influencesFuture(feedback.getInfluenceFuture())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Provides feedback on multiple tracks at once.
     */
    @PostMapping("/{recommendationId}/feedback/batch")
    @Operation(summary = "Provide batch feedback", 
               description = "Submit feedback for multiple tracks from the same recommendation set")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Batch feedback recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid feedback data"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BatchFeedbackResponseDTO> provideBatchFeedback(
            @Parameter(description = "Recommendation ID", required = true)
            @PathVariable UUID recommendationId,
            
            @Parameter(description = "Batch feedback data", required = true)
            @Valid @RequestBody BatchFeedbackRequestDTO batchFeedback,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Recording batch feedback for user: {}, recommendation: {}, {} tracks", 
            userId, recommendationId, batchFeedback.getFeedbackEntries().size());

        List<FeedbackResponseDTO> responses = batchFeedback.getFeedbackEntries().stream()
            .map(feedback -> {
                try {
                    recommendationService.recordRecommendationFeedback(
                        userId, recommendationId, feedback.getTrackId(), feedback);
                    return FeedbackResponseDTO.builder()
                        .feedbackId(UUID.randomUUID())
                        .userId(userId)
                        .recommendationId(recommendationId)
                        .trackId(feedback.getTrackId())
                        .status("RECORDED")
                        .recordedAt(LocalDateTime.now())
                        .build();
                } catch (Exception e) {
                    log.error("Error recording feedback for track: {}", feedback.getTrackId(), e);
                    return FeedbackResponseDTO.builder()
                        .userId(userId)
                        .recommendationId(recommendationId)
                        .trackId(feedback.getTrackId())
                        .status("ERROR")
                        .message(e.getMessage())
                        .build();
                }
            })
            .toList();

        long successCount = responses.stream().filter(r -> "RECORDED".equals(r.getStatus())).count();
        
        BatchFeedbackResponseDTO batchResponse = BatchFeedbackResponseDTO.builder()
            .batchId(UUID.randomUUID())
            .recommendationId(recommendationId)
            .totalEntries(batchFeedback.getFeedbackEntries().size())
            .successfulEntries((int) successCount)
            .failedEntries(batchFeedback.getFeedbackEntries().size() - (int) successCount)
            .responses(responses)
            .processedAt(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(batchResponse);
    }

    /**
     * Gets recommendation history for the user.
     */
    @GetMapping("/history")
    @Operation(summary = "Get recommendation history", 
               description = "Retrieve your recommendation history with filtering and pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<RecommendationHistoryDTO>> getRecommendationHistory(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            
            @Parameter(description = "Filter by time period (7d, 30d, 90d, all)")
            @RequestParam(defaultValue = "30d") String period,
            
            @Parameter(description = "Filter by task type")
            @RequestParam(required = false) RecommendationRequestDTO.TaskType taskType,
            
            @Parameter(description = "Filter by mood")
            @RequestParam(required = false) RecommendationRequestDTO.MoodType mood,
            
            @Parameter(description = "Minimum rating filter")
            @RequestParam(required = false) @DecimalMin("1.0") @DecimalMax("5.0") Double minRating,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Getting recommendation history for user: {}, period: {}", userId, period);

        // This would be implemented in the service layer
        List<RecommendationHistory> history = recommendationService.getRecommendationHistory(userId, period);
        
        // Convert to DTOs and create page response
        Page<RecommendationHistoryDTO> historyPage = convertToHistoryDTOs(history, page, size);

        return ResponseEntity.ok(historyPage);
    }

    /**
     * Gets analytics insights for the user's recommendation patterns.
     */
    @GetMapping("/analytics")
    @Operation(summary = "Get recommendation analytics", 
               description = "Retrieve personalized analytics about your music recommendation patterns")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationAnalyticsDTO> getRecommendationAnalytics(
            @Parameter(description = "Analytics period")
            @RequestParam(defaultValue = "30d") String period,
            
            @Parameter(description = "Include detailed breakdown")
            @RequestParam(defaultValue = "false") Boolean detailed,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Getting recommendation analytics for user: {}, period: {}", userId, period);

        // This would be implemented to generate comprehensive analytics
        RecommendationAnalyticsDTO analytics = generateUserAnalytics(userId, period, detailed);

        return ResponseEntity.ok(analytics);
    }

    /**
     * Invalidates user's recommendation cache when preferences change.
     */
    @DeleteMapping("/cache")
    @Operation(summary = "Clear recommendation cache", 
               description = "Clear your cached recommendations to force fresh generation")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cache cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> clearRecommendationCache(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Clearing recommendation cache for user: {}", userId);

        recommendationService.invalidateUserCache(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Gets trending recommendations across the platform.
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending recommendations", 
               description = "Get currently trending music across the FocusHive community")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trending recommendations retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TrendingRecommendationsDTO> getTrendingRecommendations(
            @Parameter(description = "Time period for trends")
            @RequestParam(defaultValue = "24h") String period,
            
            @Parameter(description = "Filter by task type")
            @RequestParam(required = false) RecommendationRequestDTO.TaskType taskType,
            
            @Parameter(description = "Number of trending tracks")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer limit) {
        
        log.info("Getting trending recommendations for period: {}, taskType: {}", period, taskType);

        // This would be implemented to get platform-wide trends
        TrendingRecommendationsDTO trending = generateTrendingRecommendations(period, taskType, limit);

        return ResponseEntity.ok(trending);
    }

    /**
     * Gets recommendation statistics for the user.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get recommendation statistics", 
               description = "Get your personal recommendation statistics and performance metrics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserRecommendationStatsDTO> getRecommendationStats(
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("Getting recommendation stats for user: {}", userId);

        // This would be implemented to generate user statistics
        UserRecommendationStatsDTO stats = generateUserStats(userId);

        return ResponseEntity.ok(stats);
    }

    // === PRIVATE HELPER METHODS ===

    private Page<RecommendationHistoryDTO> convertToHistoryDTOs(
            List<RecommendationHistory> history, int page, int size) {
        // Implementation would convert entities to DTOs and create Page
        return Page.empty(); // Placeholder
    }

    private RecommendationAnalyticsDTO generateUserAnalytics(
            UUID userId, String period, Boolean detailed) {
        // Implementation would generate comprehensive analytics
        return RecommendationAnalyticsDTO.builder()
            .userId(userId)
            .period(period)
            .totalRecommendations(0L)
            .build(); // Placeholder
    }

    private TrendingRecommendationsDTO generateTrendingRecommendations(
            String period, RecommendationRequestDTO.TaskType taskType, Integer limit) {
        // Implementation would generate trending recommendations
        return TrendingRecommendationsDTO.builder()
            .period(period)
            .generatedAt(LocalDateTime.now())
            .trendingTracks(List.of())
            .build(); // Placeholder
    }

    private UserRecommendationStatsDTO generateUserStats(UUID userId) {
        // Implementation would generate user statistics
        return UserRecommendationStatsDTO.builder()
            .userId(userId)
            .totalRecommendationsReceived(0L)
            .averageRating(0.0)
            .build(); // Placeholder
    }

    // === DTO CLASSES FOR RESPONSES ===

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FeedbackResponseDTO {
        private UUID feedbackId;
        private UUID userId;
        private UUID recommendationId;
        private String trackId;
        private String status;
        private String message;
        private LocalDateTime recordedAt;
        private Boolean influencesFuture;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchFeedbackRequestDTO {
        @Valid
        @Size(min = 1, max = 50, message = "Batch size must be between 1 and 50")
        private List<RecommendationFeedbackDTO> feedbackEntries;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchFeedbackResponseDTO {
        private UUID batchId;
        private UUID recommendationId;
        private Integer totalEntries;
        private Integer successfulEntries;
        private Integer failedEntries;
        private List<FeedbackResponseDTO> responses;
        private LocalDateTime processedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationHistoryDTO {
        private UUID recommendationId;
        private LocalDateTime createdAt;
        private String taskType;
        private String mood;
        private Integer totalTracks;
        private Double averageRating;
        private Double acceptanceRate;
        private Double productivityScore;
        private Boolean aboveAverage;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationAnalyticsDTO {
        private UUID userId;
        private String period;
        private Long totalRecommendations;
        private Double averageRating;
        private Double averageAcceptanceRate;
        private Map<String, Integer> topGenres;
        private Map<String, Double> taskTypePerformance;
        private Map<String, Double> moodTypePerformance;
        private List<String> topArtists;
        private RecommendationTrendsDTO trends;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RecommendationTrendsDTO {
        private Map<String, Double> ratingTrend;
        private Map<String, Double> diversityTrend;
        private Map<String, Integer> volumeTrend;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TrendingRecommendationsDTO {
        private String period;
        private LocalDateTime generatedAt;
        private List<TrendingTrackDTO> trendingTracks;
        private Map<String, Integer> trendingGenres;
        private Map<String, Integer> trendingArtists;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TrendingTrackDTO {
        private String trackId;
        private String name;
        private String artist;
        private Integer recommendationCount;
        private Double averageRating;
        private String trendReason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserRecommendationStatsDTO {
        private UUID userId;
        private Long totalRecommendationsReceived;
        private Long totalFeedbackGiven;
        private Double averageRating;
        private Double averageAcceptanceRate;
        private Integer favoriteGenreCount;
        private String mostProductiveTaskType;
        private String mostProductiveMood;
        private Double overallSatisfactionScore;
        private Map<String, Object> personalizedInsights;
    }
}