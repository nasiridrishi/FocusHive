package com.focushive.api.client;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for Identity Service communication.
 * Provides service-to-service authentication, retry logic, and comprehensive logging.
 */
@Slf4j
@Configuration
@Profile("!test") // Don't load this in test profile
@RequiredArgsConstructor
public class FeignConfiguration {
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Value("${app.version:1.0.0}")
    private String serviceVersion;
    
    @Value("${identity.service.api-key:#{null}}")
    private String serviceApiKey;
    
    /**
     * Request interceptor for service-to-service authentication and metadata.
     */
    @Bean
    public RequestInterceptor serviceAuthInterceptor() {
        return requestTemplate -> {
            // Add service identification headers
            requestTemplate.header("X-Service-Name", serviceName);
            requestTemplate.header("X-Service-Version", serviceVersion);
            
            // Add service-to-service API key if configured
            if (serviceApiKey != null && !serviceApiKey.trim().isEmpty()) {
                requestTemplate.header("X-API-Key", serviceApiKey);
            }
            
            // Propagate JWT token from current request context if available
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String token = jwt.getTokenValue();
                requestTemplate.header("Authorization", "Bearer " + token);
                log.debug("Propagating JWT token to Identity Service call");
            }
            
            // Add correlation ID for distributed tracing
            String correlationId = extractCorrelationId();
            if (correlationId != null) {
                requestTemplate.header("X-Correlation-ID", correlationId);
            }
            
            // Add request timestamp for debugging
            requestTemplate.header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));
            
            log.debug("Added service authentication headers for request to: {}", 
                     requestTemplate.feignTarget().url());
        };
    }
    
    /**
     * Custom error decoder for Identity Service responses.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new IdentityServiceErrorDecoder();
    }
    
    /**
     * Custom retry configuration for Identity Service calls.
     */
    @Bean
    public Retryer retryer() {
        // Retry up to 3 times with exponential backoff starting at 1 second
        return new Retryer.Default(1000L, TimeUnit.SECONDS.toMillis(3), 3);
    }
    
    /**
     * Request options configuration for timeouts.
     */
    @Bean
    public Request.Options requestOptions(
            @Value("${identity.service.connect-timeout:5000}") int connectTimeoutMs,
            @Value("${identity.service.read-timeout:10000}") int readTimeoutMs) {
        return new Request.Options(connectTimeoutMs, TimeUnit.MILLISECONDS, 
                                 readTimeoutMs, TimeUnit.MILLISECONDS, true);
    }
    
    /**
     * Feign logger level for debugging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    /**
     * Extract correlation ID from current request or generate a new one.
     * This supports distributed tracing across services.
     */
    private String extractCorrelationId() {
        // Try to get correlation ID from current request context
        // This would typically be set by a servlet filter
        try {
            return org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .getAttribute("correlationId", org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST)
                .toString();
        } catch (Exception e) {
            // Generate new correlation ID if not available
            return java.util.UUID.randomUUID().toString();
        }
    }
}