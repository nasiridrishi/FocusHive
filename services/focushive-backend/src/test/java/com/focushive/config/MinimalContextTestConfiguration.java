package com.focushive.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

/**
 * Minimal Spring Boot configuration for tests.
 * This configuration completely excludes WebSocket components and only includes essential beans.
 */
@SpringBootApplication(exclude = {
    WebSocketServletAutoConfiguration.class,
    WebSocketMessagingAutoConfiguration.class
})
@ComponentScan(
    basePackages = {"com.focushive.common", "com.focushive.config"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*WebSocket.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*websocket.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.chat.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.presence.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.timer.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.buddy.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.forum.*")
    }
)
@Profile("test")
public class MinimalContextTestConfiguration {
    // This class provides a minimal Spring Boot configuration for basic context loading tests
}