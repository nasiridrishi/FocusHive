package com.focushive.notification.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Base annotation for integration tests with full application context.
 * Includes all necessary test configurations and mocks.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({UnifiedTestConfiguration.class, TestSecurityConfig.class})
public @interface BaseIntegrationTest {
}