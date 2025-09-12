package com.focushive.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

/**
 * FocusHive API Gateway Application
 * 
 * Centralized API gateway providing:
 * - Request routing to microservices
 * - JWT authentication and authorization
 * - Rate limiting and throttling
 * - CORS management
 * - Security headers
 * - Request/response logging
 * - Circuit breaker patterns
 */
@SpringBootApplication
@EnableCaching
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}