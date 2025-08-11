package com.focushive.presence.controller;

import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket controller for real-time presence management.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceController {
    
    private final PresenceService presenceService;
    
    @MessageMapping("/presence/update")
    @SendToUser("/queue/presence/confirm")
    public UserPresence updatePresence(@Payload PresenceUpdate update, Principal principal) {
        // Removed debug log to avoid logging user presence updates frequently
        return presenceService.updateUserPresence(principal.getName(), update);
    }
    
    @MessageMapping("/presence/heartbeat")
    public void heartbeat(Principal principal) {
        presenceService.recordHeartbeat(principal.getName());
    }
    
    @MessageMapping("/hive/{hiveId}/join")
    public HivePresenceInfo joinHive(@DestinationVariable String hiveId, Principal principal) {
        log.info("User {} joining hive {}", principal.getName(), hiveId);
        return presenceService.joinHivePresence(hiveId, principal.getName());
    }
    
    @MessageMapping("/hive/{hiveId}/leave")
    public HivePresenceInfo leaveHive(@DestinationVariable String hiveId, Principal principal) {
        log.info("User {} leaving hive {}", principal.getName(), hiveId);
        return presenceService.leaveHivePresence(hiveId, principal.getName());
    }
    
    @SubscribeMapping("/hive/{hiveId}/presence")
    public HivePresenceInfo subscribeToHivePresence(@DestinationVariable String hiveId) {
        // Removed debug log to avoid logging subscription events frequently
        return HivePresenceInfo.builder()
                .hiveId(hiveId)
                .activeUsers(presenceService.getHiveActiveUsers(hiveId).size())
                .onlineMembers(presenceService.getHiveActiveUsers(hiveId))
                .lastActivity(System.currentTimeMillis())
                .build();
    }
    
    @MessageMapping("/hive/{hiveId}/members")
    @SendToUser("/queue/hive/members")
    public List<UserPresence> getHiveMembers(@DestinationVariable String hiveId) {
        return presenceService.getHiveActiveUsers(hiveId);
    }
    
    @MessageMapping("/session/start")
    @SendToUser("/queue/session/confirm")
    public FocusSession startFocusSession(@Payload StartSessionRequest request, Principal principal) {
        log.info("User {} starting focus session", principal.getName());
        return presenceService.startFocusSession(
                principal.getName(),
                request.hiveId(),
                request.durationMinutes()
        );
    }
    
    @MessageMapping("/session/end")
    @SendToUser("/queue/session/confirm")
    public FocusSession endFocusSession(@Payload EndSessionRequest request, Principal principal) {
        log.info("User {} ending focus session {}", principal.getName(), request.sessionId());
        return presenceService.endFocusSession(principal.getName(), request.sessionId());
    }
    
    @MessageMapping("/session/current")
    @SendToUser("/queue/session/current")
    public FocusSession getCurrentSession(Principal principal) {
        return presenceService.getActiveFocusSession(principal.getName());
    }
    
    @MessageMapping("/hive/{hiveId}/sessions")
    @SendToUser("/queue/hive/sessions")
    public List<FocusSession> getHiveSessions(@DestinationVariable String hiveId) {
        return presenceService.getHiveFocusSessions(hiveId);
    }
    
    /**
     * Request DTOs
     */
    public record StartSessionRequest(
            String hiveId,
            int durationMinutes
    ) {}
    
    public record EndSessionRequest(
            String sessionId
    ) {}
}