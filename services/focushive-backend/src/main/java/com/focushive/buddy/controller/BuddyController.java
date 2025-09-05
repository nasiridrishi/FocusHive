package com.focushive.buddy.controller;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@ConditionalOnProperty(name = "app.features.buddy.enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping("/api/buddy")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Buddy System", description = "Buddy relationship and accountability features")
public class BuddyController {
    
    private final BuddyService buddyService;
    
    // Buddy Relationship Endpoints
    
    @PostMapping("/request")
    @Operation(summary = "Send buddy request", description = "Send a buddy request to another user")
    @ApiResponse(responseCode = "201", description = "Buddy request sent successfully")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipDTO> sendBuddyRequest(
            @AuthenticationPrincipal String currentUserId,
            @Valid @RequestBody BuddyRequestDTO request) {
        
        log.info("User {} sending buddy request to user {}", currentUserId, request.getToUserId());
        BuddyRelationshipDTO relationship = buddyService.sendBuddyRequest(currentUserId, request.getToUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(relationship);
    }
    
    @PutMapping("/request/{relationshipId}/accept")
    @Operation(summary = "Accept buddy request", description = "Accept a pending buddy request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipDTO> acceptBuddyRequest(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String relationshipId) {
        
        log.info("User {} accepting buddy request {}", currentUserId, relationshipId);
        BuddyRelationshipDTO relationship = buddyService.acceptBuddyRequest(relationshipId, currentUserId);
        return ResponseEntity.ok(relationship);
    }
    
    @PutMapping("/request/{relationshipId}/reject")
    @Operation(summary = "Reject buddy request", description = "Reject a pending buddy request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipDTO> rejectBuddyRequest(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String relationshipId) {
        
        log.info("User {} rejecting buddy request {}", currentUserId, relationshipId);
        BuddyRelationshipDTO relationship = buddyService.rejectBuddyRequest(relationshipId, currentUserId);
        return ResponseEntity.ok(relationship);
    }
    
    @DeleteMapping("/relationship/{relationshipId}")
    @Operation(summary = "Terminate buddy relationship", description = "End an active buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipDTO> terminateRelationship(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String relationshipId,
            @RequestParam(required = false) String reason) {
        
        log.info("User {} terminating buddy relationship {}", currentUserId, relationshipId);
        BuddyRelationshipDTO relationship = buddyService.terminateBuddyRelationship(relationshipId, currentUserId, reason);
        return ResponseEntity.ok(relationship);
    }
    
    @GetMapping("/relationships/active")
    @Operation(summary = "Get active buddies", description = "Get list of current active buddy relationships")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyRelationshipDTO>> getActiveBuddies(
            @AuthenticationPrincipal String currentUserId) {
        
        List<BuddyRelationshipDTO> buddies = buddyService.getActiveBuddies(currentUserId);
        return ResponseEntity.ok(buddies);
    }
    
    @GetMapping("/requests/pending")
    @Operation(summary = "Get pending requests", description = "Get list of pending buddy requests received")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyRelationshipDTO>> getPendingRequests(
            @AuthenticationPrincipal String currentUserId) {
        
        List<BuddyRelationshipDTO> requests = buddyService.getPendingRequests(currentUserId);
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/requests/sent")
    @Operation(summary = "Get sent requests", description = "Get list of buddy requests sent by the user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyRelationshipDTO>> getSentRequests(
            @AuthenticationPrincipal String currentUserId) {
        
        List<BuddyRelationshipDTO> requests = buddyService.getSentRequests(currentUserId);
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/relationship/{relationshipId}")
    @Operation(summary = "Get buddy relationship details", description = "Get detailed information about a specific buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipDTO> getRelationship(@PathVariable String relationshipId) {
        BuddyRelationshipDTO relationship = buddyService.getBuddyRelationship(relationshipId);
        return ResponseEntity.ok(relationship);
    }
    
    // Buddy Matching Endpoints
    
    @GetMapping("/matches")
    @Operation(summary = "Find potential buddy matches", description = "Get list of potential buddy matches based on preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyMatchDTO>> findPotentialMatches(
            @AuthenticationPrincipal String currentUserId) {
        
        List<BuddyMatchDTO> matches = buddyService.findPotentialMatches(currentUserId);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/match-score/{userId}")
    @Operation(summary = "Calculate match score", description = "Calculate compatibility score with a specific user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyMatchScoreDTO> calculateMatchScore(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String userId) {
        
        BuddyMatchScoreDTO score = buddyService.calculateMatchScore(currentUserId, userId);
        return ResponseEntity.ok(score);
    }
    
    // Buddy Preferences Endpoints
    
    @GetMapping("/preferences")
    @Operation(summary = "Get user preferences", description = "Get buddy matching preferences for the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyPreferencesDTO> getUserPreferences(
            @AuthenticationPrincipal String currentUserId) {
        
        BuddyPreferencesDTO preferences = buddyService.getUserPreferences(currentUserId);
        return ResponseEntity.ok(preferences);
    }
    
    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences", description = "Update buddy matching preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyPreferencesDTO> updateUserPreferences(
            @AuthenticationPrincipal String currentUserId,
            @Valid @RequestBody BuddyPreferencesDTO preferences) {
        
        BuddyPreferencesDTO updated = buddyService.updateUserPreferences(currentUserId, preferences);
        return ResponseEntity.ok(updated);
    }
    
    // Buddy Goals Endpoints
    
    @PostMapping("/relationship/{relationshipId}/goals")
    @Operation(summary = "Create buddy goal", description = "Create a new shared goal for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyGoalDTO> createGoal(
            @PathVariable String relationshipId,
            @Valid @RequestBody BuddyGoalDTO goal) {
        
        BuddyGoalDTO created = buddyService.createGoal(relationshipId, goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/goals/{goalId}")
    @Operation(summary = "Update buddy goal", description = "Update an existing buddy goal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyGoalDTO> updateGoal(
            @PathVariable String goalId,
            @Valid @RequestBody BuddyGoalDTO goal) {
        
        BuddyGoalDTO updated = buddyService.updateGoal(goalId, goal);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/goals/{goalId}/complete")
    @Operation(summary = "Mark goal as complete", description = "Mark a buddy goal as completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyGoalDTO> completeGoal(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String goalId) {
        
        BuddyGoalDTO completed = buddyService.markGoalComplete(goalId, currentUserId);
        return ResponseEntity.ok(completed);
    }
    
    @GetMapping("/relationship/{relationshipId}/goals")
    @Operation(summary = "Get relationship goals", description = "Get all goals for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyGoalDTO>> getRelationshipGoals(@PathVariable String relationshipId) {
        List<BuddyGoalDTO> goals = buddyService.getRelationshipGoals(relationshipId);
        return ResponseEntity.ok(goals);
    }
    
    @GetMapping("/relationship/{relationshipId}/goals/active")
    @Operation(summary = "Get active goals", description = "Get active goals for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyGoalDTO>> getActiveGoals(@PathVariable String relationshipId) {
        List<BuddyGoalDTO> goals = buddyService.getRelationshipGoals(relationshipId);
        return ResponseEntity.ok(goals);
    }
    
    // Buddy Check-in Endpoints
    
    @PostMapping("/relationship/{relationshipId}/checkin")
    @Operation(summary = "Create check-in", description = "Create a new check-in for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyCheckinDTO> createCheckin(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String relationshipId,
            @Valid @RequestBody BuddyCheckinDTO checkin) {
        
        BuddyCheckinDTO created = buddyService.createCheckin(relationshipId, currentUserId, checkin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/relationship/{relationshipId}/checkins")
    @Operation(summary = "Get check-ins", description = "Get check-in history for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddyCheckinDTO>> getRelationshipCheckins(
            @PathVariable String relationshipId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        List<BuddyCheckinDTO> checkins = buddyService.getRelationshipCheckins(relationshipId, pageable);
        return ResponseEntity.ok(checkins);
    }
    
    @GetMapping("/relationship/{relationshipId}/checkins/stats")
    @Operation(summary = "Get check-in stats", description = "Get check-in statistics for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyCheckinStatsDTO> getCheckinStats(@PathVariable String relationshipId) {
        BuddyCheckinStatsDTO stats = buddyService.getCheckinStats(relationshipId);
        return ResponseEntity.ok(stats);
    }
    
    // Buddy Session Endpoints
    
    @PostMapping("/relationship/{relationshipId}/sessions")
    @Operation(summary = "Schedule buddy session", description = "Schedule a new buddy work session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> scheduleSession(
            @PathVariable String relationshipId,
            @Valid @RequestBody BuddySessionDTO session) {
        
        BuddySessionDTO scheduled = buddyService.scheduleSession(relationshipId, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduled);
    }
    
    @PutMapping("/sessions/{sessionId}")
    @Operation(summary = "Update session", description = "Update a scheduled buddy session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> updateSession(
            @PathVariable String sessionId,
            @Valid @RequestBody BuddySessionDTO session) {
        
        BuddySessionDTO updated = buddyService.updateSession(sessionId, session);
        return ResponseEntity.ok(updated);
    }
    
    @PutMapping("/sessions/{sessionId}/start")
    @Operation(summary = "Start session", description = "Mark user as joined to start a buddy session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> startSession(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String sessionId) {
        
        BuddySessionDTO session = buddyService.startSession(sessionId, currentUserId);
        return ResponseEntity.ok(session);
    }
    
    @PutMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End session", description = "Mark user as left to end a buddy session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> endSession(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String sessionId) {
        
        BuddySessionDTO session = buddyService.endSession(sessionId, currentUserId);
        return ResponseEntity.ok(session);
    }
    
    @PutMapping("/sessions/{sessionId}/cancel")
    @Operation(summary = "Cancel session", description = "Cancel a scheduled buddy session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> cancelSession(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String sessionId,
            @RequestParam(required = false) String reason) {
        
        BuddySessionDTO session = buddyService.cancelSession(sessionId, currentUserId, reason);
        return ResponseEntity.ok(session);
    }
    
    @PostMapping("/sessions/{sessionId}/rate")
    @Operation(summary = "Rate session", description = "Rate and provide feedback for a completed buddy session")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddySessionDTO> rateSession(
            @AuthenticationPrincipal String currentUserId,
            @PathVariable String sessionId,
            @RequestParam @Parameter(description = "Rating from 1 to 5") Integer rating,
            @RequestParam(required = false) String feedback) {
        
        BuddySessionDTO session = buddyService.rateSession(sessionId, currentUserId, rating, feedback);
        return ResponseEntity.ok(session);
    }
    
    @GetMapping("/sessions/upcoming")
    @Operation(summary = "Get upcoming sessions", description = "Get list of upcoming buddy sessions for the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddySessionDTO>> getUpcomingSessions(
            @AuthenticationPrincipal String currentUserId) {
        
        List<BuddySessionDTO> sessions = buddyService.getUpcomingSessions(currentUserId);
        return ResponseEntity.ok(sessions);
    }
    
    @GetMapping("/relationship/{relationshipId}/sessions")
    @Operation(summary = "Get relationship sessions", description = "Get all sessions for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BuddySessionDTO>> getRelationshipSessions(
            @PathVariable String relationshipId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        List<BuddySessionDTO> sessions = buddyService.getRelationshipSessions(relationshipId, pageable);
        return ResponseEntity.ok(sessions);
    }
    
    // Statistics Endpoints
    
    @GetMapping("/relationship/{relationshipId}/stats")
    @Operation(summary = "Get relationship statistics", description = "Get comprehensive statistics for a buddy relationship")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuddyRelationshipStatsDTO> getRelationshipStats(@PathVariable String relationshipId) {
        BuddyRelationshipStatsDTO stats = buddyService.getRelationshipStats(relationshipId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get user buddy stats", description = "Get buddy system statistics for the current user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserBuddyStatsDTO> getUserStats(@AuthenticationPrincipal String currentUserId) {
        UserBuddyStatsDTO stats = buddyService.getUserBuddyStats(currentUserId);
        return ResponseEntity.ok(stats);
    }
}