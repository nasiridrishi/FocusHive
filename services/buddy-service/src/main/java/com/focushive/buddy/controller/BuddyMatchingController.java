package com.focushive.buddy.controller;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Buddy Matching operations.
 * Handles matching queue management, compatibility calculation, and preferences.
 *
 * Base path: /api/v1/buddy/matching
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/buddy/matching")
@RequiredArgsConstructor
@Validated
@Tag(name = "Buddy Matching", description = "Buddy matching and compatibility operations")
public class BuddyMatchingController {

    private final BuddyMatchingService buddyMatchingService;

    // =================================================================================================
    // MATCHING QUEUE OPERATIONS
    // =================================================================================================

    @Operation(
        summary = "Join matching queue",
        description = "Add user to the matching queue to find potential buddy partners"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully joined matching queue"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or user not eligible"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/queue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinMatchingQueue(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("User {} joining matching queue", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            boolean added = buddyMatchingService.addToMatchingQueue(userId);

            Map<String, Object> data = Map.of(
                "inQueue", added,
                "userId", userId
            );

            return ResponseEntity.ok(
                ApiResponse.success(data, "Successfully joined matching queue")
            );

        } catch (Exception e) {
            log.error("Error joining matching queue for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "Leave matching queue",
        description = "Remove user from the matching queue"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully left matching queue"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/queue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> leaveMatchingQueue(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("User {} leaving matching queue", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            boolean removed = buddyMatchingService.removeFromMatchingQueue(userId);

            Map<String, Object> data = Map.of(
                "inQueue", false,
                "userId", userId
            );

            String message = removed ?
                "Successfully left matching queue" :
                "User was not in matching queue";

            return ResponseEntity.ok(
                ApiResponse.success(data, message)
            );

        } catch (Exception e) {
            log.error("Error leaving matching queue for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "Get queue status",
        description = "Check if user is currently in the matching queue"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Queue status retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/queue/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQueueStatus(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            boolean inQueue = buddyMatchingService.isUserInMatchingQueue(userId);

            Map<String, Object> data = Map.of(
                "inQueue", inQueue,
                "userId", userId
            );

            return ResponseEntity.ok(
                ApiResponse.success(data, "Queue status retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting queue status for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get queue size (Admin only)",
        description = "Get current matching queue size - requires admin privileges"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Queue size retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/queue/size")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQueueSize(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,
            @Parameter(description = "User role from authentication context")
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        try {
            if (!"ADMIN".equals(userRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Admin access required"));
            }

            Set<String> queueMembers = buddyMatchingService.getUsersInMatchingQueue();

            Map<String, Object> data = Map.of(
                "queueSize", queueMembers.size()
            );

            return ResponseEntity.ok(
                ApiResponse.success(data, "Queue size retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting queue size: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // MATCH SUGGESTIONS
    // =================================================================================================

    @Operation(
        summary = "Get match suggestions",
        description = "Get personalized buddy match suggestions based on compatibility"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Match suggestions retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMatchSuggestions(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Maximum number of suggestions to return (1-100)")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Limit must be between 1 and 100")
            @Max(value = 100, message = "Limit must be between 1 and 100")
            int limit,

            @Parameter(description = "Minimum compatibility threshold (0.0-1.0)")
            @RequestParam(required = false)
            @Min(value = 0, message = "Threshold must be between 0.0 and 1.0")
            @Max(value = 1, message = "Threshold must be between 0.0 and 1.0")
            Double threshold) {

        log.info("Getting match suggestions for user {} with limit {} and threshold {}",
                userId, limit, threshold);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            List<PotentialMatchDto> matches;

            if (threshold != null) {
                matches = buddyMatchingService.findPotentialMatchesWithThreshold(userId, limit, threshold);
            } else {
                matches = buddyMatchingService.findPotentialMatches(userId, limit);
            }

            Map<String, Object> data = Map.of(
                "matches", matches,
                "totalMatches", matches.size(),
                "limit", limit
            );

            if (threshold != null) {
                data = Map.of(
                    "matches", matches,
                    "totalMatches", matches.size(),
                    "limit", limit,
                    "threshold", threshold
                );
            }

            String message = matches.isEmpty() ?
                "No potential matches found" :
                "Match suggestions retrieved successfully";

            return ResponseEntity.ok(
                ApiResponse.success(data, message)
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for match suggestions: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Service state error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting match suggestions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // COMPATIBILITY CALCULATION
    // =================================================================================================

    @Operation(
        summary = "Calculate compatibility score",
        description = "Calculate detailed compatibility score between two users"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Compatibility calculated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or users"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient compatibility"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CompatibilityScoreDto>> calculateCompatibility(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Compatibility calculation request", required = true)
            @Valid @RequestBody CompatibilityCalculationRequest request) {

        log.info("Calculating compatibility between {} and {}", userId, request.getTargetUserId());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            if (request.getTargetUserId() == null || request.getTargetUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Target user ID is required"));
            }

            if (userId.equals(request.getTargetUserId())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot calculate compatibility with yourself"));
            }

            CompatibilityScoreDto compatibility = buddyMatchingService
                .getCompatibilityBreakdown(userId, request.getTargetUserId());

            return ResponseEntity.ok(
                ApiResponse.success(compatibility, "Compatibility calculated successfully")
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments for compatibility calculation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (com.focushive.buddy.exception.InsufficientCompatibilityException e) {
            log.warn("Insufficient compatibility: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error calculating compatibility: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // MATCHING PREFERENCES MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Get matching preferences",
        description = "Get user's matching preferences or create default ones"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<MatchingPreferencesDto>> getMatchingPreferences(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting matching preferences for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            MatchingPreferencesDto preferences = buddyMatchingService.getMatchingPreferences(userId);

            if (preferences == null) {
                // Create default preferences
                preferences = buddyMatchingService.getOrCreateMatchingPreferences(userId);
                return ResponseEntity.ok(
                    ApiResponse.success(preferences, "Default preferences created")
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(preferences, "Preferences retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Error getting matching preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Update matching preferences",
        description = "Update user's matching preferences and settings"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or validation errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<MatchingPreferencesDto>> updateMatchingPreferences(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Updated matching preferences", required = true)
            @Valid @RequestBody MatchingPreferencesDto preferences) {

        log.info("Updating matching preferences for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID header is required"));
            }

            // Validate that the user ID in the request matches the authenticated user
            if (preferences.getUserId() != null && !userId.equals(preferences.getUserId())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User ID in request body must match header"));
            }

            // Ensure the user ID is set
            preferences.setUserId(userId);

            MatchingPreferencesDto updatedPreferences = buddyMatchingService
                .updateMatchingPreferences(preferences);

            return ResponseEntity.ok(
                ApiResponse.success(updatedPreferences, "Matching preferences updated successfully")
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating matching preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // UTILITY CLASSES
    // =================================================================================================

    /**
     * Request DTO for compatibility calculation
     */
    public static class CompatibilityCalculationRequest {
        private String targetUserId;

        public String getTargetUserId() {
            return targetUserId;
        }

        public void setTargetUserId(String targetUserId) {
            this.targetUserId = targetUserId;
        }
    }
}