package com.focushive.backend.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Feign clients and inter-service communication.
 * Note: @EnableFeignClients is now in the main application class to avoid conflicts
 */
@Configuration
public class FeignConfig {

    @Value("${identity.service.token:}")
    private String serviceToken;

    /**
     * Configure request interceptor to add service-to-service authentication token.
     */
    @Bean
    public RequestInterceptor serviceTokenInterceptor() {
        return requestTemplate -> {
            // Add service token for service-to-service authentication
            if (serviceToken != null && !serviceToken.isEmpty()) {
                // Only add if not already present (user token takes precedence)
                if (!requestTemplate.headers().containsKey("X-Service-Token")) {
                    requestTemplate.header("X-Service-Token", "Bearer " + serviceToken);
                }
            }
        };
    }

    /**
     * Configure Feign logging level for debugging.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Configure request timeouts.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                30, TimeUnit.SECONDS,  // readTimeout
                true                    // followRedirects
        );
    }

    /**
     * Configure retry mechanism.
     * Feign will retry failed requests according to this configuration.
     */
    @Bean
    public Retryer retryer() {
        // Retry with exponential backoff: initial 100ms, max 1 second, up to 3 attempts
        return new Retryer.Default(100, 1000, 3);
    }
}