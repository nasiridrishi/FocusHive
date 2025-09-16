package com.focushive.api.security;

import com.focushive.api.config.SimpleRateLimitingFilter;
import com.focushive.backend.security.IdentityServiceAuthenticationFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Test security configuration for authentication tests.
 * Provides a simplified security configuration that delegates authentication to Identity Service.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("authtest")
public class TestSecurityConfiguration {

    private static final String[] PUBLIC_ENDPOINTS = {
        "/actuator/health",
        "/error"
    };

    private static final String[] PROTECTED_ENDPOINTS = {
        "/api/v1/**"
    };

    @Bean
    public SecurityFilterChain testSecurityFilterChain(
            HttpSecurity http,
            IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter,
            SimpleRateLimitingFilter simpleRateLimitingFilter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(PROTECTED_ENDPOINTS).authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\"}");
                })
            );

        // Add authentication filter that delegates to Identity Service
        http.addFilterBefore(identityServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(simpleRateLimitingFilter, IdentityServiceAuthenticationFilter.class);

        return http.build();
    }
}