package com.focushive.websocket.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Test configuration for WebSocket integration tests
 */
@TestConfiguration
@EnableWebSocketMessageBroker
public class WebSocketIntegrationTestConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Create a dedicated task scheduler for the message broker
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("websocket-test-heartbeat-");
        taskScheduler.initialize();
        
        // Enable a simple in-memory message broker
        config.enableSimpleBroker(
            "/topic",  // Public broadcasts
            "/queue",  // Private messages
            "/user"    // User-specific messages
        )
        .setHeartbeatValue(new long[]{10000, 10000}) // 10 second heartbeat
        .setTaskScheduler(taskScheduler);
        
        // Prefix for messages FROM clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints for testing
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setSessionCookieNeeded(false)
            .setHeartbeatTime(10000); // 10 seconds
        
        // Register raw WebSocket endpoint
        registry.addEndpoint("/ws-raw")
            .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport for testing
        registration.setMessageSizeLimit(64 * 1024); // 64KB
        registration.setSendBufferSizeLimit(256 * 1024); // 256KB
        registration.setSendTimeLimit(10 * 1000); // 10 seconds
        registration.setTimeToFirstMessage(15 * 1000); // 15 seconds
    }
    
    /**
     * Mock presence tracking service for testing
     */
    @Bean
    @Primary
    public MockPresenceTrackingService mockPresenceTrackingService() {
        return new MockPresenceTrackingService();
    }
}