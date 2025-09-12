package com.focushive.identity.integration;

import com.focushive.identity.config.MinimalTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic integration test that verifies the Spring application context can start successfully.
 * This is the simplest possible integration test - it just checks that all beans can be created
 * without any configuration issues.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(MinimalTestConfig.class)
@TestPropertySource(properties = {
    "focushive.security.headers.enabled=false",
    "focushive.security.headers.permissions-policy.enabled=false"
})
class BasicApplicationContextTest {

    /**
     * Test that the Spring application context loads successfully.
     * If this test passes, it means:
     * - All configuration is valid
     * - All required beans can be created
     * - No circular dependencies exist
     * - Properties are bound correctly
     */
    @Test
    void applicationContextLoads() {
        // This test passes if the Spring context starts without exceptions
        // The @SpringBootTest annotation handles the actual context loading
    }
}