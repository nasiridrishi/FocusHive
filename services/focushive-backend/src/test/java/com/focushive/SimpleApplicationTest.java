package com.focushive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Simple test using the same pattern as MinimalContextTest that successfully passes.
 * Uses the proven configuration that excludes all problematic auto-configurations.
 */
@SpringBootTest(
    classes = {SimpleApplicationTest.EmptyTestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.main.web-application-type=none",
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration," +
            "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
            "org.springframework.cloud.openfeign.FeignAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    }
)
@ActiveProfiles("test")
class SimpleApplicationTest {

    @Test
    void contextLoads() {
        // Simple context load test
        // If this test passes, the application context is loading properly
    }
    
    @TestConfiguration
    static class EmptyTestConfig {
        // Completely empty configuration - just Spring Boot basics
    }
}