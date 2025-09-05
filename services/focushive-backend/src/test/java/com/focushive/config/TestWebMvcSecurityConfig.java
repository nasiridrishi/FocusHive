package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration specifically for WebMvc controller tests that need
 * @AuthenticationPrincipal support.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("webmvc-test")
public class TestWebMvcSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain webMvcTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        // Create test users for @WithMockUser to work with @AuthenticationPrincipal
        UserDetails user = User.builder()
                .username("testuser")
                .password("{noop}password") // noop encoder for tests
                .roles("USER")
                .build();
        
        UserDetails admin = User.builder()
                .username("adminuser")
                .password("{noop}adminpassword")
                .roles("USER", "ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}