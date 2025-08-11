package com.focushive.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages back to clients
        // Prefixes for messages TO clients
        config.enableSimpleBroker(
            "/topic",  // Public broadcasts
            "/queue",  // Private messages
            "/user"    // User-specific messages
        );
        
        // Prefix for messages FROM clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
        
        // Register raw WebSocket endpoint (without SockJS fallback)
        registry.addEndpoint("/ws-raw")
            .setAllowedOriginPatterns("*");
    }
}