package com.focushive.analytics.controller;

import com.focushive.analytics.dto.*;
import com.focushive.analytics.entity.DailyGoal;
import com.focushive.analytics.enums.ReportPeriod;
import com.focushive.analytics.service.EnhancedAnalyticsService;
import com.focushive.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Enhanced Analytics Controller providing comprehensive productivity tracking,
 * achievement management, goal setting, and detailed reporting endpoints.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enhanced Analytics", description = "Comprehensive analytics and productivity tracking endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EnhancedAnalyticsController {

    private final EnhancedAnalyticsService analyticsService;

    // ==================== PRODUCTIVITY METRICS ====================

    @GetMapping("/user/{userId}/summary")
    @Operation(summary = "Get user productivity summary",
              description = "Retrieve comprehensive productivity summary for the last 30 days")
    public ResponseEntity<ApiResponse<ProductivitySummaryResponse>> getUserProductivitySummary(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.info("Getting productivity summary for user: {}", userId);

        // In a real implementation, verify user access permissions
        ProductivitySummaryResponse summary = analyticsService.getUserProductivitySummary(userId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/user/{userId}/report")
    @Operation(summary = "Get detailed analytics report",
              description = "Generate detailed analytics report for specified period")
    public ResponseEntity<ApiResponse<DetailedReportResponse>> getUserDetailedReport(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Report period") @RequestParam(defaultValue = "WEEKLY") ReportPeriod period,
            Authentication authentication) {

        log.info("Generating detailed report for user: {}, period: {}", userId, period);

        DetailedReportResponse report = analyticsService.getUserDetailedReport(userId, period);

        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== STREAK MANAGEMENT ====================

    @GetMapping("/streaks/{userId}")
    @Operation(summary = "Get user streak information",
              description = "Retrieve comprehensive streak tracking and milestone information")
    public ResponseEntity<ApiResponse<StreakInfoResponse>> getStreakInformation(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.info("Getting streak information for user: {}", userId);

        StreakInfoResponse streakInfo = analyticsService.getStreakInformation(userId);

        return ResponseEntity.ok(ApiResponse.success(streakInfo));
    }

    // ==================== ACHIEVEMENT SYSTEM ====================

    @GetMapping("/achievements/{userId}")
    @Operation(summary = "Get user achievement progress",
              description = "Retrieve all achievement progress and unlock status for user")
    public ResponseEntity<ApiResponse<AchievementProgressResponse>> getUserAchievements(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.info("Getting achievements for user: {}", userId);

        AchievementProgressResponse achievements = analyticsService.getUserAchievements(userId);

        return ResponseEntity.ok(ApiResponse.success(achievements));
    }

    // ==================== GOAL SETTING ====================

    @PostMapping("/goals")
    @Operation(summary = "Set daily goal",
              description = "Create or update daily focus goal for user")
    public ResponseEntity<ApiResponse<DailyGoal>> setDailyGoal(
            @Valid @RequestBody DailyGoalRequest request,
            Authentication authentication) {

        String userId = authentication.getName(); // Get from authenticated user
        log.info("Setting daily goal for user: {}, target: {} minutes", userId, request.getTargetMinutes());

        DailyGoal goal = analyticsService.setDailyGoal(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(goal, "Daily goal set successfully"));
    }

    @GetMapping("/goals/{userId}/today")
    @Operation(summary = "Get today's goal",
              description = "Retrieve today's goal progress and status")
    public ResponseEntity<ApiResponse<DailyGoalResponse>> getTodaysGoal(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.info("Getting today's goal for user: {}", userId);

        DailyGoalResponse goal = analyticsService.getDailyGoal(userId);

        if (goal == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No goal set for today"));
        }

        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @PutMapping("/goals/{userId}/progress")
    @Operation(summary = "Update goal progress",
              description = "Add minutes to today's goal progress")
    public ResponseEntity<ApiResponse<String>> updateGoalProgress(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Minutes to add") @RequestParam int minutes,
            Authentication authentication) {

        log.info("Updating goal progress for user: {}, adding: {} minutes", userId, minutes);

        analyticsService.updateGoalProgress(userId, minutes);

        return ResponseEntity.ok(ApiResponse.success("Goal progress updated successfully"));
    }

    // ==================== HIVE ANALYTICS ====================

    @GetMapping("/hive/{hiveId}/summary")
    @Operation(summary = "Get hive analytics summary",
              description = "Retrieve comprehensive analytics for hive performance")
    public ResponseEntity<ApiResponse<HiveAnalyticsResponse>> getHiveAnalytics(
            @Parameter(description = "Hive ID") @PathVariable String hiveId,
            Authentication authentication) {

        log.info("Getting hive analytics for hive: {}", hiveId);

        HiveAnalyticsResponse analytics = analyticsService.getHiveAnalytics(hiveId);

        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    // ==================== DATA EXPORT ====================

    @GetMapping("/export/{userId}")
    @Operation(summary = "Export user analytics data",
              description = "Export all analytics data for user in JSON format")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportUserAnalyticsData(
            @Parameter(description = "User ID") @PathVariable String userId,
            Authentication authentication) {

        log.info("Exporting analytics data for user: {}", userId);

        Map<String, Object> exportData = analyticsService.exportUserAnalyticsData(userId);

        return ResponseEntity.ok(ApiResponse.success(exportData, "Analytics data exported successfully"));
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    @Operation(summary = "Analytics service health check",
              description = "Check if analytics service is operational")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {

        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "Enhanced Analytics Service",
            "timestamp", System.currentTimeMillis(),
            "features", Map.of(
                "productivityTracking", true,
                "achievementSystem", true,
                "goalSetting", true,
                "hiveAnalytics", true,
                "streakTracking", true,
                "dataExport", true
            )
        );

        return ResponseEntity.ok(ApiResponse.success(health));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/platform-stats")
    @Operation(summary = "Get platform-wide analytics",
              description = "Retrieve aggregated analytics across all users (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStatistics(
            Authentication authentication) {

        log.info("Getting platform statistics");

        // Placeholder for platform-wide statistics
        Map<String, Object> stats = Map.of(
            "message", "Platform statistics endpoint - to be implemented",
            "status", "Coming soon"
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== ERROR HANDLING ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("Error in analytics controller: ", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An error occurred while processing analytics request"));
    }
}