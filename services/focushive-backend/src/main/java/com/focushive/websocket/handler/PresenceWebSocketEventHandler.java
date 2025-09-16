package com.focushive.websocket.handler;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.service.PresenceTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presence-specific WebSocket event handler.
 * Manages presence lifecycle, connection tracking, and subscription management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceWebSocketEventHandler {

    private final PresenceTrackingService presenceTrackingService;
    private final ConnectionMetrics connectionMetrics = new ConnectionMetrics();

    /**
     * Handle WebSocket connection event
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal user = event.getUser();

            if (user != null && sessionId != null) {
                Long userId = extractUserId(user);
                Long hiveId = extractHiveId(headerAccessor);
                Boolean isReconnect = extractReconnectFlag(headerAccessor);

                log.info("WebSocket connected - User: {}, Session: {}, Hive: {}, Reconnect: {}",
                    userId, sessionId, hiveId, isReconnect);

                if (Boolean.TRUE.equals(isReconnect)) {
                    // Handle reconnection with state recovery
                    presenceTrackingService.recoverPresenceState(userId, sessionId);
                } else {
                    // Handle new connection
                    presenceTrackingService.handleUserConnection(userId, sessionId, hiveId);
                    presenceTrackingService.updateUserPresence(
                        userId,
                        PresenceUpdate.PresenceStatus.ONLINE,
                        hiveId,
                        null
                    );
                }

                connectionMetrics.trackConnection(sessionId);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket connection", e);
        }
    }

    /**
     * Handle WebSocket disconnection event
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal user = event.getUser();

            if (user != null && sessionId != null) {
                Long userId = extractUserId(user);
                Boolean cleanupRequired = extractCleanupFlag(headerAccessor);

                log.info("WebSocket disconnected - User: {}, Session: {}, Cleanup: {}",
                    userId, sessionId, cleanupRequired);

                presenceTrackingService.handleUserDisconnection(userId, sessionId);
                presenceTrackingService.removeUserPresence(userId);

                if (Boolean.TRUE.equals(cleanupRequired)) {
                    presenceTrackingService.cleanupUserSubscriptions(userId);
                }

                connectionMetrics.trackDisconnection(sessionId);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnection", e);
        }
    }

    /**
     * Handle subscription event
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String destination = headerAccessor.getDestination();
            Principal user = event.getUser();

            if (user != null && destination != null) {
                Long userId = extractUserId(user);

                log.debug("User {} (session: {}) subscribed to {}", userId, sessionId, destination);

                // Handle hive presence subscriptions
                if (destination.matches("/topic/hive/\\d+/presence")) {
                    Long hiveId = extractHiveIdFromDestination(destination);
                    if (hiveId != null) {
                        presenceTrackingService.subscribeToHivePresence(userId, Set.of(hiveId));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling subscription event", e);
        }
    }

    /**
     * Handle unsubscription event
     */
    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String destination = headerAccessor.getDestination();
            Principal user = event.getUser();

            if (user != null && destination != null) {
                Long userId = extractUserId(user);

                log.debug("User {} (session: {}) unsubscribed from {}", userId, sessionId, destination);

                // Handle hive presence unsubscriptions
                if (destination.matches("/topic/hive/\\d+/presence")) {
                    Long hiveId = extractHiveIdFromDestination(destination);
                    if (hiveId != null) {
                        presenceTrackingService.unsubscribeFromHivePresence(userId, hiveId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling unsubscription event", e);
        }
    }

    /**
     * Get connection metrics
     */
    public ConnectionMetrics getConnectionMetrics() {
        return connectionMetrics;
    }

    // Helper methods

    private Long extractUserId(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            // Try to parse as Long first
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            // If not a number, try to extract from username format
            // This is a fallback for testing or different authentication schemes
            log.warn("Could not parse user ID as Long: {}, using hash", principal.getName());
            return (long) principal.getName().hashCode();
        }
    }

    private Long extractHiveId(StompHeaderAccessor headerAccessor) {
        Object hiveId = headerAccessor.getHeader("hiveId");
        if (hiveId != null) {
            try {
                return Long.parseLong(hiveId.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid hive ID format: {}", hiveId);
            }
        }
        return null;
    }

    private Boolean extractReconnectFlag(StompHeaderAccessor headerAccessor) {
        Object reconnect = headerAccessor.getHeader("reconnect");
        return reconnect != null ? Boolean.parseBoolean(reconnect.toString()) : null;
    }

    private Boolean extractCleanupFlag(StompHeaderAccessor headerAccessor) {
        Object cleanup = headerAccessor.getHeader("cleanupRequired");
        return cleanup != null ? Boolean.parseBoolean(cleanup.toString()) : null;
    }

    private Long extractHiveIdFromDestination(String destination) {
        if (destination == null) {
            return null;
        }
        String[] parts = destination.split("/");
        if (parts.length >= 4 && "hive".equals(parts[2])) {
            try {
                return Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("Invalid hive ID in destination: {}", destination);
            }
        }
        return null;
    }

    /**
     * Inner class for tracking connection metrics
     */
    public static class ConnectionMetrics {
        private int totalConnections = 0;
        private int activeConnections = 0;
        private final Map<String, Long> connectionTimestamps = new ConcurrentHashMap<>();

        public void trackConnection(String sessionId) {
            if (sessionId != null) {
                totalConnections++;
                activeConnections++;
                connectionTimestamps.put(sessionId, System.currentTimeMillis());
            }
        }

        public void trackDisconnection(String sessionId) {
            if (sessionId != null) {
                activeConnections = Math.max(0, activeConnections - 1);
                connectionTimestamps.remove(sessionId);
            }
        }

        public int getTotalConnections() {
            return totalConnections;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public Long getConnectionTimestamp(String sessionId) {
            return connectionTimestamps.get(sessionId);
        }

        public Map<String, Long> getAllConnectionTimestamps() {
            return new ConcurrentHashMap<>(connectionTimestamps);
        }
    }
}