package com.focushive.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.focushive.identity.security.JwtAuthenticationFilter;
import com.focushive.identity.security.PathTraversalPreventionFilter;
import com.focushive.identity.security.OWASPCompliantCorsFilter;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@Profile({"!test", "!security-test", "!owasp-test"})
public class SecurityConfig {

    @Value("${security.cors.allowed-origins}")
    private String corsAllowedOrigins;

    // OAuth2 Authorization Server configuration is now handled by AuthorizationServerConfig

    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                         JwtAuthenticationFilter jwtAuthenticationFilter,
                                                         PathTraversalPreventionFilter pathTraversalPreventionFilter,
                                                         OWASPCompliantCorsFilter owaspCompliantCorsFilter,
                                                         CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // This filter chain handles API requests and everything else not handled by previous chains
                .securityMatcher(request ->
                    request.getRequestURI().startsWith("/api/") ||
                    request.getRequestURI().startsWith("/actuator/") ||
                    request.getRequestURI().equals("/health") ||
                    request.getRequestURI().startsWith("/api-docs/") ||
                    request.getRequestURI().startsWith("/swagger-ui/")
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Disable CSRF protection for stateless JWT authentication
                // JWT tokens are not vulnerable to CSRF attacks since they are sent in headers, not cookies
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((authorize) -> authorize
                        // A01: Allow CORS preflight requests (OPTIONS) to pass through
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // A05: Secure actuator endpoints - only health check is public
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")  // All other actuator endpoints require admin
                        .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/validate").permitAll()
                        .requestMatchers("/api/auth/introspect").permitAll()
                        .requestMatchers("/api/auth/jwks", "/api/auth/jwt-health").permitAll()
                        .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/redirect").permitAll()  // A10: Allow redirect endpoint for SSRF testing
                        .requestMatchers("/oauth2/**", "/.well-known/**").permitAll()
                        .requestMatchers("/api/oauth2/**").permitAll()
                        .requestMatchers("/api/performance-test/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(owaspCompliantCorsFilter, org.springframework.web.filter.CorsFilter.class)
                .addFilterBefore(pathTraversalPreventionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // OAuth2 Authorization Server beans are now in AuthorizationServerConfig

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized: " + authException.getMessage() + "\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Parse allowed origins from properties
        String[] allowedOriginsArray = corsAllowedOrigins.split(",");

        SecurityAwareCorsConfiguration configuration = new SecurityAwareCorsConfiguration();
        configuration.setTrustedOrigins(Arrays.asList(allowedOriginsArray));

        // For OWASP A01 compliance: Accept preflight requests from any origin
        // but only return CORS headers for trusted origins
        // Allow all origins for OPTIONS but control headers via checkOrigin
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // IMPORTANT: Don't set allowCredentials to true with wildcard origins
        // This is done by the custom checkOrigin method instead
        configuration.setAllowCredentials(false);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Custom CORS configuration that properly handles OWASP A01 compliance.
     *
     * For unauthorized origins:
     * - Returns null for Access-Control-Allow-Origin (header not set)
     * - Still allows OPTIONS requests to pass through (required for CORS spec)
     * - Logs security violations for monitoring
     */
    private static class SecurityAwareCorsConfiguration extends CorsConfiguration {

        private static final org.slf4j.Logger securityLogger =
            org.slf4j.LoggerFactory.getLogger("SECURITY.CORS");

        private java.util.List<String> trustedOrigins;

        public void setTrustedOrigins(java.util.List<String> trustedOrigins) {
            this.trustedOrigins = trustedOrigins;
        }

        @Override
        public String checkOrigin(String requestOrigin) {
            if (requestOrigin == null) {
                // No origin header - allow for same-origin requests
                return null;
            }

            // Check if origin is in our trusted list
            boolean isTrusted = trustedOrigins != null && trustedOrigins.contains(requestOrigin);

            if (isTrusted) {
                securityLogger.debug("CORS: Authorized origin access: {}", requestOrigin);
                return requestOrigin;
            } else {
                // Log security violation but allow preflight to proceed
                // Return null to indicate no CORS headers should be set for this origin
                securityLogger.warn("CORS violation: Unauthorized origin attempted access: {}", requestOrigin);
                return null;
            }
        }

        @Override
        public Boolean getAllowCredentials() {
            // Only allow credentials for trusted origins
            return false;
        }

    }

}