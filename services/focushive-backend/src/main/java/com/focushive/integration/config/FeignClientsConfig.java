package com.focushive.integration.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable Feign clients for external microservice communication.
 * This enables declarative REST clients for Buddy and Notification services.
 * Note: @EnableFeignClients is now in the main application class to avoid conflicts
 */
@Configuration
public class FeignClientsConfig {
    // Configuration is handled via application.yml
    // @EnableFeignClients moved to main application class to avoid multiple child contexts
}