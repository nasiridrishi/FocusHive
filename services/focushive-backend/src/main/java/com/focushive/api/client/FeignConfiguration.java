package com.focushive.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.codec.Encoder;
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
public class FeignConfiguration {

    private final ObjectFactory<HttpMessageConverters> messageConverters;

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
            // CRITICAL: Add Content-Type headers to prevent 415 errors
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");

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
                if (log.isDebugEnabled()) {
                    log.debug("Propagating JWT token to Identity Service call");
                }
            }
            
            // Add correlation ID for distributed tracing
            String correlationId = extractCorrelationId();
            if (correlationId != null) {
                requestTemplate.header("X-Correlation-ID", correlationId);
            }
            
            // Add request timestamp for debugging
            requestTemplate.header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()));
            
            if (log.isDebugEnabled()) {
                log.debug("Added service authentication headers for request to: {}", 
                         requestTemplate.feignTarget().url());
            }
        };
    }
    
    /**
     * Custom error decoder for Identity Service responses.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new IdentityServiceErrorDecoder();
    }

    @Autowired
    public FeignConfiguration(ObjectFactory<HttpMessageConverters> messageConverters) {
        this.messageConverters = messageConverters;
    }

    /**
     * Provide the standard Spring decoder.
     * This bean is required for Feign clients to decode HTTP responses.
     */
    @Bean
    public Decoder decoder() {
        return new ResponseEntityDecoder(new SpringDecoder(messageConverters));
    }

    /**
     * Provide the standard Spring encoder.
     * This bean is required for Feign clients to encode HTTP requests.
     */
    @Bean
    public Encoder encoder() {
        return new SpringEncoder(messageConverters);
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
