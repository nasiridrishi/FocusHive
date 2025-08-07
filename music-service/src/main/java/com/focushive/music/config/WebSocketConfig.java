package com.focushive.music.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time music features.
 * 
 * Configures STOMP messaging for collaborative playlists, live music sync,
 * and real-time music event broadcasting within hives.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configures message broker for WebSocket communication.
     * 
     * @param registry MessageBrokerRegistry for configuration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for real-time music features
        registry.enableSimpleBroker(
            "/topic",  // General topics (e.g., /topic/hive/{hiveId}/music)
            "/queue"   // Personal queues (e.g., /queue/user/{userId}/music)
        );
        
        // Set application destination prefix for client messages
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for personal messages
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configures STOMP endpoints for WebSocket connections.
     * 
     * @param registry StompEndpointRegistry for endpoint configuration
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary WebSocket endpoint for music features
        registry.addEndpoint("/ws/music")
            .setAllowedOrigins(allowedOrigins.split(","))
            .withSockJS(); // Enable SockJS fallback for older browsers

        // Health check endpoint for WebSocket connectivity
        registry.addEndpoint("/ws/health")
            .setAllowedOrigins(allowedOrigins.split(","))
            .withSockJS();
    }
}