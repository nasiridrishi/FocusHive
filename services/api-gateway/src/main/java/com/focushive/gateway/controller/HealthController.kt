package com.focushive.gateway.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Health Check Controller for API Gateway
 * 
 * Provides health status for the gateway and its dependencies
 */
@RestController
@RequestMapping("/health")
class HealthController @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>
) {
    
    @GetMapping
    fun health(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.fromCallable {
            val health = mutableMapOf<String, Any>(
                "status" to "UP",
                "timestamp" to Instant.now().toString(),
                "service" to "api-gateway",
                "version" to "1.0.0"
            )
            
            // Check Redis connectivity
            val redisHealth = checkRedis()
            health["redis"] = redisHealth
            
            // Check overall health
            val overallStatus = if (redisHealth["status"] == "UP") "UP" else "DEGRADED"
            health["status"] = overallStatus
            
            ResponseEntity.ok(health.toMap())
        }.onErrorResume { throwable ->
            Mono.just(
                ResponseEntity.status(503).body(
                    mapOf(
                        "status" to "DOWN",
                        "timestamp" to Instant.now().toString(),
                        "service" to "api-gateway",
                        "error" to throwable.message
                    )
                )
            )
        }
    }
    
    @GetMapping("/detailed")
    fun detailedHealth(): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.fromCallable {
            val health = mutableMapOf<String, Any>(
                "status" to "UP",
                "timestamp" to Instant.now().toString(),
                "service" to "api-gateway",
                "version" to "1.0.0"
            )
            
            // Check all dependencies
            val dependencies = mutableMapOf<String, Any>()
            
            // Redis health
            dependencies["redis"] = checkRedis()
            
            // Downstream services health (simplified check)
            dependencies["services"] = checkDownstreamServices()
            
            health["dependencies"] = dependencies
            
            // Determine overall status
            val allUp = dependencies.values.all { 
                (it as? Map<*, *>)?.get("status") == "UP" 
            }
            health["status"] = if (allUp) "UP" else "DEGRADED"
            
            ResponseEntity.ok(health.toMap())
        }.onErrorResume { throwable ->
            Mono.just(
                ResponseEntity.status(503).body(
                    mapOf(
                        "status" to "DOWN",
                        "timestamp" to Instant.now().toString(),
                        "service" to "api-gateway",
                        "error" to throwable.message
                    )
                )
            )
        }
    }
    
    private fun checkRedis(): Map<String, Any> {
        return try {
            redisTemplate.opsForValue().set("health:check", "test", 10)
            val result = redisTemplate.opsForValue().get("health:check")
            if (result == "test") {
                mapOf(
                    "status" to "UP",
                    "responseTime" to measureResponseTime { 
                        redisTemplate.opsForValue().get("health:check")
                    }
                )
            } else {
                mapOf(
                    "status" to "DOWN",
                    "error" to "Redis test failed"
                )
            }
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to e.message
            )
        }
    }
    
    private fun checkDownstreamServices(): Map<String, Any> {
        val services = mapOf(
            "identity-service" to "http://localhost:8081/actuator/health",
            "focushive-backend" to "http://localhost:8080/actuator/health",
            "music-service" to "http://localhost:8082/actuator/health",
            "notification-service" to "http://localhost:8083/actuator/health",
            "chat-service" to "http://localhost:8084/actuator/health",
            "analytics-service" to "http://localhost:8085/actuator/health",
            "forum-service" to "http://localhost:8086/actuator/health",
            "buddy-service" to "http://localhost:8087/actuator/health"
        )
        
        val results = mutableMapOf<String, Any>()
        var upCount = 0
        
        services.forEach { (serviceName, _) ->
            // Simplified check - in production, you'd make HTTP calls to check actual health
            results[serviceName] = mapOf(
                "status" to "UNKNOWN",
                "message" to "Health check not implemented"
            )
        }
        
        return mapOf(
            "total" to services.size,
            "up" to upCount,
            "down" to (services.size - upCount),
            "services" to results
        )
    }
    
    private fun measureResponseTime(block: () -> Any): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}