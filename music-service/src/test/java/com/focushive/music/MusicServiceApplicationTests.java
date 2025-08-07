package com.focushive.music;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the Music Service application.
 * 
 * This test class verifies that the Spring Boot application context
 * loads successfully with all configurations and dependencies.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@SpringBootTest(classes = {MusicServiceApplication.class, com.focushive.music.config.TestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.cloud.openfeign.client.config.default.url=http://localhost:8080"
})
class MusicServiceApplicationTests {

    /**
     * Test that the Spring Boot application context loads successfully.
     * This is a smoke test to ensure all beans are properly configured
     * and there are no configuration errors.
     */
    @Test
    void contextLoads() {
        // This test will fail if there are any context loading issues
        // such as missing dependencies, configuration errors, or bean creation failures
    }

    /**
     * Test that the application starts and shuts down properly.
     * This verifies the complete application lifecycle.
     */
    @Test
    void applicationStartsAndStops() {
        // The test framework handles starting/stopping the application
        // This test passes if the context loads and destroys without errors
    }
}