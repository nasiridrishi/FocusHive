package com.focushive.notification.config;

import com.focushive.notification.repository.DeadLetterMessageRepository;
import com.focushive.notification.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.mockito.Mockito.mock;

/**
 * Test configuration providing mock beans for all new services.
 * Ensures tests have all required dependencies without starting real services.
 */
@TestConfiguration
public class TestBeanConfig {

    @Bean
    @Primary
    public SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();

        // Set default test values
        SecurityProperties.AuthenticationConfig authConfig = new SecurityProperties.AuthenticationConfig();
        authConfig.setMaxFailedAttempts(5);
        authConfig.setLockoutDuration(java.time.Duration.ofMinutes(15));
        authConfig.setFailedAttemptsWindow(java.time.Duration.ofMinutes(5));
        authConfig.setTrackByIp(true);
        authConfig.setAutoUnlock(true);

        SecurityProperties.RateLimitingConfig rateLimitingConfig = new SecurityProperties.RateLimitingConfig();
        rateLimitingConfig.setEnabled(true);
        rateLimitingConfig.setDefaultRequestsPerMinute(60);

        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setBlacklistingEnabled(false);
        jwtConfig.setValidateIssuer(false);
        jwtConfig.setValidateAudience(false);
        jwtConfig.setBlacklistTtl(java.time.Duration.ofHours(24));

        SecurityProperties.HeadersConfig headersConfig = new SecurityProperties.HeadersConfig();
        headersConfig.setEnabled(true);

        SecurityProperties.AuditConfig auditConfig = new SecurityProperties.AuditConfig();
        auditConfig.setEnabled(true);

        SecurityProperties.CorsConfig corsConfig = new SecurityProperties.CorsConfig();
        corsConfig.setEnabled(true);
        corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:3000"));

        properties.setAuthentication(authConfig);
        properties.setRateLimiting(rateLimitingConfig);
        properties.setJwt(jwtConfig);
        properties.setHeaders(headersConfig);
        properties.setAudit(auditConfig);
        properties.setCors(corsConfig);

        return properties;
    }

    @Bean
    @Primary
    public RateLimitingService rateLimitingService() {
        return mock(RateLimitingService.class);
    }

    @Bean
    @Primary
    public TokenBlacklistService tokenBlacklistService() {
        return mock(TokenBlacklistService.class);
    }

    @Bean
    @Primary
    public EmailMetricsService emailMetricsService() {
        // Use real implementation with simple meter registry for tests
        return new EmailMetricsService(new SimpleMeterRegistry());
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public AsyncEmailService asyncEmailService() {
        return mock(AsyncEmailService.class);
    }

    @Bean
    @Primary
    public DeadLetterQueueService deadLetterQueueService() {
        return mock(DeadLetterQueueService.class);
    }

    @Bean
    @Primary
    public DeadLetterMessageRepository deadLetterMessageRepository() {
        return mock(DeadLetterMessageRepository.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    @Primary
    public TemplateEngine templateEngine() {
        return new SpringTemplateEngine();
    }
}