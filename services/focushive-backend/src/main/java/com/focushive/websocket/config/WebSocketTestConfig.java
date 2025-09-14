package com.focushive.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * WebSocket configuration for test environment
 * This configuration disables WebSocket message broker during testing
 * to avoid the "No handlers" error while still allowing unit tests
 */
@Configuration
@Profile("test")
public class WebSocketTestConfig {
    // This configuration class exists primarily to ensure test profile
    // has a WebSocket configuration that doesn't enable the message broker
}