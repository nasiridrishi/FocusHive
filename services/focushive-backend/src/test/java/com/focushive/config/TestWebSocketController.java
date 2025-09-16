package com.focushive.config;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Test WebSocket controller that provides message handlers for testing.
 * This controller is only active in test profile when websocket.test.enabled=true.
 */
@Controller
@Profile("test")
public class TestWebSocketController {

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public String handleTestMessage(String message) {
        return "Test: " + message;
    }

    @MessageMapping("/echo")
    @SendTo("/topic/echo")
    public String handleEchoMessage(String message) {
        return "Echo: " + message;
    }

    @MessageMapping("/app/test")
    @SendTo("/topic/test")
    public String handleAppTestMessage(String message) {
        return "App Test: " + message;
    }
}