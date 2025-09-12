package com.focushive.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets

/**
 * Security Configuration for FocusHive API Gateway
 * 
 * Provides:
 * - JWT token validation
 * - CORS configuration
 * - Public endpoint exclusions
 * - Security headers
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    
    @Value("\${focushive.jwt.secret:your-256-bit-secret-key-here-make-it-secure}")
    private lateinit var jwtSecret: String
    
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081}")
    private lateinit var issuerUri: String
    
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints - no authentication required
                    .pathMatchers(
                        "/auth/login",
                        "/auth/register",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/oauth2/**",
                        "/.well-known/**",
                        "/health/**",
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/error",
                        "/fallback/**"
                    ).permitAll()
                    
                    // WebSocket connections (handled by individual services)
                    .pathMatchers("/ws/**", "/websocket/**").permitAll()
                    
                    // All other endpoints require authentication
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(reactiveJwtDecoder())
                }
            }
            .build()
    }
    
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        // For symmetric key (HMAC) - use this if identity service uses HMAC
        val secretKeySpec = SecretKeySpec(
            jwtSecret.toByteArray(StandardCharsets.UTF_8),
            SignatureAlgorithm.HS256.name
        )
        
        return NimbusReactiveJwtDecoder.withSecretKey(secretKeySpec)
            .macAlgorithm(SignatureAlgorithm.HS256)
            .build()
        
        // Alternative: For asymmetric keys (RSA) - uncomment if identity service uses RSA
        /*
        return NimbusReactiveJwtDecoder
            .withJwkSetUri("$issuerUri/oauth2/jwks")
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build()
        */
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // Allow specific origins in production
            allowedOriginPatterns = listOf(
                "http://localhost:*",
                "https://localhost:*", 
                "http://127.0.0.1:*",
                "https://focushive.com",
                "https://*.focushive.com"
            )
            
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type", 
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-User-Id",
                "X-Persona-Id",
                "X-Correlation-ID",
                "X-Request-ID"
            )
            
            exposedHeaders = listOf(
                "Authorization",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Retry-After-Seconds",
                "X-Correlation-ID",
                "X-Request-ID"
            )
            
            allowCredentials = true
            maxAge = 3600L // 1 hour
        }
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
    
    /**
     * Custom matcher for WebSocket upgrade requests
     */
    @Bean
    fun webSocketMatcher(): PathPatternParserServerWebExchangeMatcher {
        return PathPatternParserServerWebExchangeMatcher("/ws/**")
    }
}