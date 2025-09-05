package com.focushive.config;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.client.IdentityServiceFallback;
import com.focushive.api.health.CircuitBreakerHealthIndicator;
import com.focushive.api.health.IdentityServiceHealthIndicator;
import com.focushive.api.security.IdentityServiceAuthenticationFilter;
import com.focushive.api.security.JwtTokenProvider;
import com.focushive.api.service.CustomUserDetailsService;
import com.focushive.backend.service.IdentityIntegrationService;
import com.focushive.notification.service.delivery.NotificationDeliveryService;
import com.focushive.websocket.controller.BuddyWebSocketController;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Comprehensive test mock configuration for the FocusHive backend service.
 * This configuration provides @Primary mock beans for ALL external dependencies
 * to prevent test failures and bean conflicts.
 * 
 * Active only for test profile to ensure it doesn't interfere with production.
 * 
 * External dependencies mocked:
 * - Redis (RedisConnectionFactory, RedisTemplate, StringRedisTemplate)
 * - Feign clients (IdentityServiceClient and fallbacks) 
 * - WebSocket messaging (SimpMessagingTemplate)
 * - Cache management (CacheManager - using in-memory instead of Redis)
 * - Circuit breaker components (CircuitBreakerRegistry)
 * - Security components (authentication, JWT, user details)
 * - REST template components
 * - Health indicators
 * - Notification delivery services
 * - Event publishing (ApplicationEventPublisher)
 */
@TestConfiguration
@Profile("test")
public class TestMockConfig {

    // ================================
    // REDIS DEPENDENCIES
    // ================================
    
    /**
     * Mock Redis connection factory to replace actual Redis connections in tests.
     * Prevents Redis connectivity issues during testing.
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    /**
     * Mock Redis template for general object caching.
     * Prevents Redis operations from being executed during tests.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    /**
     * Mock String Redis template for string-based operations.
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    /**
     * Named Redis template for presence-specific operations.
     * Used by presence services for user status tracking.
     * Not marked as @Primary to avoid conflicts with the main redisTemplate.
     */
    @Bean(name = "presenceRedisTemplate")
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> presenceRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    // ================================
    // CACHE MANAGEMENT
    // ================================
    
    /**
     * In-memory cache manager to replace Redis-based caching.
     * Provides actual caching functionality for tests without external dependencies.
     * Includes all cache names used throughout the application.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            // Presence service caches
            "userPresence",
            "activeFocusSession", 
            "hiveActiveUsers",
            "hiveFocusSessions",
            "hiveMembership",
            "hiveUserIds",
            
            // Analytics caches
            "dailySummaries",
            "dailySummaryRanges",
            
            // Identity service caches
            "identity-user",
            "identity",
            "identity-email",
            "personas",
            "persona",
            "active-persona",
            "current-user",
            
            // General application caches
            "userProfiles",
            "hiveDetails",
            "userSettings",
            "systemSettings",
            "notificationTemplates"
        );
    }

    // ================================
    // FEIGN CLIENTS AND IDENTITY INTEGRATION
    // ================================
    
    /**
     * Mock Identity Service client (main API client).
     * Primary external service integration for user authentication and management.
     */
    @Bean(name = "identityServiceClient")
    @Primary
    public IdentityServiceClient identityServiceClient() {
        return mock(IdentityServiceClient.class);
    }

    /**
     * Mock Identity Service fallback for circuit breaker scenarios.
     * Not marked as @Primary to avoid conflicts with main IdentityServiceClient.
     */
    @Bean
    public IdentityServiceFallback identityServiceFallback() {
        return mock(IdentityServiceFallback.class);
    }

    /**
     * Mock Identity Integration Service (backend client).
     * Alternative client interface for identity operations.
     */
    @Bean(name = "identityIntegrationService")
    @Primary
    public IdentityIntegrationService identityIntegrationService() {
        return mock(IdentityIntegrationService.class);
    }

    // ================================
    // WEBSOCKET AND MESSAGING
    // ================================
    
    /**
     * Mock STOMP messaging template for WebSocket communications.
     * Used for real-time features like presence updates, chat, timers.
     */
    @Bean
    @Primary
    public SimpMessagingTemplate simpMessagingTemplate() {
        return mock(SimpMessagingTemplate.class);
    }

    /**
     * Mock buddy WebSocket controller for buddy system features.
     */
    @Bean
    @Primary
    public BuddyWebSocketController buddyWebSocketController() {
        return mock(BuddyWebSocketController.class);
    }

    // ================================
    // CIRCUIT BREAKER AND RESILIENCE
    // ================================
    
    /**
     * Mock circuit breaker registry for resilience patterns.
     * Used by Resilience4j for fault tolerance.
     */
    @Bean
    @Primary
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return mock(CircuitBreakerRegistry.class);
    }

    // ================================
    // SECURITY COMPONENTS
    // ================================
    
    /**
     * Mock Identity Service authentication filter for security.
     * Handles JWT token validation and user authentication.
     */
    @Bean(name = "identityServiceAuthenticationFilter")
    @Primary
    public IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter() {
        return mock(IdentityServiceAuthenticationFilter.class);
    }

    /**
     * Test JWT token provider with predictable configuration.
     * Uses test-specific secret key and settings.
     */
    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider("test-secret-key-for-testing-purposes-only", 3600000L);
    }

    /**
     * Mock custom user details service for authentication.
     */
    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        return mock(CustomUserDetailsService.class);
    }

    /**
     * In-memory user details service for test authentication.
     * Provides test users without external dependencies.
     */
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        UserDetails user = User.builder()
                .username("testuser")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();
                
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("password"))
                .roles("USER", "ADMIN")
                .build();
                
        return new InMemoryUserDetailsManager(user, admin);
    }

    /**
     * BCrypt password encoder for test authentication.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Test authentication manager configuration.
     */
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // ================================
    // HTTP CLIENT COMPONENTS
    // ================================
    
    /**
     * Mock REST template builder with configured defaults.
     * Returns a functional RestTemplate when build() is called.
     */
    @Bean
    @Primary
    public RestTemplateBuilder restTemplateBuilder() {
        RestTemplateBuilder mock = mock(RestTemplateBuilder.class, RETURNS_SELF);
        when(mock.build()).thenReturn(new RestTemplate());
        return mock;
    }

    /**
     * Standard REST template for HTTP operations.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // ================================
    // CORS CONFIGURATION
    // ================================
    
    /**
     * CORS configuration for test environment.
     * Allows all origins and methods for testing flexibility.
     */
    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ================================
    // HEALTH INDICATORS
    // ================================
    
    /**
     * Mock Identity Service health indicator.
     * Prevents external health checks during testing.
     */
    @Bean
    @Primary
    public IdentityServiceHealthIndicator identityServiceHealthIndicator() {
        return mock(IdentityServiceHealthIndicator.class);
    }

    /**
     * Mock circuit breaker health indicator.
     * Prevents circuit breaker state checks during testing.
     */
    @Bean
    @Primary
    public CircuitBreakerHealthIndicator circuitBreakerHealthIndicator() {
        return mock(CircuitBreakerHealthIndicator.class);
    }

    // ================================
    // NOTIFICATION SERVICES
    // ================================
    
    /**
     * Mock notification delivery service to prevent actual notifications.
     * Used by notification system for email/push notifications.
     */
    @Bean
    @Primary
    public NotificationDeliveryService notificationDeliveryService() {
        return mock(NotificationDeliveryService.class);
    }

    // ================================
    // EVENT PUBLISHING
    // ================================
    
    /**
     * Mock application event publisher to prevent actual event publishing.
     * Used for domain events and cross-service communication.
     */
    @Bean
    @Primary
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    // ================================
    // ALTERNATIVE IDENTITY CLIENT (BACKEND PACKAGE)
    // ================================
    
    /**
     * Mock backend identity service client (alternative interface).
     * Some services use this instead of the main API client.
     * Not marked as @Primary to avoid conflicts with main IdentityServiceClient.
     */
    @Bean(name = "backendIdentityServiceClient")
    public com.focushive.backend.client.IdentityServiceClient backendIdentityServiceClient() {
        return mock(com.focushive.backend.client.IdentityServiceClient.class);
    }

    /**
     * Mock backend identity service fallback.
     * Not marked as @Primary to avoid conflicts.
     */
    @Bean
    public com.focushive.backend.client.fallback.IdentityServiceFallback backendIdentityServiceFallback() {
        return mock(com.focushive.backend.client.fallback.IdentityServiceFallback.class);
    }
}