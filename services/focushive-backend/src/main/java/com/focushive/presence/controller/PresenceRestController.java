package com.focushive.presence.controller;

import com.focushive.presence.dto.FocusSession;
import com.focushive.presence.dto.HivePresenceInfo;
import com.focushive.presence.dto.UserPresence;
import com.focushive.presence.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API controller for presence queries.
 * Real-time updates are handled via WebSocket.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/presence")
@Tag(name = "Presence", description = "Real-time presence and focus session management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PresenceRestController {
    
    private final PresenceService presenceService;
    
    @Operation(summary = "Get user presence", description = "Gets the current presence status of a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presence retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found or offline"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserPresence> getUserPresence(
            @Parameter(description = "User ID") @PathVariable String userId) {
        UserPresence presence = presenceService.getUserPresence(userId);
        return presence != null 
                ? ResponseEntity.ok(presence) 
                : ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Get my presence", description = "Gets the current user's presence status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presence retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Not online"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserPresence> getMyPresence(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserPresence presence = presenceService.getUserPresence(getUserId(userDetails));
        return presence != null 
                ? ResponseEntity.ok(presence) 
                : ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Get hive presence", description = "Gets presence information for a hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hive presence retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/hives/{hiveId}")
    public ResponseEntity<HivePresenceInfo> getHivePresence(
            @Parameter(description = "Hive ID") @PathVariable String hiveId) {
        List<UserPresence> activeUsers = presenceService.getHiveActiveUsers(hiveId);
        List<FocusSession> sessions = presenceService.getHiveFocusSessions(hiveId);
        
        HivePresenceInfo info = HivePresenceInfo.builder()
                .hiveId(hiveId)
                .activeUsers(activeUsers.size())
                .focusingSessions((int) sessions.stream()
                        .filter(s -> s.getStatus() == FocusSession.SessionStatus.ACTIVE)
                        .count())
                .onlineMembers(activeUsers)
                .lastActivity(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.ok(info);
    }
    
    @Operation(summary = "Get multiple hives presence", description = "Gets presence information for multiple hives")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presence information retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/hives/batch")
    public ResponseEntity<Map<String, HivePresenceInfo>> getMultipleHivesPresence(
            @RequestBody Set<String> hiveIds) {
        Map<String, HivePresenceInfo> presenceMap = presenceService.getHivesPresenceInfo(hiveIds);
        return ResponseEntity.ok(presenceMap);
    }
    
    @Operation(summary = "Get my active session", description = "Gets the current user's active focus session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No active session"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/sessions/me")
    public ResponseEntity<FocusSession> getMyActiveSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        FocusSession session = presenceService.getActiveFocusSession(getUserId(userDetails));
        return session != null 
                ? ResponseEntity.ok(session) 
                : ResponseEntity.notFound().build();
    }
    
    @Operation(summary = "Get hive sessions", description = "Gets all active focus sessions in a hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/hives/{hiveId}/sessions")
    public ResponseEntity<List<FocusSession>> getHiveSessions(
            @Parameter(description = "Hive ID") @PathVariable String hiveId) {
        List<FocusSession> sessions = presenceService.getHiveFocusSessions(hiveId);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * Helper method to extract user ID from UserDetails.
     */
    private String getUserId(UserDetails userDetails) {
        // TODO: Update based on actual UserDetails implementation
        return userDetails.getUsername();
    }
}