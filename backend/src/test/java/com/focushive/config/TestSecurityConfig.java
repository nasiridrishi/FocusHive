package com.focushive.config;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.security.IdentityServiceAuthenticationFilter;
import com.focushive.api.security.JwtTokenProvider;
import com.focushive.api.service.CustomUserDetailsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Unified test security configuration that provides mock implementations for components
 * that depend on external services like the Identity Service.
 * This replaces multiple conflicting test configurations.
 */
@TestConfiguration
public class TestSecurityConfig {
    
    // Disable Feign client bean creation for tests
    static {
        System.setProperty("spring.cloud.openfeign.client.enabled", "false");
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health",
            "/error"
    };

    @Bean(name = "identityServiceClient")
    @Primary
    public IdentityServiceClient mockIdentityServiceClient() {
        // Return a mock that does nothing - we don't need it for unit tests
        return mock(IdentityServiceClient.class);
    }
    
    @Bean(name = "identityServiceAuthenticationFilter")
    @Primary
    public IdentityServiceAuthenticationFilter mockIdentityServiceAuthenticationFilter() {
        // Return a mock filter for tests
        return mock(IdentityServiceAuthenticationFilter.class);
    }
    
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // Minimal security configuration for WebMvcTest slice tests
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // Allow all requests for slice tests
                );
                
        return http.build();
    }
    
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        // Create a simple in-memory user for testing
        UserDetails user = User.builder()
                .username("testuser")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
                .build();
                
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("password"))
                .roles("USER", "ADMIN")
                .build();
                
        return new InMemoryUserDetailsManager(user, admin);
    }
    
    @Bean
    @Primary
    public JwtTokenProvider testJwtTokenProvider() {
        // Use the test configuration values
        return new JwtTokenProvider("test-secret-key-for-testing-purposes-only", 3600000L);
    }
    
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    @Primary
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        // Return a mock for tests
        return mock(CustomUserDetailsService.class);
    }
    
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        // Return a mock Redis template for tests
        return mock(RedisTemplate.class);
    }
}