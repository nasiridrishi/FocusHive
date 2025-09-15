package com.focushive.identity.integration.config;

import com.focushive.identity.integration.service.ServiceJwtTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign configuration for notification service client.
 * Handles authentication and other cross-cutting concerns for notification service communication.
 */
@Slf4j
@Configuration
public class NotificationServiceFeignConfig {

    @Value("${notification.service.api-key:}")
    private String notificationServiceApiKey;

    @Value("${notification.service.auth.enabled:true}")
    private boolean authEnabled;

    @Autowired(required = false)
    private ServiceJwtTokenProvider serviceJwtTokenProvider;

    // Cache the service token to avoid regenerating on every request
    private String cachedServiceToken;
    private long tokenCacheExpiry = 0;

    /**
     * Request interceptor to add authentication headers to notification service requests.
     * In production, this would use proper service-to-service authentication (OAuth2, JWT, etc.)
     */
    @Bean
    public RequestInterceptor notificationServiceRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Add correlation ID for tracing
                template.header("X-Correlation-ID", java.util.UUID.randomUUID().toString());

                // Add service identifier
                template.header("X-Source-Service", "identity-service");

                // Add authentication if enabled
                if (authEnabled) {
                    // Prefer API Key authentication for service-to-service calls
                    if (notificationServiceApiKey != null && !notificationServiceApiKey.isEmpty()) {
                        template.header("X-API-Key", notificationServiceApiKey);
                        log.debug("Added API key authentication to notification service request");
                    } else if (serviceJwtTokenProvider != null) {
                        // Fallback to JWT authentication if no API key configured
                        String serviceToken = getCachedOrGenerateToken();
                        template.header("Authorization", "Bearer " + serviceToken);
                        log.debug("Added JWT Bearer token authentication to notification service request");
                    } else {
                        log.warn("No authentication configured for notification service. " +
                                "Requests may fail with 401 Unauthorized.");
                    }
                }

                log.debug("Intercepted request to notification service: {} {}",
                    template.method(), template.url());
            }
        };
    }

    /**
     * Get cached token or generate a new one if expired.
     * This reduces the number of token generations and improves performance.
     */
    private synchronized String getCachedOrGenerateToken() {
        // Check if we have a valid cached token
        if (cachedServiceToken != null && System.currentTimeMillis() < tokenCacheExpiry) {
            return cachedServiceToken;
        }

        // Generate new token
        cachedServiceToken = serviceJwtTokenProvider.generateServiceToken();

        // Cache for 4 minutes (tokens are valid for 5 minutes by default)
        tokenCacheExpiry = System.currentTimeMillis() + (4 * 60 * 1000);

        log.info("Generated new service token for notification service, cached until {}",
                 new java.util.Date(tokenCacheExpiry));

        return cachedServiceToken;
    }

    /**
     * Configure Feign logging level.
     * Can be NONE, BASIC, HEADERS, or FULL
     */
    @Bean
    public feign.Logger.Level notificationServiceLogLevel() {
        return feign.Logger.Level.BASIC;
    }
}