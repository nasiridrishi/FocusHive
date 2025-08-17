package com.focushive.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Create a dedicated task scheduler for the message broker
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("websocket-heartbeat-");
        taskScheduler.initialize();
        
        // Enable a simple in-memory message broker with performance optimizations
        config.enableSimpleBroker(
            "/topic",  // Public broadcasts
            "/queue",  // Private messages
            "/user"    // User-specific messages
        )
        .setHeartbeatValue(new long[]{30000, 30000}) // 30 second heartbeat
        .setTaskScheduler(taskScheduler);
        
        // Prefix for messages FROM clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints with optimized SockJS settings
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
            .setTransportHandlers(/* Use default transports */)
            .setSessionCookieNeeded(false)
            .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
            .setHeartbeatTime(30000); // 30 seconds
        
        // Register raw WebSocket endpoint with better performance for modern browsers
        registry.addEndpoint("/ws-raw")
            .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport for better performance
        registration.setMessageSizeLimit(128 * 1024); // 128KB
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB
        registration.setSendTimeLimit(20 * 1000); // 20 seconds
        registration.setTimeToFirstMessage(30 * 1000); // 30 seconds
    }
}