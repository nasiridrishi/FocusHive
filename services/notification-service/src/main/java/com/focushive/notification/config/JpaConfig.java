package com.focushive.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.persistence.EntityManager;

/**
 * JPA configuration separated from main application class.
 * This allows @WebMvcTest to exclude JPA configuration entirely,
 * preventing ApplicationContext loading failures in controller tests.
 *
 * TDD: This separation ensures controller tests focus only on web layer
 * without loading unnecessary JPA infrastructure.
 */
@Configuration
@EnableJpaAuditing
@ConditionalOnClass(EntityManager.class)
public class JpaConfig {
    // JPA auditing configuration is handled by @EnableJpaAuditing
    // This class intentionally left empty - its purpose is configuration separation
}