package com.focushive.buddy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Buddy Service.
 * Implements JWT-based authentication, CORS, and comprehensive endpoint security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("identity-service")
public class SecurityConfig {

    @Value("${buddy.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${buddy.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${buddy.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${buddy.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${buddy.cors.max-age:3600}")
    private long corsMaxAge;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8081/.well-known/jwks.json}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())

            // Set session management to stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure endpoint authorization
            .authorizeHttpRequests(authz -> authz
                // Public health endpoints
                .requestMatchers("/api/v1/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()

                // Swagger documentation (only in non-prod environments)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                // OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Actuator endpoints require ACTUATOR_ADMIN role (except health)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")

                // Admin endpoints
                .requestMatchers("/api/v1/buddy/matching/queue/size").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // All other API endpoints require authentication
                .requestMatchers("/api/v1/buddy/**").authenticated()

                // Deny all other requests by default
                .anyRequest().denyAll()
            )

            // Configure JWT authentication
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse and set allowed origins - NEVER use * in production
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Parse and set allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // Set allowed headers
        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(Arrays.asList("*"));
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }

        // Set credentials support
        configuration.setAllowCredentials(allowCredentials);

        // Set max age for preflight requests
        configuration.setMaxAge(corsMaxAge);

        // Set exposed headers
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count", "X-Page-Number", "X-Page-Size"));

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}