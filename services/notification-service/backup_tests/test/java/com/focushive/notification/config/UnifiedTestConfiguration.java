package com.focushive.notification.config;
import com.focushive.notification.repository.DeadLetterMessageRepository;
import com.focushive.notification.repository.NotificationPreferenceRepository;
import com.focushive.notification.repository.NotificationRepository;
import com.focushive.notification.repository.NotificationTemplateRepository;
import com.focushive.notification.service.AsyncEmailService;
import com.focushive.notification.service.DeadLetterQueueService;
import com.focushive.notification.service.EmailMetricsService;
import com.focushive.notification.service.EmailNotificationService;
import com.focushive.notification.service.NotificationPreferenceService;
import com.focushive.notification.service.NotificationService;
import com.focushive.notification.service.NotificationTemplateService;
import com.focushive.notification.service.RateLimitingService;
import com.focushive.notification.service.TokenBlacklistService;
import com.focushive.notification.service.UserContextService;
import com.focushive.notification.service.SecurityAuditService;
import com.focushive.notification.monitoring.CorrelationIdService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;

/**
 * Unified test configuration that provides all required beans for testing.
 * This configuration can be used by all test types (@WebMvcTest, @SpringBootTest, etc.)
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@Profile("test")
public class UnifiedTestConfiguration {

    // ===== Core Services =====

    @Bean
    @Primary
    public NotificationService notificationService() {
        return mock(NotificationService.class);
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
    public NotificationPreferenceService notificationPreferenceService() {
        return mock(NotificationPreferenceService.class);
    }

    @Bean
    @Primary
    public NotificationTemplateService notificationTemplateService() {
        return mock(NotificationTemplateService.class);
    }

    @Bean
    @Primary
    public DeadLetterQueueService deadLetterQueueService() {
        return mock(DeadLetterQueueService.class);
    }

    // ===== Security Services =====

    @Bean
    @Primary
    public SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();

        // Authentication config
        SecurityProperties.AuthenticationConfig authConfig = new SecurityProperties.AuthenticationConfig();
        authConfig.setMaxFailedAttempts(5);
        authConfig.setLockoutDuration(java.time.Duration.ofMinutes(15));
        authConfig.setFailedAttemptsWindow(java.time.Duration.ofMinutes(5));
        authConfig.setTrackByIp(true);
        authConfig.setAutoUnlock(true);

        // Rate limiting config
        SecurityProperties.RateLimitingConfig rateLimitingConfig = new SecurityProperties.RateLimitingConfig();
        rateLimitingConfig.setEnabled(true);
        rateLimitingConfig.setDefaultRequestsPerMinute(60);

        // JWT config
        SecurityProperties.JwtConfig jwtConfig = new SecurityProperties.JwtConfig();
        jwtConfig.setBlacklistingEnabled(false);
        jwtConfig.setValidateIssuer(false);
        jwtConfig.setValidateAudience(false);
        jwtConfig.setBlacklistTtl(java.time.Duration.ofHours(24));

        // Headers config
        SecurityProperties.HeadersConfig headersConfig = new SecurityProperties.HeadersConfig();
        headersConfig.setEnabled(true);

        // Audit config
        SecurityProperties.AuditConfig auditConfig = new SecurityProperties.AuditConfig();
        auditConfig.setEnabled(true);

        // CORS config
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
    public UserContextService userContextService() {
        return mock(UserContextService.class);
    }

    @Bean
    @Primary
    public SecurityAuditService securityAuditService() {
        return mock(SecurityAuditService.class);
    }

    // ===== Monitoring Services =====

    @Bean
    @Primary
    public CorrelationIdService correlationIdService() {
        return mock(CorrelationIdService.class);
    }

    // ===== Metrics & Monitoring =====

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

    // ===== Repositories =====

    @Bean
    @Primary
    public NotificationRepository notificationRepository() {
        return mock(NotificationRepository.class);
    }

    @Bean
    @Primary
    public NotificationPreferenceRepository notificationPreferenceRepository() {
        return mock(NotificationPreferenceRepository.class);
    }

    @Bean
    @Primary
    public NotificationTemplateRepository notificationTemplateRepository() {
        return mock(NotificationTemplateRepository.class);
    }

    @Bean
    @Primary
    public DeadLetterMessageRepository deadLetterMessageRepository() {
        return mock(DeadLetterMessageRepository.class);
    }

    // ===== External Dependencies =====

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(3025); // GreenMail test port
        return mailSender;
    }

    @Bean
    @Primary
    public TemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        engine.setTemplateResolver(resolver);
        return engine;
    }

    // ===== Task Executors =====

    @Bean(name = "emailTaskExecutor")
    @Primary
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-async-");
        executor.initialize();
        return executor;
    }

    // ===== JWT Components =====

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        String secret = "test-secret-key-for-testing-purposes-only-minimum-256-bits";
        return NimbusJwtDecoder.withSecretKey(
            new SecretKeySpec(secret.getBytes(), "HmacSHA256")
        ).build();
    }

    @Bean
    @Primary
    public JwtEncoder jwtEncoder() {
        String secret = "test-secret-key-for-testing-purposes-only-minimum-256-bits";
        com.nimbusds.jose.jwk.source.ImmutableSecret<com.nimbusds.jose.proc.SecurityContext> immutableSecret =
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secret.getBytes());
        return new NimbusJwtEncoder(immutableSecret);
    }

    // ===== Test Data Builders =====

    @Bean
    public TestDataBuilder testDataBuilder() {
        return new TestDataBuilder();
    }

    /**
     * Helper class for creating test data
     */
    public static class TestDataBuilder {
        // Add helper methods for creating test entities
    }
}