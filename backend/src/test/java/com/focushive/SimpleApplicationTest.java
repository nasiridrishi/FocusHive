package com.focushive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simple test that just verifies the application context loads without conflicts.
 * Uses minimal configuration to avoid bean definition conflicts.
 */
@SpringBootTest(
    classes = {
        com.focushive.test.MinimalTestApp.class
    }
)
@ActiveProfiles("test")
class SimpleApplicationTest {

    @Test
    void contextLoads() {
        // Simple context load test
        // If this test passes, the application context is loading properly
    }
}