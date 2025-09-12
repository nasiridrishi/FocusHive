package com.focushive.gateway.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * JWT Authentication Filter for Spring Cloud Gateway
 * 
 * Validates JWT tokens from the Identity Service and adds user context
 * to request headers for downstream services.
 */
@Component
class JwtAuthenticationFilter : AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config>() {
    
    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    
    @Value("\${focushive.jwt.secret:your-256-bit-secret-key-here-make-it-secure}")
    private lateinit var jwtSecret: String
    
    @Value("\${focushive.jwt.expiration:86400000}") // 24 hours
    private var jwtExpirationMs: Int = 86400000
    
    class Config {
        var enabled: Boolean = true
    }
    
    override fun getConfigClass(): Class<Config> = Config::class.java
    
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            if (!config.enabled) {
                return@GatewayFilter chain.filter(exchange)
            }
            
            val request = exchange.request
            val response = exchange.response
            
            // Skip authentication for health checks and public endpoints
            if (isPublicEndpoint(request.path.value())) {
                return@GatewayFilter chain.filter(exchange)
            }
            
            val token = extractToken(request)
            
            if (token == null) {
                logger.warn("Missing authorization token for path: ${request.path.value()}")
                return@GatewayFilter unauthorized(response)
            }
            
            try {
                val claims = validateAndParseToken(token)
                val modifiedExchange = addUserContextHeaders(exchange, claims)
                
                logger.debug("Successfully authenticated user: ${claims["sub"]} for path: ${request.path.value()}")
                chain.filter(modifiedExchange)
                
            } catch (e: Exception) {
                logger.warn("JWT validation failed for path: ${request.path.value()}", e)
                unauthorized(response)
            }
        }
    }
    
    private fun isPublicEndpoint(path: String): Boolean {
        val publicPaths = setOf(
            "/auth/login",
            "/auth/register", 
            "/auth/forgot-password",
            "/auth/reset-password",
            "/oauth2/",
            "/health",
            "/actuator/health",
            "/.well-known/",
            "/error"
        )
        
        return publicPaths.any { path.startsWith(it) } || 
               path.contains("/actuator/health") ||
               path.contains("/.well-known/")
    }
    
    private fun extractToken(request: ServerHttpRequest): String? {
        val authHeader = request.headers.getFirst("Authorization")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else null
    }
    
    private fun validateAndParseToken(token: String): Claims {
        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
        
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }
    
    private fun addUserContextHeaders(
        exchange: ServerWebExchange, 
        claims: Claims
    ): ServerWebExchange {
        val userId = claims.subject
        val username = claims["username"] as? String
        val roles = claims["roles"] as? List<*>
        val personaId = claims["persona_id"] as? String
        
        val modifiedRequest = exchange.request.mutate()
            .header("X-User-Id", userId)
            .header("X-Username", username ?: "")
            .header("X-User-Roles", roles?.joinToString(",") ?: "")
            .header("X-Persona-Id", personaId ?: "")
            .header("X-Auth-Provider", "focushive-identity-service")
            .build()
        
        return exchange.mutate().request(modifiedRequest).build()
    }
    
    private fun unauthorized(response: ServerHttpResponse): Mono<Void> {
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        
        val body = """
            {
                "error": "Unauthorized",
                "message": "Valid JWT token required",
                "timestamp": "${Date()}",
                "status": 401
            }
        """.trimIndent()
        
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}