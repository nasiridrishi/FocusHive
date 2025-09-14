package com.focushive.hive.integration;

import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.service.IdentityIntegrationService;
import com.focushive.backend.security.IdentityServiceAuthenticationFilter;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Test configuration for integration tests.
 * Provides mock beans for external dependencies and disabled security.
 */
@TestConfiguration
@EnableWebSecurity
public class HiveIntegrationTestConfiguration {

    /**
     * Mock the IdentityServiceClient to avoid Feign dependency issues
     */
    @Bean
    @Primary
    public IdentityServiceClient mockIdentityServiceClient() {
        return Mockito.mock(IdentityServiceClient.class);
    }

    /**
     * Mock the IdentityIntegrationService to avoid dependency on IdentityServiceClient
     * Note: Not marked as @Primary to avoid conflicts with TestMockConfig's primary bean
     */
    @Bean
    public IdentityIntegrationService mockIdentityIntegrationService() {
        return Mockito.mock(IdentityIntegrationService.class);
    }

    /**
     * Configure test security - permit all requests to simplify testing
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}