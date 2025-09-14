package com.focushive.notification.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration to handle H2 database compatibility issues.
 * Basic configuration for H2 database tests.
 * Other mock beans are provided by specific test configurations.
 */
@TestConfiguration
@Profile("test")
public class H2TestConfiguration {
    // This configuration now serves as a placeholder for H2-specific settings
    // Mock beans are provided by EmailTestConfiguration to avoid duplication
}