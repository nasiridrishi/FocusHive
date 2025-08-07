package com.focushive.music.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration for the Music Service.
 * 
 * Provides simplified security configuration for testing
 * without JWT validation dependencies.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@TestConfiguration
@EnableWebSecurity
public class TestConfig {

    /**
     * Test security filter chain that permits all requests.
     * 
     * @param http The HttpSecurity configuration
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .build();
    }
}