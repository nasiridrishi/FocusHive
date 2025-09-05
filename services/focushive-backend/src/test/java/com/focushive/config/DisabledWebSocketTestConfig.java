package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;

/**
 * Test configuration that completely disables WebSocket functionality.
 * Use this configuration for tests that don't need WebSocket features.
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    WebSocketServletAutoConfiguration.class,
    WebSocketMessagingAutoConfiguration.class
})
public class DisabledWebSocketTestConfig {
    // This configuration explicitly disables all WebSocket auto-configuration
}