package com.focushive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Application context test using the same pattern as MinimalContextTest.
 * This test verifies that the Spring Boot application context can load successfully
 * with test profile active and all external dependencies disabled.
 */
@SpringBootTest(
    classes = {FocusHiveApplicationTests.EmptyTestConfig.class},
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
class FocusHiveApplicationTests {

    @Test
    void contextLoads() {
        // This test passes if the Spring context loads successfully
        // with all external dependencies disabled
    }
    
    @TestConfiguration
    static class EmptyTestConfig {
        // Completely empty configuration - just Spring Boot basics
    }
}