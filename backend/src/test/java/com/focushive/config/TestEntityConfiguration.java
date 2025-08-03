package com.focushive.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that ensures proper entity scanning for H2 compatibility.
 */
@Configuration
@Profile("test")
@EntityScan(basePackages = "com.focushive")
@ComponentScan(basePackages = "com.focushive")
public class TestEntityConfiguration {
    // Configuration for test environment
}