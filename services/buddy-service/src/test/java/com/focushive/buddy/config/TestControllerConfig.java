package com.focushive.buddy.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Test configuration for controller tests that excludes JPA/Database components
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.focushive.buddy",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = EnableJpaAuditing.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Repository"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Entity")
    }
)
public class TestControllerConfig {
}