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
 * REST controller for music recommendation endpoints.
 * 
 * Provides APIs for generating and managing music recommendations
 * based on user preferences, session context, and hive dynamics.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/music/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Music Recommendations", description = "APIs for music recommendations and personalization")
@SecurityRequirement(name = "Bearer Authentication")
public class MusicRecommendationController {

    private final EnhancedMusicRecommendationService musicRecommendationService;

    /**
     * Generates music recommendations for a user session.
     * 
     * @param sessionId The session ID
     * @param request Recommendation request
     * @param jwt JWT token containing user information
     * @return List of music recommendations
     */
    @PostMapping("/sessions/{sessionId}")
    @Operation(summary = "Generate session recommendations", 
               description = "Generate personalized music recommendations for a focus session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationResponse> generateSessionRecommendations(
            @Parameter(description = "Session ID", required = true)
            @PathVariable UUID sessionId,
            
            @Parameter(description = "Recommendation request", required = true)
            @Valid @RequestBody GenerateRecommendationsRequest request,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Generating recommendations for sessionId: {}, userId: {}, sessionType: {}", 
            sessionId, userId, request.sessionType());
        
        List<RecommendationDTO> recommendations = musicRecommendationService.generateSessionRecommendations(
            userId, sessionId, request.sessionType(), request.preferences()
        );
        
        RecommendationResponse response = new RecommendationResponse(
            sessionId,
            userId,
            request.sessionType(),
            recommendations,
            System.currentTimeMillis(),
            "Generated based on your preferences and session context"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Updates recommendations for an existing session.
     * 
     * @param sessionId The session ID
     * @param request Update request
     * @param jwt JWT token containing user information
     * @return Updated recommendations
     */
    @PutMapping("/sessions/{sessionId}")
    @Operation(summary = "Update session recommendations", 
               description = "Update recommendations for an existing session with new preferences")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RecommendationResponse> updateSessionRecommendations(
            @Parameter(description = "Session ID", required = true)
            @PathVariable UUID sessionId,
            
            @Parameter(description = "Update request", required = true)
            @Valid @RequestBody UpdateRecommendationsRequest request,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Updating recommendations for sessionId: {}, userId: {}", sessionId, userId);
        
        List<RecommendationDTO> recommendations = musicRecommendationService.updateSessionRecommendations(
            sessionId, userId, request.newPreferences()
        );
        
        RecommendationResponse response = new RecommendationResponse(
            sessionId,
            userId,
            null, // Session type not changed in update
            recommendations,
            System.currentTimeMillis(),
            "Updated based on your new preferences"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets quick recommendations without creating a session.
     * 
     * @param request Quick recommendation request
     * @param jwt JWT token containing user information
     * @return List of quick recommendations
     */
    @PostMapping("/quick")
    @Operation(summary = "Get quick recommendations", 
               description = "Get quick music recommendations without creating a session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quick recommendations generated"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QuickRecommendationResponse> getQuickRecommendations(
            @Parameter(description = "Quick recommendation request", required = true)
            @Valid @RequestBody QuickRecommendationRequest request,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID tempSessionId = UUID.randomUUID(); // Temporary session for quick recommendations
        
        log.info("Generating quick recommendations for userId: {}, mood: {}", userId, request.mood());
        
        Map<String, Object> preferences = Map.of(
            "mood", request.mood(),
            "genres", request.genres(),
            "energy", request.energyLevel()
        );
        
        List<RecommendationDTO> recommendations = musicRecommendationService.generateSessionRecommendations(
            userId, tempSessionId, "quick", preferences
        );
        
        QuickRecommendationResponse response = new QuickRecommendationResponse(
            userId,
            request.mood(),
            request.genres(),
            recommendations,
            System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }

    // Request/Response DTOs

    /**
     * Request object for generating recommendations.
     */
    public record GenerateRecommendationsRequest(
        @NotBlank(message = "Session type is required")
        @Size(max = 50, message = "Session type must not exceed 50 characters")
        String sessionType,
        
        @Parameter(description = "User preferences for the session")
        Map<String, Object> preferences
    ) {}

    /**
     * Request object for updating recommendations.
     */
    public record UpdateRecommendationsRequest(
        @Parameter(description = "Updated preferences")
        Map<String, Object> newPreferences
    ) {}

    /**
     * Request object for quick recommendations.
     */
    public record QuickRecommendationRequest(
        @NotBlank(message = "Mood is required")
        @Size(max = 50, message = "Mood must not exceed 50 characters")
        String mood,
        
        @Size(max = 10, message = "Maximum 10 genres allowed")
        List<String> genres,
        
        @NotBlank(message = "Energy level is required")
        @Size(max = 20, message = "Energy level must not exceed 20 characters")
        String energyLevel
    ) {}

    /**
     * Response object for recommendations.
     */
    public record RecommendationResponse(
        UUID sessionId,
        UUID userId,
        String sessionType,
        List<RecommendationDTO> recommendations,
        long generatedAt,
        String description
    ) {}

    /**
     * Response object for quick recommendations.
     */
    public record QuickRecommendationResponse(
        UUID userId,
        String mood,
        List<String> genres,
        List<RecommendationDTO> recommendations,
        long generatedAt
    ) {}
}