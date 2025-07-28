package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.service.TimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time timer synchronization across hive members.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TimerWebSocketController {
    
    private final TimerService timerService;
    
    /**
     * Start a shared timer for the hive.
     * All hive members will see the timer in real-time.
     */
    @MessageMapping("/hive/{hiveId}/timer/start")
    @SendTo("/topic/hive/{hiveId}/timer")
    public TimerStateDto startHiveTimer(
            @DestinationVariable String hiveId,
            @Payload TimerStateDto timerRequest,
            Principal principal) {
        
        log.debug("Starting hive timer for hive: {} by user: {}", hiveId, principal.getName());
        return timerService.startHiveTimer(hiveId, principal.getName(), timerRequest);
    }
    
    /**
     * Pause the shared hive timer.
     */
    @MessageMapping("/hive/{hiveId}/timer/pause")
    @SendTo("/topic/hive/{hiveId}/timer")
    public TimerStateDto pauseHiveTimer(
            @DestinationVariable String hiveId,
            Principal principal) {
        
        log.debug("Pausing hive timer for hive: {} by user: {}", hiveId, principal.getName());
        return timerService.pauseHiveTimer(hiveId, principal.getName());
    }
    
    /**
     * Resume the shared hive timer.
     */
    @MessageMapping("/hive/{hiveId}/timer/resume")
    @SendTo("/topic/hive/{hiveId}/timer")
    public TimerStateDto resumeHiveTimer(
            @DestinationVariable String hiveId,
            Principal principal) {
        
        log.debug("Resuming hive timer for hive: {} by user: {}", hiveId, principal.getName());
        return timerService.resumeHiveTimer(hiveId, principal.getName());
    }
    
    /**
     * Stop the shared hive timer.
     */
    @MessageMapping("/hive/{hiveId}/timer/stop")
    @SendTo("/topic/hive/{hiveId}/timer/stop")
    public TimerStateDto stopHiveTimer(
            @DestinationVariable String hiveId,
            Principal principal) {
        
        log.debug("Stopping hive timer for hive: {} by user: {}", hiveId, principal.getName());
        return timerService.stopHiveTimer(hiveId, principal.getName());
    }
    
    /**
     * Subscribe to timer updates when joining a hive.
     * Returns current timer state if one is active.
     */
    @SubscribeMapping("/topic/hive/{hiveId}/timer")
    public TimerStateDto subscribeToTimer(@DestinationVariable String hiveId) {
        log.debug("Client subscribing to timer updates for hive: {}", hiveId);
        return timerService.getHiveTimerState(hiveId);
    }
    
    /**
     * Notify when a user starts a personal focus session.
     * This is broadcast to all hive members for awareness.
     */
    @MessageMapping("/hive/{hiveId}/session/start")
    @SendTo("/topic/hive/{hiveId}/sessions")
    public SessionBroadcast broadcastSessionStart(
            @DestinationVariable String hiveId,
            @Payload StartSessionRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        log.debug("Broadcasting session start for user: {} in hive: {}", principal.getName(), hiveId);
        
        // Start the session
        FocusSessionDto session = timerService.startSession(principal.getName(), request);
        
        // Create broadcast message
        return SessionBroadcast.builder()
                .userId(principal.getName())
                .username((String) headerAccessor.getSessionAttributes().get("username"))
                .action("started")
                .sessionType(session.getSessionType().toString())
                .durationMinutes(session.getDurationMinutes())
                .sessionId(session.getId())
                .build();
    }
    
    /**
     * Notify when a user ends their focus session.
     */
    @MessageMapping("/hive/{hiveId}/session/end")
    @SendTo("/topic/hive/{hiveId}/sessions")
    public SessionBroadcast broadcastSessionEnd(
            @DestinationVariable String hiveId,
            @Payload EndSessionRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        
        log.debug("Broadcasting session end for user: {} in hive: {}", principal.getName(), hiveId);
        
        // End the session
        FocusSessionDto session = timerService.endSession(principal.getName(), request.getSessionId());
        
        // Create broadcast message
        return SessionBroadcast.builder()
                .userId(principal.getName())
                .username((String) headerAccessor.getSessionAttributes().get("username"))
                .action("ended")
                .sessionType(session.getSessionType().toString())
                .actualDurationMinutes(session.getActualDurationMinutes())
                .sessionId(session.getId())
                .completed(session.getCompleted())
                .build();
    }
    
    /**
     * Subscribe to session updates when joining a hive.
     * Shows who is currently in focus sessions.
     */
    @SubscribeMapping("/topic/hive/{hiveId}/sessions")
    public void subscribeToSessions(@DestinationVariable String hiveId) {
        log.debug("Client subscribing to session updates for hive: {}", hiveId);
        // Current active sessions could be returned here if needed
    }
}