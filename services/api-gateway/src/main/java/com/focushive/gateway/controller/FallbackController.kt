package com.focushive.gateway.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Fallback Controller for Circuit Breaker Patterns
 * 
 * Provides fallback responses when downstream services are unavailable
 */
@RestController
@RequestMapping("/fallback")
class FallbackController {
    
    @RequestMapping("/identity")
    fun identityFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Identity Service", "Authentication temporarily unavailable"))
    }
    
    @RequestMapping("/focushive")
    fun focushiveFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("FocusHive Backend", "Core services temporarily unavailable"))
    }
    
    @RequestMapping("/music")
    fun musicFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Music Service", "Music features temporarily unavailable"))
    }
    
    @RequestMapping("/notifications")
    fun notificationsFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Notification Service", "Notifications temporarily unavailable"))
    }
    
    @RequestMapping("/chat")
    fun chatFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Chat Service", "Chat features temporarily unavailable"))
    }
    
    @RequestMapping("/analytics")
    fun analyticsFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Analytics Service", "Analytics temporarily unavailable"))
    }
    
    @RequestMapping("/forum")
    fun forumFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Forum Service", "Forum features temporarily unavailable"))
    }
    
    @RequestMapping("/buddy")
    fun buddyFallback(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("Buddy Service", "Buddy features temporarily unavailable"))
    }
    
    private fun createFallbackResponse(serviceName: String, message: String): Map<String, Any> {
        return mapOf(
            "error" to "Service Unavailable",
            "service" to serviceName,
            "message" to message,
            "timestamp" to Instant.now().toString(),
            "status" to 503,
            "fallback" to true,
            "retryAfter" to "30s"
        )
    }
}