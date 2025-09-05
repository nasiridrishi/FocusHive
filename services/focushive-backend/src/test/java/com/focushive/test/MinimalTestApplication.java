package com.focushive.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal test application that excludes all WebSocket functionality and Redis dependencies.
 * This configuration ensures no mapping conflicts occur during testing.
 */
@SpringBootApplication(
    exclude = {
        // External dependencies
        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
        // WebSocket autoconfigurations
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration.class,
        // Additional WebSocket and messaging exclusions
        org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration.class,
        org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration.class,
        // Security exclusions that might interfere
        org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = "com.focushive",
    excludeFilters = {
        // Exclude ALL WebSocket related packages and configurations
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.websocket\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*WebSocket.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Messaging.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*STOMP.*"
        ),
        // Exclude controllers that cause WebSocket mapping conflicts  
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.presence\\.controller\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.timer\\.controller\\..*WebSocket.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.chat\\.controller\\..*WebSocket.*"
        ),
        // Exclude Redis and cache configurations
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Redis.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Cache.*"
        ),
        // Exclude external service integrations
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.api\\.client\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.api\\.config\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.backend\\.config\\..*"
        ),
        // Exclude forum (if not needed for core tests)
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.forum\\..*"
        ),
        // Exclude specific configuration classes that might trigger WebSocket setup
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.websocket\\.config\\..*"
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.focushive\\.presence\\.config\\..*"
        )
    }
)
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = "com.focushive",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.focushive\\.forum\\.repository\\..*"
    )
)
@EntityScan(basePackages = "com.focushive")
public class MinimalTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinimalTestApplication.class, args);
    }
}