package com.focushive.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

/**
 * Global Filter for FocusHive API Gateway
 * 
 * Provides:
 * - Request/response logging with timing
 * - Security headers
 * - Request correlation IDs
 * - Performance monitoring
 */
@Component
class GlobalFilter : GlobalFilter, Ordered {
    
    private val logger = LoggerFactory.getLogger(GlobalFilter::class.java)
    
    companion object {
        const val START_TIME_ATTR = "startTime"
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val REQUEST_ID_HEADER = "X-Request-ID"
    }
    
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val startTime = Instant.now()
        val request = exchange.request
        val response = exchange.response
        
        // Generate correlation ID if not present
        val correlationId = request.headers.getFirst(CORRELATION_ID_HEADER) 
            ?: generateCorrelationId()
        
        // Add correlation ID to response
        response.headers.add(CORRELATION_ID_HEADER, correlationId)
        response.headers.add(REQUEST_ID_HEADER, generateRequestId())
        
        // Add security headers
        addSecurityHeaders(response)
        
        // Log incoming request
        logRequest(request, correlationId)
        
        // Store start time for performance monitoring
        exchange.attributes[START_TIME_ATTR] = startTime
        
        return chain.filter(exchange)
            .doFinally { 
                logResponse(request, response, startTime, correlationId)
            }
    }
    
    private fun addSecurityHeaders(response: ServerHttpResponse) {
        response.headers.apply {
            // CORS headers (additional to gateway CORS config)
            add("X-Content-Type-Options", "nosniff")
            add("X-Frame-Options", "DENY")
            add("X-XSS-Protection", "1; mode=block")
            add("Referrer-Policy", "strict-origin-when-cross-origin")
            add("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")
            
            // Content Security Policy
            add("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' https: wss: ws:; " +
                "media-src 'self'; " +
                "object-src 'none'; " +
                "base-uri 'self'")
            
            // Strict Transport Security
            add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
            
            // API Gateway identification
            add("X-Powered-By", "FocusHive-API-Gateway")
            add("Server", "FocusHive-Gateway/1.0")
        }
    }
    
    private fun logRequest(request: ServerHttpRequest, correlationId: String) {
        val method = request.method?.name
        val path = request.path.value()
        val userAgent = request.headers.getFirst("User-Agent") ?: "Unknown"
        val clientIp = getClientIpAddress(request)
        val contentType = request.headers.contentType?.toString() ?: "none"
        
        logger.info(
            "Incoming request - Method: {}, Path: {}, IP: {}, User-Agent: {}, Content-Type: {}, Correlation-ID: {}",
            method, path, clientIp, userAgent, contentType, correlationId
        )
        
        // Log headers for debugging (excluding sensitive ones)
        if (logger.isDebugEnabled) {
            val safeHeaders = request.headers.filterKeys { 
                !it.lowercase().contains("authorization") && 
                !it.lowercase().contains("cookie") &&
                !it.lowercase().contains("password")
            }
            logger.debug("Request headers: {}", safeHeaders)
        }
    }
    
    private fun logResponse(
        request: ServerHttpRequest, 
        response: ServerHttpResponse, 
        startTime: Instant,
        correlationId: String
    ) {
        val duration = Duration.between(startTime, Instant.now()).toMillis()
        val method = request.method?.name
        val path = request.path.value()
        val statusCode = response.statusCode?.value() ?: 0
        val clientIp = getClientIpAddress(request)
        
        val logLevel = when {
            statusCode >= 500 -> "ERROR"
            statusCode >= 400 -> "WARN"
            statusCode >= 300 -> "INFO"
            else -> "INFO"
        }
        
        val message = "Request completed - Method: {}, Path: {}, Status: {}, Duration: {}ms, IP: {}, Correlation-ID: {}"
        
        when (logLevel) {
            "ERROR" -> logger.error(message, method, path, statusCode, duration, clientIp, correlationId)
            "WARN" -> logger.warn(message, method, path, statusCode, duration, clientIp, correlationId)
            else -> logger.info(message, method, path, statusCode, duration, clientIp, correlationId)
        }
        
        // Log performance warning for slow requests
        if (duration > 5000) { // 5 seconds
            logger.warn("Slow request detected - Duration: {}ms, Path: {}, Correlation-ID: {}", 
                duration, path, correlationId)
        }
    }
    
    private fun getClientIpAddress(request: ServerHttpRequest): String {
        // Check various headers for the real client IP
        val headers = listOf(
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        )
        
        for (header in headers) {
            val ip = request.headers.getFirst(header)
            if (!ip.isNullOrBlank() && !"unknown".equals(ip, ignoreCase = true)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim()
            }
        }
        
        // Fallback to remote address
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }
    
    private fun generateCorrelationId(): String {
        return "fh-gw-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun generateRequestId(): String {
        return "req-${System.nanoTime()}-${(100..999).random()}"
    }
    
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}