package com.focushive.identity.config;

import com.focushive.identity.interceptor.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for rate limiting functionality.
 * Registers the rate limiting interceptor for web requests.
 * Redis configuration is handled separately in RateLimitingRedisConfig to avoid circular dependencies.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "focushive.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class RateLimitingConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/v1/auth/**")
                .addPathPatterns("/oauth2/**")
                .order(1); // Execute early in the interceptor chain

        log.info("Rate limiting interceptor registered for authentication endpoints");
    }
}