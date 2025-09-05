package com.focushive.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test configuration for JPA repository tests.
 * Provides minimal configuration needed for @DataJpaTest without conflicts.
 */
@TestConfiguration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
    "com.focushive.notification.repository",
    "com.focushive.user.repository",
    "com.focushive.hive.repository",
    "com.focushive.timer.repository",
    "com.focushive.analytics.repository",
    "com.focushive.chat.repository",
    "com.focushive.buddy.repository",
    "com.focushive.forum.repository"
})
@EntityScan(basePackages = {
    "com.focushive.notification.entity",
    "com.focushive.user.entity",
    "com.focushive.hive.entity",
    "com.focushive.timer.entity",
    "com.focushive.analytics.entity",
    "com.focushive.chat.entity",
    "com.focushive.buddy.entity",
    "com.focushive.forum.entity",
    "com.focushive.common.entity"
})
@ActiveProfiles("test")
public class TestJpaConfig {
    // Configuration for repository tests
}