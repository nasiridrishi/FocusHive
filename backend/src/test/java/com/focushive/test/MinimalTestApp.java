package com.focushive.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for testing with absolutely no conflicting configurations
 */
@SpringBootApplication(
    exclude = {
        // Exclude all auto-configurations that might cause conflicts
        org.springframework.cloud.openfeign.FeignAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.class
    }
)
public class MinimalTestApp {
    public static void main(String[] args) {
        SpringApplication.run(MinimalTestApp.class, args);
    }
}