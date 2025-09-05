package com.focushive.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal test application for basic Spring Boot context tests.
 * This is the simplest possible Spring Boot application for testing.
 */
@SpringBootApplication(
    exclude = {
        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration.class,
        org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration.class
    }
)
public class MinimalTestApp {
    public static void main(String[] args) {
        SpringApplication.run(MinimalTestApp.class, args);
    }
}