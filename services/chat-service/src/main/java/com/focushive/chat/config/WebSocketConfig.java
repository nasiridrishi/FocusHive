package com.focushive.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Value("${chat.websocket.allowed-origins}")
    private String[] allowedOrigins;
    
    @Value("${chat.websocket.endpoint}")
    private String endpoint;
    
    @Value("${chat.websocket.message-broker-prefix}")
    private String messageBrokerPrefix;
    
    @Value("${chat.websocket.app-destination-prefix}")
    private String appDestinationPrefix;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topics (for sending messages to clients)
        config.enableSimpleBroker(messageBrokerPrefix);
        
        // Set prefix for app destinations (messages from clients)
        config.setApplicationDestinationPrefixes(appDestinationPrefix);
        
        // Enable user-specific destinations
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint for WebSocket connections
        registry.addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // Enable SockJS fallback
    }
}