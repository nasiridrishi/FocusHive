package com.focushive.notification.config;

import com.focushive.notification.monitoring.CorrelationIdService;
import com.focushive.notification.monitoring.NotificationMetricsService;
import com.focushive.notification.repository.DeadLetterMessageRepository;
import com.focushive.notification.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Test configuration specifically for WebMvcTest sliced tests.
 * Provides minimal beans needed for web layer testing.
 */
@TestConfiguration
public class WebMvcTestConfig {

    @Bean
    @Primary
    public RabbitTemplate mockRabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public MeterRegistry testMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    @Qualifier("fromEmailAddress")
    public String fromEmailAddress() {
        return "no-reply@test.local";
    }

    @Bean
    @Primary
    public NotificationDigestService mockNotificationDigestService() {
        return Mockito.mock(NotificationDigestService.class);
    }

    @Bean
    @Primary
    public JwtDecoder mockJwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    @Bean
    @Primary
    public SecurityAuditService mockSecurityAuditService() {
        return Mockito.mock(SecurityAuditService.class);
    }

    @Bean
    @Primary
    public UserContextService mockUserContextService() {
        return Mockito.mock(UserContextService.class);
    }

    @Bean
    @Primary
    public RateLimitingService mockRateLimitingService() {
        return Mockito.mock(RateLimitingService.class);
    }

    @Bean
    @Primary
    public CorrelationIdService mockCorrelationIdService() {
        return Mockito.mock(CorrelationIdService.class);
    }

    @Bean
    @Primary
    public NotificationMetricsService mockNotificationMetricsService() {
        return Mockito.mock(NotificationMetricsService.class);
    }

    @Bean
    @Primary
    public SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();

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
    public TokenBlacklistService mockTokenBlacklistService() {
        return Mockito.mock(TokenBlacklistService.class);
    }

    @Bean
    @Primary
    public EmailMetricsService mockEmailMetricsService() {
        return new EmailMetricsService(testMeterRegistry());
    }

    @Bean
    @Primary
    public AsyncEmailService mockAsyncEmailService() {
        return Mockito.mock(AsyncEmailService.class);
    }

    @Bean
    @Primary
    public DeadLetterQueueService mockDeadLetterQueueService() {
        return Mockito.mock(DeadLetterQueueService.class);
    }

    @Bean
    @Primary
    public DeadLetterMessageRepository mockDeadLetterMessageRepository() {
        return Mockito.mock(DeadLetterMessageRepository.class);
    }

    @Bean
    @Primary
    public org.thymeleaf.TemplateEngine mockTemplateEngine() {
        return Mockito.mock(org.thymeleaf.TemplateEngine.class);
    }
}