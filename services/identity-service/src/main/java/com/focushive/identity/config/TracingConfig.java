package com.focushive.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Tracing configuration for Identity Service.
 * Provides minimal configuration for test profiles.
 */
@Configuration
@Profile("test")
public class TracingConfig {
    // Empty configuration to disable tracing in tests
}