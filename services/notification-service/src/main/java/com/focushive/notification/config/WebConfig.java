package com.focushive.notification.config;

import com.focushive.notification.interceptor.EncryptionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering interceptors and other web components.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;
    private final EncryptionInterceptor encryptionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register encryption interceptor for sensitive data
        registry.addInterceptor(encryptionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/actuator/**", "/actuator/**")
                .order(0); // Execute first to handle encryption/decryption

        // Register rate limiting interceptor for all API endpoints
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**") // Don't rate limit actuator endpoints
                .order(1); // Execute after encryption interceptor
    }
}