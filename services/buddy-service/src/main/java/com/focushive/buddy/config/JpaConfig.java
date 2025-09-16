package com.focushive.buddy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration class.
 * Separated from main application class to allow controller tests to run without JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}