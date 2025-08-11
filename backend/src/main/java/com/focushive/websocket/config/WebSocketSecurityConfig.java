package com.focushive.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Allow connection without authentication (auth will be handled in handshake)
            .simpDestMatchers("/app/**").authenticated()
            .simpSubscribeDestMatchers("/user/**", "/topic/**", "/queue/**").authenticated()
            // Allow CONNECT frames from anyone (authentication happens here)
            .simpTypeMatchers(org.springframework.messaging.simp.SimpMessageType.CONNECT).permitAll()
            // All other messages require authentication
            .anyMessage().authenticated();
    }

    @Override
    protected boolean sameOriginDisabled() {
        // Disable CSRF for WebSocket
        return true;
    }
}