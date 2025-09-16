package com.focushive.api.config;

import com.focushive.api.config.SimpleRateLimitingFilter;
import com.focushive.backend.security.IdentityServiceAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
@Profile("!test") // Don't load this configuration in test profile
public class SecurityConfig {

    private final IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter;
    private final SimpleRateLimitingFilter simpleRateLimitingFilter;
    
    @Value("${backend.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    @Value("${backend.csrf.enabled:false}")
    private boolean csrfEnabled;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/demo/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**", // SpringDoc OpenAPI docs
            "/webjars/**",
            "/actuator/health",
            "/error",
            "/ws", // WebSocket endpoint
            "/ws/**", // WebSocket sub-paths
            "/web/public/**" // Public web endpoints
    };

    private static final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**",
            "/actuator/**" // Most actuator endpoints should be admin-only except health
    };

    private static final String[] PRIVATE_ENDPOINTS = {
            "/api/v1/hives/**",
            "/api/v1/presence/**",
            "/api/v1/timer/**",
            "/api/v1/analytics/**",
            "/api/v1/chat/**",
            "/api/notifications/**",
            "/api/buddy/**",
            "/api/health/**",
            "/api/test/**"
    };


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure CSRF protection
                .csrf(csrf -> {
                    if (csrfEnabled) {
                        log.info("CSRF protection enabled");
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                            .ignoringRequestMatchers(PUBLIC_ENDPOINTS); // Don't apply CSRF to public endpoints
                    } else {
                        log.info("CSRF protection disabled");
                        csrf.disable();
                    }
                })

                // Configure session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        .requestMatchers(PRIVATE_ENDPOINTS).authenticated()
                        .anyRequest().authenticated()
                )

                // Configure exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Authentication failed for request: {} - {}",
                                    request.getRequestURI(), authException.getMessage());
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Access denied for request: {} - {}",
                                    request.getRequestURI(), accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                        })
                );

        // Configure security headers
        SimplifiedSecurityHeadersConfig.configureSecurityHeaders(http);

        // Add custom filters
        http.addFilterBefore(simpleRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(identityServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse and validate allowed origins
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            log.warn("No CORS allowed origins configured, using default localhost origins");
            origins = Arrays.asList("http://localhost:3000", "http://localhost:5173");
        }

        log.info("Configuring CORS with allowed origins: {}", origins);

        // Allowed origins (explicit list for security)
        configuration.setAllowedOrigins(origins);

        // Allowed methods (RESTful + OPTIONS for preflight)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed headers (explicitly list required headers)
        configuration.setAllowedHeaders(Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-CSRF-TOKEN" // For CSRF protection when enabled
        ));

        // Exposed headers (allow client to access these response headers)
        configuration.setExposedHeaders(Arrays.asList(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Allow credentials (needed for cookies and authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}