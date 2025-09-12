package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.service.TimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for timer and productivity tracking.
 */
@RestController
@RequestMapping("/api/v1/timer")
@RequiredArgsConstructor
@Tag(name = "Timer", description = "Timer and productivity tracking operations")
public class TimerController {
    
    private final TimerService timerService;
    
    /**
     * Start a new focus/work/study session.
     */
    @PostMapping("/sessions/start")
    @Operation(summary = "Start a new session", 
               description = "Start a new focus, work, study, or break session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionDto> startSession(
            @Valid @RequestBody StartSessionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        FocusSessionDto session = timerService.startSession(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
    
    /**
     * End the current active session.
     */
    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End a session", 
               description = "End the specified session and calculate productivity stats")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToTimer(T(java.util.UUID).fromString(#sessionId))")
    public ResponseEntity<FocusSessionDto> endSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        FocusSessionDto session = timerService.endSession(userDetails.getUsername(), sessionId);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Pause the current active session.
     */
    @PostMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause a session", 
               description = "Pause the specified session (counts as interruption)")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToTimer(T(java.util.UUID).fromString(#sessionId))")
    public ResponseEntity<FocusSessionDto> pauseSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        FocusSessionDto session = timerService.pauseSession(userDetails.getUsername(), sessionId);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get current active session.
     */
    @GetMapping("/sessions/current")
    @Operation(summary = "Get current session", 
               description = "Get the user's current active session if any")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionDto> getCurrentSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        FocusSessionDto session = timerService.getCurrentSession(userDetails.getUsername());
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.noContent().build();
    }
    
    /**
     * Get session history.
     */
    @GetMapping("/sessions/history")
    @Operation(summary = "Get session history", 
               description = "Get paginated history of user's sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FocusSessionDto>> getSessionHistory(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<FocusSessionDto> sessions = timerService.getSessionHistory(
                userDetails.getUsername(), page, size);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Get daily productivity stats.
     */
    @GetMapping("/stats/daily")
    @Operation(summary = "Get daily stats", 
               description = "Get productivity statistics for a specific date")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductivityStatsDto> getDailyStats(
            @Parameter(description = "Date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ProductivityStatsDto stats = timerService.getDailyStats(userDetails.getUsername(), date);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get weekly productivity stats.
     */
    @GetMapping("/stats/weekly")
    @Operation(summary = "Get weekly stats", 
               description = "Get productivity statistics for a week starting from specified date")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductivityStatsDto>> getWeeklyStats(
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<ProductivityStatsDto> stats = timerService.getWeeklyStats(
                userDetails.getUsername(), startDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get monthly productivity stats.
     */
    @GetMapping("/stats/monthly")
    @Operation(summary = "Get monthly stats", 
               description = "Get productivity statistics for a specific month")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductivityStatsDto>> getMonthlyStats(
            @Parameter(description = "Year") @RequestParam int year,
            @Parameter(description = "Month (1-12)") @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<ProductivityStatsDto> stats = timerService.getMonthlyStats(
                userDetails.getUsername(), year, month);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get current streak.
     */
    @GetMapping("/stats/streak")
    @Operation(summary = "Get current streak", 
               description = "Get the number of consecutive days with completed sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Integer> getCurrentStreak(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Integer streak = timerService.getCurrentStreak(userDetails.getUsername());
        return ResponseEntity.ok(streak);
    }
    
    /**
     * Get Pomodoro settings.
     */
    @GetMapping("/pomodoro/settings")
    @Operation(summary = "Get Pomodoro settings", 
               description = "Get user's Pomodoro timer preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PomodoroSettingsDto> getPomodoroSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        PomodoroSettingsDto settings = timerService.getPomodoroSettings(userDetails.getUsername());
        return ResponseEntity.ok(settings);
    }
    
    /**
     * Update Pomodoro settings.
     */
    @PutMapping("/pomodoro/settings")
    @Operation(summary = "Update Pomodoro settings", 
               description = "Update user's Pomodoro timer preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PomodoroSettingsDto> updatePomodoroSettings(
            @Valid @RequestBody PomodoroSettingsDto settings,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        PomodoroSettingsDto updated = timerService.updatePomodoroSettings(
                userDetails.getUsername(), settings);
        return ResponseEntity.ok(updated);
    }
}