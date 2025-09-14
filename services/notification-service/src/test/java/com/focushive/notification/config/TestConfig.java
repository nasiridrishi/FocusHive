package com.focushive.notification.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test configuration that excludes problematic auto-configurations for testing.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    OAuth2ResourceServerAutoConfiguration.class,
    RabbitAutoConfiguration.class,
    RedisAutoConfiguration.class,
    MailSenderAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.focushive.notification",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*SecurityConfig.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*MessageConfig.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*RedisConfig.*")
    }
)
@EnableJpaRepositories(basePackages = "com.focushive.notification.repository")
public class TestConfig {
}