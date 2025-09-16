package com.focushive.test.config;

import com.focushive.api.security.JwtTokenBlacklistService;
import com.focushive.api.security.JwtTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mock security beans for @WebMvcTest tests.
 * This prevents NoSuchBeanDefinitionException for JWT-related beans.
 * Uses @ConditionalOnMissingBean to avoid conflicts with real beans in integration tests.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    /**
     * Provides a mock JwtTokenProvider only when no real bean exists.
     * This is primarily for @WebMvcTest slice tests that don't load the full context.
     */
    @Bean
    @ConditionalOnMissingBean(JwtTokenProvider.class)
    public JwtTokenProvider mockJwtTokenProvider() {
        return mock(JwtTokenProvider.class);
    }

    /**
     * Provides a mock JwtTokenBlacklistService only when no real bean exists.
     * This is primarily for @WebMvcTest slice tests that don't load the full context.
     */
    @Bean
    @ConditionalOnMissingBean(JwtTokenBlacklistService.class)
    public JwtTokenBlacklistService mockJwtTokenBlacklistService() {
        return mock(JwtTokenBlacklistService.class);
    }
}