package com.focushive.notification.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables security for unit tests.
 * This configuration is used for @WebMvcTest to bypass authentication.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Provides a security filter chain that permits all requests without authentication.
     * This is suitable for unit tests where we want to test controller logic without
     * dealing with security concerns.
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .build();
    }
}