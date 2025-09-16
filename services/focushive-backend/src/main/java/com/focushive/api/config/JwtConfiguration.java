package com.focushive.api.config;

import com.focushive.api.security.JwtTokenProvider;
import com.focushive.api.security.JwtTokenBlacklistService;
import com.focushive.api.security.JwtCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Optional;

/**
 * Unified JWT Configuration.
 *
 * This is the SINGLE source of truth for JWT Token Provider bean configuration.
 * It handles both Redis-enabled and Redis-disabled scenarios with proper conditional logic.
 */
@Slf4j
@Configuration
public class JwtConfiguration {

    @Value("${spring.security.jwt.secret:dGhpcyBpcyBhIHNlY3VyZSBzZWNyZXQga2V5IGZvciBkZXZlbG9wbWVudCBvbmx5IQ==}")
    private String jwtSecret;

    @Value("${spring.security.jwt.expiration:86400000}") // 24 hours default
    private Long jwtExpiration;

    /**
     * Single JWT Token Provider bean definition.
     *
     * This is the ONLY JWT Token Provider bean in the entire application.
     * It automatically adapts based on available services:
     * - When Redis is available: Uses blacklist and cache services
     * - When Redis is not available: Works without these services
     *
     * The Optional wrappers ensure graceful degradation when services are not available.
     */
    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider(
            Optional<JwtTokenBlacklistService> blacklistService,
            Optional<JwtCacheService> cacheService) {

        if (blacklistService.isPresent() && cacheService.isPresent()) {
            log.info("Configuring JWT Token Provider with full Redis support (blacklist and cache)");
        } else if (blacklistService.isPresent()) {
            log.info("Configuring JWT Token Provider with blacklist support only");
        } else if (cacheService.isPresent()) {
            log.info("Configuring JWT Token Provider with cache support only");
        } else {
            log.info("Configuring basic JWT Token Provider (no Redis services)");
        }

        return new JwtTokenProvider(jwtSecret, jwtExpiration, blacklistService, cacheService);
    }
}