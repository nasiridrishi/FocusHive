package com.focushive.integration.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable Feign clients for external microservice communication.
 * This enables declarative REST clients for Buddy and Notification services.
 */
@Configuration
@EnableFeignClients(basePackages = "com.focushive.integration.client")
public class FeignClientsConfig {
    // Configuration is handled via application.yml
    // This class enables Feign client scanning for the specified package
}