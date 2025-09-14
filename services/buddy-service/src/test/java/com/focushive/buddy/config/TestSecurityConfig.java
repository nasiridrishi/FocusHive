package com.focushive.buddy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Test-specific security configuration that disables authentication
 * for integration tests and provides JWT mocking capabilities.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.disable());

        return http.build();
    }

    /**
     * Helper method to mock authentication for test methods
     */
    public static void mockAuthentication(String userId) {
        mockAuthentication(userId, List.of("ROLE_USER"));
    }

    /**
     * Helper method to mock authentication with specific roles
     */
    public static void mockAuthentication(String userId, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                userId, null, authorities);
        authentication.setAuthenticated(true);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Helper method to clear authentication context
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper method to mock admin authentication
     */
    public static void mockAdminAuthentication(String userId) {
        mockAuthentication(userId, List.of("ROLE_USER", "ROLE_ADMIN"));
    }
}