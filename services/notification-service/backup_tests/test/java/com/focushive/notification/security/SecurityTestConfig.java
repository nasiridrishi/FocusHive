package com.focushive.notification.security;

import com.focushive.notification.config.BaseTestConfig;
import com.focushive.notification.service.SecurityAuditService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test configuration for security integration tests.
 * Provides minimal security configuration for testing purposes.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Import(BaseTestConfig.class)
@ActiveProfiles("test")
public class SecurityTestConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            response.setHeader("X-XSS-Protection", "1; mode=block");
                            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                        })
                )
                .build();
    }
}