package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Test WebSocket configuration that provides a minimal, working WebSocket setup.
 * This configuration overrides the main WebSocket configuration to prevent
 * "No handlers" errors during testing by providing at least one message handler.
 * Only activated when websocket testing is specifically enabled.
 */
@TestConfiguration
@Profile("test")
@EnableWebSocketMessageBroker
@ComponentScan("com.focushive.config")
@ConditionalOnProperty(name = "websocket.test.enabled", havingValue = "true", matchIfMissing = false)
public class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Minimal broker setup
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register endpoints matching production configuration
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Also register raw WebSocket endpoint for tests
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }

}