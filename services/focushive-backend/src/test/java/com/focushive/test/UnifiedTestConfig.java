package com.focushive.test;

import com.focushive.config.TestMockConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Unified test configuration that combines all test configurations.
 * This provides a single import for tests that need comprehensive mocking.
 */
@TestConfiguration
@Profile("test")
@Import({
    TestMockConfig.class
})
public class UnifiedTestConfig {
    // This class serves as an aggregator for all test configurations
    // No additional beans are defined here
}