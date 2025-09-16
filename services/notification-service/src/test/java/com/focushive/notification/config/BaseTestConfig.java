package com.focushive.notification.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Base test configuration that provides mock beans for external dependencies.
 * This configuration is used to ensure tests can run without requiring
 * actual Redis, RabbitMQ, or OAuth2 servers.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    OAuth2ResourceServerAutoConfiguration.class,
    RabbitAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@Profile("test")
public class BaseTestConfig {

    /**
     * Mock RabbitTemplate for tests.
     * This prevents the need for an actual RabbitMQ server during testing.
     */
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
}
