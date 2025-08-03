package com.focushive.api.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Feign client configuration for Identity Service communication.
 */
@Configuration
@Profile("!test") // Don't load this in test profile
public class FeignConfiguration {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add service-to-service authentication header if needed
            requestTemplate.header("X-Service-Name", "focushive-backend");
            requestTemplate.header("X-Service-Version", "1.0.0");
        };
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new IdentityServiceErrorDecoder();
    }
}