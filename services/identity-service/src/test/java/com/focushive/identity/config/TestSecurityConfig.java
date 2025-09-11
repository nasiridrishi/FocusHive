package com.focushive.identity.config;

import com.focushive.identity.security.JwtAuthenticationFilter;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.CustomUserDetailsService;
import com.focushive.identity.service.TokenBlacklistService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables authentication for testing.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private CustomUserDetailsService customUserDetailsService;
    
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    @Primary
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
    
    // PasswordEncoder is provided by main SecurityConfig
}