package com.focushive.identity.config;

import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.security.JwtAuthenticationFilter;
import com.focushive.identity.service.CustomUserDetailsService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Test security configuration that provides mock implementations
 * for JWT-related components and disables security for tests.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    /**
     * Mock JwtTokenProvider for tests.
     */
    @Bean
    @Primary
    public JwtTokenProvider mockJwtTokenProvider() {
        JwtTokenProvider mockProvider = Mockito.mock(JwtTokenProvider.class);
        
        // Configure mock behavior if needed
        Mockito.when(mockProvider.validateToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockProvider.extractUsername(Mockito.anyString())).thenReturn("testuser");
        
        return mockProvider;
    }

    /**
     * Mock CustomUserDetailsService for tests.
     */
    @Bean
    @Primary
    public CustomUserDetailsService mockCustomUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    /**
     * PasswordEncoder for tests.
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Mock AuthenticationManager for tests.
     */
    @Bean
    @Primary
    public AuthenticationManager authenticationManager() {
        return Mockito.mock(AuthenticationManager.class);
    }

    /**
     * Mock JwtAuthenticationFilter for tests.
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter mockJwtAuthenticationFilter() {
        return Mockito.mock(JwtAuthenticationFilter.class);
    }

    // TokenBlacklistService is now provided by TestTokenBlacklistService
    // No need to mock it anymore

    /**
     * Test security filter chain that completely disables security for testing.
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
}