package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.service.FocusTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time timer synchronization.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TimerWebSocketController {

    private final FocusTimerService timerService;

    /**
     * Start a timer session via WebSocket.
     */
    @MessageMapping("/timer/start")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse startTimer(
            @Payload StartTimerRequest request,
            Principal principal) {

        log.debug("Starting timer via WebSocket for user: {}", principal.getName());
        request.setUserId(principal.getName());
        return timerService.startTimer(request);
    }

    /**
     * Pause a timer session via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/pause")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse pauseTimer(
            @DestinationVariable String sessionId,
            Principal principal) {

        log.debug("Pausing timer {} via WebSocket", sessionId);
        return timerService.pauseTimer(sessionId, principal.getName());
    }

    /**
     * Resume a timer session via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/resume")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse resumeTimer(
            @DestinationVariable String sessionId,
            Principal principal) {

        log.debug("Resuming timer {} via WebSocket", sessionId);
        return timerService.resumeTimer(sessionId, principal.getName());
    }

    /**
     * Complete a timer session via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/complete")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse completeTimer(
            @DestinationVariable String sessionId,
            @Payload(required = false) Integer productivityScore,
            Principal principal) {

        log.debug("Completing timer {} via WebSocket", sessionId);
        return timerService.completeTimer(sessionId, principal.getName(), productivityScore);
    }

    /**
     * Cancel a timer session via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/cancel")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse cancelTimer(
            @DestinationVariable String sessionId,
            @Payload(required = false) String reason,
            Principal principal) {

        log.debug("Cancelling timer {} via WebSocket", sessionId);
        return timerService.cancelTimer(sessionId, principal.getName(),
            reason != null ? reason : "User cancelled");
    }

    /**
     * Update session metrics via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/metrics")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse updateMetrics(
            @DestinationVariable String sessionId,
            @Payload UpdateSessionMetricsRequest request,
            Principal principal) {

        log.debug("Updating metrics for timer {} via WebSocket", sessionId);
        return timerService.updateSessionMetrics(sessionId, principal.getName(), request);
    }

    /**
     * Add note to session via WebSocket.
     */
    @MessageMapping("/timer/{sessionId}/note")
    @SendTo("/topic/timer/updates")
    public FocusSessionResponse addNote(
            @DestinationVariable String sessionId,
            @Payload String note,
            Principal principal) {

        log.debug("Adding note to timer {} via WebSocket", sessionId);
        return timerService.addSessionNote(sessionId, principal.getName(), note);
    }

    /**
     * Subscribe to get current active session.
     */
    @SubscribeMapping("/topic/timer/{userId}")
    public FocusSessionResponse subscribeToUserTimer(@DestinationVariable String userId) {
        log.debug("User subscribing to timer updates: {}", userId);
        return timerService.getActiveSession(userId);
    }

    /**
     * Subscribe to hive timer updates.
     */
    @SubscribeMapping("/topic/hive/{hiveId}/timer")
    public void subscribeToHiveTimer(@DestinationVariable String hiveId) {
        log.debug("Subscribing to hive {} timer updates", hiveId);
        // The actual timer updates are broadcast by the service via broadcastTimerUpdate
    }
}