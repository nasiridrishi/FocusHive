package com.focushive.analytics.controller;

import com.focushive.analytics.dto.SessionRequest;
import com.focushive.analytics.dto.SessionResponse;
import com.focushive.analytics.dto.UserStats;
import com.focushive.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Productivity tracking and insights endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    @Operation(summary = "Start a focus session", description = "Records the beginning of a new focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Session started successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/sessions/start")
    public ResponseEntity<SessionResponse> startSession(
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SessionResponse response = analyticsService.startSession(request, getUserId(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @RequestBody EndSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SessionResponse response = analyticsService.endSession(sessionId, request, getUserId(userDetails));
        return ResponseEntity.ok(response);
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
            @Parameter(description = "End date") @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Users can only access their own stats unless they have admin role
        String currentUserId = getUserId(userDetails);
        if (!userId.equals(currentUserId) && !hasAdminRole(userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserStats stats = analyticsService.getUserStats(userId, startDate, endDate);
        return ResponseEntity.ok(stats);
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
            @Parameter(description = "Time period") @RequestParam(defaultValue = "WEEK") TimePeriod period,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Note: Access control for hive leaderboard should be implemented based on hive membership
        List<LeaderboardEntry> leaderboard = analyticsService.getHiveLeaderboard(hiveId, period);
        return ResponseEntity.ok(leaderboard);
    }
    
    // Additional endpoints for Linear task UOL-188 - specific URL patterns
    
    @Operation(summary = "Start focus session (alternative endpoint)", description = "Records the beginning of a new focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Session started successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/session/start")
    public ResponseEntity<SessionResponse> startFocusSession(
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SessionResponse response = analyticsService.startSession(request, getUserId(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "End focus session (alternative endpoint)", description = "Records the completion of a focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session ended successfully",
            content = @Content(schema = @Schema(implementation = SessionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid session data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/session/end")
    public ResponseEntity<SessionResponse> endFocusSession(
            @RequestBody EndFocusSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (request.sessionId() == null || request.sessionId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        EndSessionRequest endRequest = new EndSessionRequest(
            request.actualDurationMinutes(),
            request.completed(),
            request.breaksTaken(),
            request.distractionsLogged(),
            request.notes()
        );
        SessionResponse response = analyticsService.endSession(request.sessionId(), endRequest, getUserId(userDetails));
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get user statistics (alternative endpoint)", description = "Retrieves productivity statistics for current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserStats.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/user/stats")
    public ResponseEntity<UserStats> getCurrentUserStats(
            @Parameter(description = "Start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        String currentUserId = getUserId(userDetails);
        UserStats stats = analyticsService.getUserStats(currentUserId, startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    @Operation(summary = "Get leaderboard (alternative endpoint)", description = "Retrieves productivity leaderboard")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid hive ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @Parameter(description = "Hive ID") @RequestParam String hiveId,
            @Parameter(description = "Time period") @RequestParam(defaultValue = "WEEK") TimePeriod period,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (hiveId == null || hiveId.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<LeaderboardEntry> leaderboard = analyticsService.getHiveLeaderboard(hiveId, period);
        return ResponseEntity.ok(leaderboard);
    }
    
    public record EndSessionRequest(
        Integer actualDurationMinutes,
        Boolean completed,
        Integer breaksTaken,
        Integer distractionsLogged,
        String notes
    ) {}
    
    public record EndFocusSessionRequest(
        String sessionId,
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
    
    // Helper methods
    private String getUserId(UserDetails userDetails) {
        // Assuming the username is the user ID in this implementation
        // This might need to be adjusted based on your actual User implementation
        return userDetails.getUsername();
    }
    
    private boolean hasAdminRole(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}