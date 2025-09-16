package com.focushive.api.config;

import com.focushive.api.security.JwtAuthenticationFilter;
import com.focushive.api.security.JwtCacheService;
import com.focushive.api.security.JwtTokenBlacklistService;
import com.focushive.api.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * JWT Security Configuration.
 *
 * This configuration class wires up JWT-related support services.
 * The JwtTokenProvider bean itself is defined in JwtConfiguration.java.
 *
 * Features:
 * - Conditional blacklist service activation (requires Redis)
 * - Conditional caching service activation (requires Redis)
 * - JWT Authentication Filter configuration
 * - Performance optimization through service availability detection
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.features.authentication.enabled", havingValue = "true")
public class JwtSecurityConfig {

    /**
     * JWT Token Blacklist Service.
     * Only created when Redis is available.
     */
    @Bean
    @ConditionalOnProperty(name = "app.features.redis.enabled", havingValue = "true")
    public JwtTokenBlacklistService jwtTokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        log.info("Configuring JWT Token Blacklist Service with Redis support");
        return new JwtTokenBlacklistService(redisTemplate);
    }

    /**
     * JWT Cache Service.
     * Only created when Redis is available.
     */
    @Bean
    @ConditionalOnProperty(name = "app.features.redis.enabled", havingValue = "true")
    public JwtCacheService jwtCacheService(RedisTemplate<String, String> redisTemplate) {
        log.info("Configuring JWT Cache Service with Redis support");
        return new JwtCacheService(redisTemplate);
    }

    /**
     * JWT Authentication Filter.
     * Always created when authentication is enabled.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        log.info("Configuring JWT Authentication Filter");
        return new JwtAuthenticationFilter(tokenProvider);
    }
}