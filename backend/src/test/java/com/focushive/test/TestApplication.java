package com.focushive.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Test application configuration that excludes Feign clients and other
 * external dependencies that aren't needed for unit tests.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.focushive",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com.focushive.api.client.*"
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {com.focushive.api.controller.AuthProxyController.class}
        )
    }
)
@EnableJpaAuditing
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}