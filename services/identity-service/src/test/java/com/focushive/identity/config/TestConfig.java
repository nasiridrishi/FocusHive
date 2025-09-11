package com.focushive.identity.config;

import com.focushive.identity.service.EmailService;
import com.focushive.identity.service.TokenBlacklistService;
import io.micrometer.observation.ObservationRegistry;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Test configuration for Identity Service tests.
 * Provides mock implementations for external services and Redis.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {
    
    /**
     * Mock EmailService for tests.
     */
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }
    
    /**
     * Mock TokenBlacklistService for tests.
     */
    @Bean
    @Primary
    public TokenBlacklistService mockTokenBlacklistService() {
        TokenBlacklistService mockService = Mockito.mock(TokenBlacklistService.class);
        // By default, no tokens are blacklisted
        Mockito.when(mockService.isBlacklisted(Mockito.anyString())).thenReturn(false);
        return mockService;
    }
    
    /**
     * No-op ObservationRegistry for tests to avoid tracing issues.
     */
    @Bean
    @Primary
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }
    
    // Redis beans are now handled by using embedded Redis or H2 in tests
    // No need to mock RedisTemplates - they will use test configurations
    
}