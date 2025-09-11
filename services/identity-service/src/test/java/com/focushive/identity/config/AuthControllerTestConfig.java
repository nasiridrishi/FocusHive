package com.focushive.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.*;
import io.micrometer.observation.ObservationRegistry;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration specifically for AuthController tests.
 * Provides all necessary mock beans and configurations.
 */
@TestConfiguration
@Profile("test")
@EnableWebSecurity
public class AuthControllerTestConfig {

    /**
     * Primary ObjectMapper for tests
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Mock JwtTokenProvider for tests
     */
    @Bean
    @Primary
    public JwtTokenProvider mockJwtTokenProvider() {
        JwtTokenProvider mockProvider = Mockito.mock(JwtTokenProvider.class);
        
        // Configure default mock behavior
        Mockito.when(mockProvider.validateToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockProvider.extractUsername(Mockito.anyString())).thenReturn("testuser");
        
        return mockProvider;
    }


    /**
     * Mock PersonaService for tests
     */
    @Bean
    @Primary
    public PersonaService mockPersonaService() {
        return Mockito.mock(PersonaService.class);
    }

    /**
     * Mock UserRepository for tests
     */
    @Bean
    @Primary
    public UserRepository mockUserRepository() {
        return Mockito.mock(UserRepository.class);
    }

    /**
     * Mock PersonaRepository for tests
     */
    @Bean
    @Primary
    public PersonaRepository mockPersonaRepository() {
        return Mockito.mock(PersonaRepository.class);
    }

    /**
     * Mock CustomUserDetailsService for tests
     */
    @Bean
    @Primary
    public CustomUserDetailsService mockCustomUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    /**
     * Mock EmailService for tests
     */
    @Bean
    @Primary
    public EmailService mockEmailService() {
        return Mockito.mock(EmailService.class);
    }

    /**
     * Mock TokenBlacklistService for tests
     */
    @Bean
    @Primary
    public TokenBlacklistService mockTokenBlacklistService() {
        TokenBlacklistService mockService = Mockito.mock(TokenBlacklistService.class);
        Mockito.when(mockService.isBlacklisted(Mockito.anyString())).thenReturn(false);
        return mockService;
    }


    /**
     * PasswordEncoder for tests
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Mock AuthenticationManager for tests
     */
    @Bean
    @Primary
    public AuthenticationManager mockAuthenticationManager() {
        return Mockito.mock(AuthenticationManager.class);
    }

    /**
     * No-op ObservationRegistry for tests
     */
    @Bean
    @Primary
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }

    /**
     * Test security filter chain that disables security for testing
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