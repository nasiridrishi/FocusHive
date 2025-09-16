package com.focushive.notification.config;

import com.focushive.notification.controller.NotificationController;
import com.focushive.notification.controller.NotificationPreferenceController;
import com.focushive.notification.monitoring.CorrelationIdService;
import com.focushive.notification.security.ApiSignatureService;
import com.focushive.notification.security.DataEncryptionService;
import com.focushive.notification.service.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test configuration specifically for controller tests.
 * Provides all necessary mocks for controller testing without database dependencies.
 */
@TestConfiguration
@ComponentScan(
    basePackageClasses = {
        NotificationController.class,
        NotificationPreferenceController.class
    },
    useDefaultFilters = false,
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            NotificationController.class,
            NotificationPreferenceController.class
        })
    }
)
public class ControllerTestConfiguration {

    @Bean
    @Primary
    public NotificationService notificationService() {
        return mock(NotificationService.class);
    }

    @Bean
    @Primary
    public NotificationPreferenceService notificationPreferenceService() {
        return mock(NotificationPreferenceService.class);
    }

    @Bean
    @Primary
    public NotificationDigestService notificationDigestService() {
        return mock(NotificationDigestService.class);
    }

    @Bean
    @Primary
    public NotificationTemplateService notificationTemplateService() {
        return mock(NotificationTemplateService.class);
    }

    @Bean
    @Primary
    public NotificationCleanupService notificationCleanupService() {
        return mock(NotificationCleanupService.class);
    }

    @Bean
    @Primary
    public DeadLetterQueueService deadLetterQueueService() {
        return mock(DeadLetterQueueService.class);
    }

    @Bean
    @Primary
    public EmailNotificationService emailNotificationService() {
        return mock(EmailNotificationService.class);
    }

    @Bean
    @Primary
    public AsyncEmailService asyncEmailService() {
        return mock(AsyncEmailService.class);
    }

    @Bean
    @Primary
    public UserContextService userContextService() {
        return mock(UserContextService.class);
    }

    @Bean
    @Primary
    public SecurityAuditService securityAuditService() {
        return mock(SecurityAuditService.class);
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
    public CorrelationIdService correlationIdService() {
        return mock(CorrelationIdService.class);
    }

    @Bean
    @Primary
    public EmailMetricsService emailMetricsService() {
        return new EmailMetricsService(meterRegistry());
    }

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @Primary
    public DataEncryptionService dataEncryptionService() {
        return mock(DataEncryptionService.class);
    }

    @Bean
    @Primary
    public ApiSignatureService apiSignatureService() {
        return mock(ApiSignatureService.class);
    }

    @Bean
    @Primary
    public com.focushive.notification.monitoring.NotificationMetricsService notificationMetricsService() {
        return mock(com.focushive.notification.monitoring.NotificationMetricsService.class);
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
}