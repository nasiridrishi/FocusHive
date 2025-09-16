package com.focushive.buddy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

/**
 * Test Security Configuration
 * Disables security for E2E tests to focus on API functionality
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Security filter chain that permits all requests for testing
     */
    @Bean
    @Primary
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );

        return http.build();
    }

    /**
     * Mock authentication for testing
     */
    public static void mockAuthentication(String userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Mock admin authentication for testing
     */
    public static void mockAdminAuthentication(String userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
            )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Clear authentication context
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
