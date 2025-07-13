package com.focushive.analytics.controller;

import com.focushive.analytics.dto.SessionRequest;
import com.focushive.analytics.dto.SessionResponse;
import com.focushive.analytics.dto.UserStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Productivity tracking and insights endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {
    
    @Operation(summary = "Start a focus session", description = "Records the beginning of a new focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Session started successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/sessions/start")
    public ResponseEntity<SessionResponse> startSession(@Valid @RequestBody SessionRequest request) {
        // TODO: Implement session start logic
        return ResponseEntity.status(HttpStatus.CREATED).body(new SessionResponse());
    }
    
    @Operation(summary = "End a focus session", description = "Records the completion of a focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session ended successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<SessionResponse> endSession(
            @Parameter(description = "Session ID") @PathVariable String sessionId,
            @RequestBody EndSessionRequest request) {
        // TODO: Implement session end logic
        return ResponseEntity.ok(new SessionResponse());
    }
    
    @Operation(summary = "Get user statistics", description = "Retrieves productivity statistics for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserStats.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<UserStats> getUserStats(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) LocalDate endDate) {
        // TODO: Implement get user stats logic
        return ResponseEntity.ok(new UserStats());
    }
    
    @Operation(summary = "Get hive leaderboard", description = "Retrieves productivity leaderboard for a hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/hives/{hiveId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getHiveLeaderboard(
            @Parameter(description = "Hive ID") @PathVariable String hiveId,
            @Parameter(description = "Time period") @RequestParam(defaultValue = "WEEK") TimePeriod period) {
        // TODO: Implement leaderboard logic
        return ResponseEntity.ok(List.of());
    }
    
    public record EndSessionRequest(
        Integer actualDurationMinutes,
        Boolean completed,
        Integer breaksTaken,
        Integer distractionsLogged,
        String notes
    ) {}
    
    public record LeaderboardEntry(
        String userId,
        String username,
        Integer totalMinutes,
        Integer sessionsCompleted,
        Double completionRate,
        Integer rank
    ) {}
    
    public enum TimePeriod {
        DAY, WEEK, MONTH, ALL_TIME
    }
}