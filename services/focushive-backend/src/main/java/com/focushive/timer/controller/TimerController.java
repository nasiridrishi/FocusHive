package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.TimerTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.focushive.timer.service.FocusTimerService;
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
    
    private final FocusTimerService timerService;
    
    /**
     * Start a new focus/work/study session.
     */
    @PostMapping("/sessions/start")
    @Operation(summary = "Start a new session",
               description = "Start a new focus, work, study, or break session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionResponse> startSession(
            @Valid @RequestBody StartTimerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        request.setUserId(userDetails.getUsername());
        FocusSessionResponse session = timerService.startTimer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }
    
    /**
     * End the current active session.
     */
    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End a session",
               description = "End the specified session and calculate productivity stats")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToTimer(T(java.util.UUID).fromString(#sessionId))")
    public ResponseEntity<FocusSessionResponse> endSession(
            @PathVariable String sessionId,
            @RequestParam(required = false) Integer productivityScore,
            @AuthenticationPrincipal UserDetails userDetails) {

        FocusSessionResponse session = timerService.completeTimer(sessionId, userDetails.getUsername(), productivityScore);
        return ResponseEntity.ok(session);
    }
    
    /**
     * Pause the current active session.
     */
    @PostMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause a session",
               description = "Pause the specified session (counts as interruption)")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToTimer(T(java.util.UUID).fromString(#sessionId))")
    public ResponseEntity<FocusSessionResponse> pauseSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        FocusSessionResponse session = timerService.pauseTimer(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(session);
    }
    
    /**
     * Get current active session.
     */
    @GetMapping("/sessions/current")
    @Operation(summary = "Get current session",
               description = "Get the user's current active session if any")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionResponse> getCurrentSession(
            @AuthenticationPrincipal UserDetails userDetails) {

        FocusSessionResponse session = timerService.getActiveSession(userDetails.getUsername());
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.noContent().build();
    }
    
    /**
     * Resume a paused session.
     */
    @PostMapping("/sessions/{sessionId}/resume")
    @Operation(summary = "Resume a session",
               description = "Resume a paused session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionResponse> resumeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        FocusSessionResponse session = timerService.resumeTimer(sessionId, userDetails.getUsername());
        return ResponseEntity.ok(session);
    }

    /**
     * Cancel a session.
     */
    @PostMapping("/sessions/{sessionId}/cancel")
    @Operation(summary = "Cancel a session",
               description = "Cancel an active or paused session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FocusSessionResponse> cancelSession(
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "User cancelled") String reason,
            @AuthenticationPrincipal UserDetails userDetails) {

        FocusSessionResponse session = timerService.cancelTimer(sessionId, userDetails.getUsername(), reason);
        return ResponseEntity.ok(session);
    }

    /**
     * Get session history.
     */
    @GetMapping("/sessions/history")
    @Operation(summary = "Get session history",
               description = "Get paginated history of user's sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FocusSessionResponse>> getSessionHistory(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Page<FocusSessionResponse> sessions = timerService.getUserSessions(
                userDetails.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Get user statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get user statistics",
               description = "Get timer statistics for date range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimerStatisticsResponse> getUserStats(
            @Parameter(description = "Start date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {

        TimerStatisticsResponse stats = timerService.getUserStatistics(
                userDetails.getUsername(),
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay());
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

        Integer streak = timerService.calculateUserStreak(userDetails.getUsername());
        return ResponseEntity.ok(streak);
    }

    /**
     * Get user templates.
     */
    @GetMapping("/templates")
    @Operation(summary = "Get user templates",
               description = "Get all timer templates for the user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TimerTemplate>> getUserTemplates(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TimerTemplate> templates = timerService.getUserTemplates(userDetails.getUsername());
        return ResponseEntity.ok(templates);
    }

    /**
     * Get system templates.
     */
    @GetMapping("/templates/system")
    @Operation(summary = "Get system templates",
               description = "Get predefined system timer templates")
    public ResponseEntity<List<TimerTemplate>> getSystemTemplates() {
        List<TimerTemplate> templates = timerService.getSystemTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Create template.
     */
    @PostMapping("/templates")
    @Operation(summary = "Create template",
               description = "Create a new timer template")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimerTemplate> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        request.setUserId(userDetails.getUsername());
        TimerTemplate template = timerService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }
}