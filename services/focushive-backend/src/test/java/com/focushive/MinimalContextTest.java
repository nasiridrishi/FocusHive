package com.focushive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Absolutely minimal context test that loads only basic Spring Boot functionality.
 * This test should pass without any external dependencies.
 */
@SpringBootTest(
    classes = {MinimalContextTest.EmptyTestConfig.class},
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
class MinimalContextTest {

    @Test
    void contextLoads() {
        // This test passes if the minimal Spring context loads successfully
        // with all external dependencies excluded
    }
    
    @TestConfiguration
    static class EmptyTestConfig {
        // Completely empty configuration - just Spring Boot basics
    }
}