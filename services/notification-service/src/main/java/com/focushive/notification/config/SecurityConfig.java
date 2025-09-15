package com.focushive.notification.config;

import com.focushive.notification.security.ApiKeyAuthenticationFilter;
import com.focushive.notification.security.CustomAuthenticationEntryPoint;
import com.focushive.notification.security.RequestValidationFilter;
import com.focushive.notification.security.ServiceAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Notification Service.
 * 
 * Configures JWT authentication, CORS, and endpoint security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${notification.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final RequestValidationFilter requestValidationFilter;
    private final ServiceAuthenticationFilter serviceAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    public SecurityConfig(CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                         RequestValidationFilter requestValidationFilter,
                         ServiceAuthenticationFilter serviceAuthenticationFilter,
                         ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.requestValidationFilter = requestValidationFilter;
        this.serviceAuthenticationFilter = serviceAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }

    /**
     * Security filter chain for public endpoints (Swagger/OpenAPI).
     * This chain has higher precedence (Order 1) and handles all documentation endpoints.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(request -> {
                String path = request.getRequestURI();
                return path.startsWith("/v3/") ||
                       path.startsWith("/swagger-ui") ||
                       path.equals("/swagger-ui.html") ||
                       path.startsWith("/swagger-resources") ||
                       path.startsWith("/webjars/") ||
                       path.startsWith("/configuration/") ||
                       path.startsWith("/actuator/") ||
                       path.equals("/health") ||
                       path.equals("/api/v1/health") ||
                       path.startsWith("/api-docs/") ||
                       path.startsWith("/api/test/") ||
                       path.startsWith("/api/monitoring/") ||
                       path.equals("/api/metrics") ||
                       path.equals("/api/auth/token/validate/public") ||
                       path.equals("/error");
            })
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            // Add request validation filter for public endpoints too
            .addFilterBefore(requestValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // Explicitly disable OAuth2 resource server for these endpoints
            .oauth2ResourceServer(AbstractHttpConfigurer::disable)
            // Add custom exception handling for public endpoints
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            .build();
    }

    /**
     * Security filter chain for protected API endpoints.
     * This chain has lower precedence (Order 2) and handles all other endpoints.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        // Configure security for all other endpoints with OAuth2 resource server
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Add request validation filter FIRST to catch malformed JSON
            .addFilterBefore(requestValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // Add API key authentication filter for service-to-service auth (checks API keys first)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Add service authentication filter for JWT-based service auth (fallback to JWT)
            .addFilterAfter(serviceAuthenticationFilter, ApiKeyAuthenticationFilter.class)
            .authorizeHttpRequests(authz -> authz
                // OPTIONS requests should be permitted for CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All endpoints require authentication by default
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            // Global exception handling with custom entry point
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
