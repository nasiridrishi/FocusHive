package com.focushive.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Stub implementation of WebSocket event handler when buddy features are disabled
 */
@Component
@ConditionalOnProperty(name = "app.features.buddy.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class WebSocketEventHandlerStub {
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = event.getUser() != null ? event.getUser().getName() : null;
        
        if (username != null) {
            log.info("WebSocket connected - Session: {}, User: {} (buddy features disabled)", sessionId, username);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket disconnected - Session: {} (buddy features disabled)", sessionId);
    }
    
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (destination != null) {
            log.debug("WebSocket subscription - Session: {}, Destination: {} (buddy features disabled)", sessionId, destination);
        }
    }
}