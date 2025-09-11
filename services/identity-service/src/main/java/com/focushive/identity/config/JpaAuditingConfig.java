package com.focushive.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing configuration that is only enabled in non-test profiles.
 * This prevents JPA auditing from being enabled during unit tests that
 * exclude JPA autoconfiguration.
 */
@Configuration
@EnableJpaAuditing
@Profile("!web-mvc-test")
public class JpaAuditingConfig {
    // Configuration class for JPA auditing
}