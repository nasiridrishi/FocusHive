package com.focushive.buddy.controller;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyCheckinService;
import com.focushive.buddy.exception.BuddyServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Buddy Check-in operations.
 * Handles daily/weekly check-ins, analytics, streaks, and accountability scoring.
 *
 * Base path: /api/v1/buddy/checkins
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/buddy/checkins")
@RequiredArgsConstructor
@Validated
@Tag(name = "Buddy Check-ins", description = "Check-in management and accountability tracking operations")
public class BuddyCheckinController {

    private final BuddyCheckinService buddyCheckinService;

    // =================================================================================================
    // DAILY CHECK-IN MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Create daily check-in",
        description = "Create a new daily check-in for accountability tracking"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Daily check-in created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid check-in data or validation errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate check-in for today"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/daily")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<CheckinResponseDto>> createDailyCheckin(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Daily check-in details", required = true)
            @Valid @RequestBody CheckinRequestDto request) {

        log.info("Creating daily check-in for user {} in partnership {}", userId, request.getPartnershipId());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            // Validate request data - mood is validated by enum, no additional validation needed

            if (request.getProductivityRating() != null && (request.getProductivityRating() < 1 || request.getProductivityRating() > 10)) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Productivity score must be between 1 and 10"));
            }

            if (request.getFocusHours() != null && request.getFocusHours() < 0) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Focus hours cannot be negative"));
            }

            UUID userUuid = UUID.fromString(userId);
            CheckinResponseDto checkin = buddyCheckinService.createDailyCheckin(userUuid, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.focushive.buddy.dto.ApiResponse.success(checkin, "Daily check-in created successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Invalid check-in data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (BuddyServiceException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating daily check-in for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get check-in details",
        description = "Get detailed information about a specific check-in"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check-in details retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid check-in ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Check-in not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<CheckinResponseDto>> getCheckinDetails(
            @Parameter(description = "Check-in ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting check-in details for {} requested by {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            CheckinResponseDto checkin = buddyCheckinService.getDailyCheckin(userUuid, id);

            if (checkin == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(checkin, "Check-in details retrieved")
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error("Invalid check-in ID format"));
        } catch (Exception e) {
            log.error("Error getting check-in details for {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Update check-in",
        description = "Update an existing check-in"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check-in updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Check-in not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<CheckinResponseDto>> updateCheckin(
            @Parameter(description = "Check-in ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Updated check-in data", required = true)
            @Valid @RequestBody CheckinRequestDto request) {

        log.info("Updating check-in {} for user {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            CheckinResponseDto updatedCheckin = buddyCheckinService.updateDailyCheckin(userUuid, id, request);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(updatedCheckin, "Check-in updated successfully")
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error updating check-in {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Delete check-in",
        description = "Delete a check-in"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check-in deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Check-in not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> deleteCheckin(
            @Parameter(description = "Check-in ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Deleting check-in {} for user {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            buddyCheckinService.deleteCheckin(userUuid, id);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    Map.of("checkinId", id, "status", "deleted"),
                    "Check-in deleted successfully"
                )
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error deleting check-in {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get check-in history",
        description = "Get check-in history for a user and partnership within date range"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check-in history retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getCheckinHistory(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID")
            @RequestParam(required = false) UUID partnershipId,

            @Parameter(description = "Start date for history")
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(description = "End date for history")
            @RequestParam(required = false) LocalDate endDate,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        log.info("Getting check-in history for user {} in partnership {} from {} to {}, page: {}, size: {}",
                userId, partnershipId, startDate, endDate, page, size);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            // Set default date range if not provided
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("End date must be after start date"));
            }

            UUID userUuid = UUID.fromString(userId);
            List<CheckinResponseDto> history;

            if (partnershipId != null) {
                history = buddyCheckinService.getCheckinHistory(
                    userUuid, partnershipId, startDate, endDate
                );
            } else {
                // Get all checkins for user across all partnerships
                history = buddyCheckinService.getUserCheckins(userUuid, startDate, endDate);
            }

            // Apply pagination
            int totalElements = history.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<CheckinResponseDto> pagedContent = history;
            if (startIndex < totalElements) {
                pagedContent = history.subList(startIndex, endIndex);
            } else {
                pagedContent = new ArrayList<>();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("content", pagedContent);  // Paginated content
            data.put("totalElements", totalElements);  // Total count
            data.put("totalPages", (int) Math.ceil((double) totalElements / size));
            data.put("page", page);
            data.put("size", size);
            data.put("first", page == 0);
            data.put("last", endIndex >= totalElements);
            data.put("numberOfElements", pagedContent.size());

            // Keep backward compatibility
            data.put("checkins", pagedContent);
            data.put("totalCount", totalElements);
            data.put("period", Map.of("startDate", startDate, "endDate", endDate));

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Check-in history retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting check-in history for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // WEEKLY REVIEW MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Create weekly review",
        description = "Create a comprehensive weekly review"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Weekly review created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid review data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/weekly")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<WeeklyReviewDto>> createWeeklyReview(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Weekly review details", required = true)
            @Valid @RequestBody WeeklyReviewDto request) {

        log.info("Creating weekly review for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            UUID partnershipId = request.getPartnershipId() != null ? request.getPartnershipId() : UUID.randomUUID();
            WeeklyReviewDto review = buddyCheckinService.createWeeklyReview(userUuid, partnershipId, request);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.focushive.buddy.dto.ApiResponse.success(review, "Weekly review created successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating weekly review for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get weekly data aggregation",
        description = "Get aggregated weekly data for analysis"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly aggregation retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/weekly/aggregate")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<WeeklyReviewDto>> getWeeklyAggregation(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Week start date", required = true)
            @RequestParam LocalDate weekStart) {

        log.info("Getting weekly aggregation for user {} in partnership {} for week starting {}",
                userId, partnershipId, weekStart);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            WeeklyReviewDto aggregation = buddyCheckinService.aggregateWeeklyData(userUuid, partnershipId, weekStart);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(aggregation, "Weekly aggregation retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting weekly aggregation: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Calculate weekly progress",
        description = "Calculate comprehensive weekly progress metrics"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly progress calculated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/weekly/progress")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<ProductivityMetricsDto>> calculateWeeklyProgress(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Week start date", required = true)
            @RequestParam LocalDate weekStart) {

        log.info("Calculating weekly progress for user {} in partnership {} for week starting {}",
                userId, partnershipId, weekStart);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            ProductivityMetricsDto metrics = buddyCheckinService.calculateWeeklyProgress(userUuid, partnershipId, weekStart);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(metrics, "Weekly progress calculated")
            );

        } catch (Exception e) {
            log.error("Error calculating weekly progress: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get weekly insights",
        description = "Generate AI-powered weekly insights"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly insights generated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/weekly/insights")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getWeeklyInsights(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Week start date", required = true)
            @RequestParam LocalDate weekStart) {

        log.info("Getting weekly insights for user {} in partnership {} for week starting {}",
                userId, partnershipId, weekStart);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            String insights = buddyCheckinService.generateWeeklyInsights(userUuid, partnershipId, weekStart);

            Map<String, Object> data = Map.of(
                "insights", insights,
                "weekStart", weekStart,
                "partnershipId", partnershipId
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Weekly insights generated")
            );

        } catch (Exception e) {
            log.error("Error generating weekly insights: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // STREAK MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Get streak statistics",
        description = "Get comprehensive streak statistics for user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Streak statistics retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/streaks")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<StreakStatisticsDto>> getStreakStatistics(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        log.info("Getting streak statistics for user {} in partnership {}", userId, partnershipId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            StreakStatisticsDto stats = buddyCheckinService.getStreakStatistics(userUuid, partnershipId);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(stats, "Streak statistics retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting streak statistics: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Calculate daily streak",
        description = "Calculate current daily check-in streak"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Daily streak calculated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/streaks/daily")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<StreakStatisticsDto>> calculateDailyStreak(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            StreakStatisticsDto streak = buddyCheckinService.calculateDailyStreak(userUuid, partnershipId);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(streak, "Daily streak calculated")
            );

        } catch (Exception e) {
            log.error("Error calculating daily streak: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Calculate weekly streak",
        description = "Calculate current weekly check-in streak"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Weekly streak calculated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/streaks/weekly")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<StreakStatisticsDto>> calculateWeeklyStreak(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            StreakStatisticsDto streak = buddyCheckinService.calculateWeeklyStreak(userUuid, partnershipId);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(streak, "Weekly streak calculated")
            );

        } catch (Exception e) {
            log.error("Error calculating weekly streak: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get longest streak",
        description = "Get the longest streak achieved by user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Longest streak retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/streaks/longest")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<StreakStatisticsDto>> getLongestStreak(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            StreakStatisticsDto streak = buddyCheckinService.calculateLongestStreak(userUuid, partnershipId);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(streak, "Longest streak retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting longest streak: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Recover broken streak",
        description = "Attempt to recover a broken streak with valid reason"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Streak recovery processed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid recovery request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/streaks/recover")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> recoverStreak(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Streak recovery request", required = true)
            @Valid @RequestBody StreakRecoveryRequest request) {

        log.info("Processing streak recovery for user {} for date {}", userId, request.getRecoverDate());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            boolean recovered = buddyCheckinService.recoverStreak(
                userUuid, request.getPartnershipId(), request.getRecoverDate(), request.getReason()
            );

            Map<String, Object> data = Map.of(
                "recovered", recovered,
                "recoverDate", request.getRecoverDate(),
                "reason", request.getReason()
            );

            String message = recovered ? "Streak recovered successfully" : "Streak recovery failed";

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, message)
            );

        } catch (Exception e) {
            log.error("Error recovering streak: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // ACCOUNTABILITY SCORING
    // =================================================================================================

    @Operation(
        summary = "Get accountability score",
        description = "Get comprehensive accountability score for user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accountability score retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/accountability")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<AccountabilityScoreDto>> getAccountabilityScore(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        log.info("Getting accountability score for user {} in partnership {}", userId, partnershipId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            AccountabilityScoreDto score = buddyCheckinService.calculateAccountabilityScore(userUuid, partnershipId);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(score, "Accountability score retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting accountability score: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get accountability score history",
        description = "Get historical accountability scores for analysis"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Score history retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/accountability/history")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getAccountabilityHistory(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Start date for history")
            @RequestParam LocalDate startDate,

            @Parameter(description = "End date for history")
            @RequestParam LocalDate endDate) {

        log.info("Getting accountability history for user {} from {} to {}", userId, startDate, endDate);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("End date must be after start date"));
            }

            UUID userUuid = UUID.fromString(userId);
            List<AccountabilityScoreDto> history = buddyCheckinService.getScoreHistory(
                userUuid, partnershipId, startDate, endDate
            );

            Map<String, Object> data = Map.of(
                "scores", history,
                "totalCount", history.size(),
                "period", Map.of("startDate", startDate, "endDate", endDate)
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Score history retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting accountability history: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Compare accountability with partner",
        description = "Compare accountability scores between partners"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accountability comparison retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/accountability/compare")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> compareAccountabilityWithPartner(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        log.info("Comparing accountability for partnership {}", partnershipId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Map<String, AccountabilityScoreDto> comparison = buddyCheckinService.compareWithPartner(partnershipId);

            Map<String, Object> data = Map.of(
                "comparison", comparison,
                "partnershipId", partnershipId
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Accountability comparison retrieved")
            );

        } catch (Exception e) {
            log.error("Error comparing accountability: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get accountability improvement suggestions",
        description = "Get personalized suggestions to improve accountability score"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Improvement suggestions retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/accountability/suggestions")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getAccountabilityImprovementSuggestions(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            AccountabilityScoreDto currentScore = buddyCheckinService.calculateAccountabilityScore(userUuid, partnershipId);
            List<String> suggestions = buddyCheckinService.suggestScoreImprovement(userUuid, currentScore);

            Map<String, Object> data = Map.of(
                "currentScore", currentScore.getScore(),
                "suggestions", suggestions,
                "partnershipId", partnershipId
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Improvement suggestions retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting accountability suggestions: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // ANALYTICS AND INSIGHTS
    // =================================================================================================

    @Operation(
        summary = "Get comprehensive analytics",
        description = "Get comprehensive check-in analytics for specified period"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/analytics")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<CheckinAnalyticsDto>> getCheckinAnalytics(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Start date for analytics")
            @RequestParam LocalDate startDate,

            @Parameter(description = "End date for analytics")
            @RequestParam LocalDate endDate) {

        log.info("Getting check-in analytics for user {} from {} to {}", userId, startDate, endDate);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Start date must be before end date"));
            }

            UUID userUuid = UUID.fromString(userId);
            CheckinAnalyticsDto analytics = buddyCheckinService.generateCheckinAnalytics(
                userUuid, partnershipId, startDate, endDate
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(analytics, "Analytics retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Error getting check-in analytics: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Identify behavior patterns",
        description = "Identify behavioral patterns from check-in data"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Patterns identified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/patterns")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> identifyPatterns(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Start date for analysis")
            @RequestParam LocalDate startDate,

            @Parameter(description = "End date for analysis")
            @RequestParam LocalDate endDate) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            Map<String, Object> patterns = buddyCheckinService.identifyPatterns(
                userUuid, partnershipId, startDate, endDate
            );

            Map<String, Object> data = Map.of(
                "patterns", patterns,
                "period", Map.of("startDate", startDate, "endDate", endDate)
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Patterns identified successfully")
            );

        } catch (Exception e) {
            log.error("Error identifying patterns: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get predictive analytics",
        description = "Get AI-powered predictions for future behavior"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Predictions generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/predictions")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getPredictiveAnalytics(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            UUID userUuid = UUID.fromString(userId);
            LocalDateTime nextCheckin = buddyCheckinService.predictNextCheckin(userUuid, partnershipId);
            String optimalTime = buddyCheckinService.suggestOptimalCheckinTime(userUuid, partnershipId);

            Map<String, Object> data = Map.of(
                "predictedNextCheckin", nextCheckin,
                "optimalCheckinTime", optimalTime,
                "partnershipId", partnershipId
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Predictions generated successfully")
            );

        } catch (Exception e) {
            log.error("Error generating predictions: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Export check-in data",
        description = "Export check-in data in specified format for analysis"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data exported successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid export parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCheckinData(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership ID", required = true)
            @RequestParam UUID partnershipId,

            @Parameter(description = "Start date for export")
            @RequestParam LocalDate startDate,

            @Parameter(description = "End date for export")
            @RequestParam LocalDate endDate,

            @Parameter(description = "Export format")
            @RequestParam(defaultValue = "csv") String format) {

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            UUID userUuid = UUID.fromString(userId);
            byte[] exportData = buddyCheckinService.exportCheckinData(userUuid, partnershipId, startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Content-Disposition", "attachment; filename=checkin-data-" + userId + ".csv");

            return ResponseEntity.ok()
                .headers(headers)
                .body(exportData);

        } catch (Exception e) {
            log.error("Error exporting check-in data: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // =================================================================================================
    // HELPER CLASSES
    // =================================================================================================

    public static class StreakRecoveryRequest {
        private UUID partnershipId;
        private LocalDate recoverDate;
        private String reason;

        public UUID getPartnershipId() { return partnershipId; }
        public void setPartnershipId(UUID partnershipId) { this.partnershipId = partnershipId; }
        public LocalDate getRecoverDate() { return recoverDate; }
        public void setRecoverDate(LocalDate recoverDate) { this.recoverDate = recoverDate; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}