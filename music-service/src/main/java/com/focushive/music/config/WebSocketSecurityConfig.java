package com.focushive.music.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
 * WebSocket security configuration for music service.
 * 
 * Configures security for WebSocket endpoints and message destinations,
 * ensuring proper authentication and authorization for real-time features.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    /**
     * Configures message security for WebSocket destinations.
     * 
     * @param messages MessageSecurityMetadataSourceRegistry for configuration
     */
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Allow connection messages
            .simpTypeMatchers(
                org.springframework.messaging.simp.SimpMessageType.CONNECT,
                org.springframework.messaging.simp.SimpMessageType.CONNECT_ACK,
                org.springframework.messaging.simp.SimpMessageType.HEARTBEAT,
                org.springframework.messaging.simp.SimpMessageType.UNSUBSCRIBE,
                org.springframework.messaging.simp.SimpMessageType.DISCONNECT
            ).permitAll()
            
            // Music-related destinations require authentication
            .simpDestMatchers("/app/music/**").authenticated()
            .simpDestMatchers("/topic/hive/*/music/**").authenticated()
            .simpDestMatchers("/queue/user/*/music/**").authenticated()
            
            // Subscription destinations require authentication
            .simpSubscribeDestMatchers("/topic/hive/**").authenticated()
            .simpSubscribeDestMatchers("/queue/user/**").authenticated()
            
            // All other messages require authentication
            .anyMessage().authenticated();
    }

    /**
     * Disable CSRF for WebSocket connections as we use JWT authentication.
     * 
     * @return false to disable CSRF
     */
    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}