package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Test configuration specifically for repository tests.
 * Includes only JPA-related functionality and excludes all external dependencies.
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    WebSocketServletAutoConfiguration.class,
    WebSocketMessagingAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    FeignAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = {"com.focushive"})
@EnableJpaAuditing
public class RepositoryTestConfig {
    // Configuration for repository tests only
}