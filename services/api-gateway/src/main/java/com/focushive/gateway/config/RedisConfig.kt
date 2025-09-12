package com.focushive.gateway.config

import com.bucket4j.Bucket
import com.bucket4j.BucketConfiguration
import com.bucket4j.redis.jedis.cas.JedisCasBasedProxyManager
import io.github.bucket4j.caffeine.CaffeineProxyManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import reactor.core.publisher.Mono
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.time.Duration

/**
 * Redis Configuration for API Gateway
 * 
 * Provides:
 * - Redis connection configuration
 * - Rate limiting with Bucket4j
 * - Cache configuration
 * - Key resolution for rate limiting
 */
@Configuration
class RedisConfig {
    
    @Value("\${spring.data.redis.host:localhost}")
    private lateinit var redisHost: String
    
    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379
    
    @Value("\${spring.data.redis.timeout:2000ms}")
    private lateinit var redisTimeout: String
    
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val factory = LettuceConnectionFactory(redisHost, redisPort)
        factory.setValidateConnection(true)
        return factory
    }
    
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()
        template.afterPropertiesSet()
        return template
    }
    
    @Bean
    fun jedisPool(): JedisPool {
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 20
            maxIdle = 10
            minIdle = 2
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
        }
        return JedisPool(poolConfig, redisHost, redisPort, 2000)
    }
    
    @Bean
    fun proxyManager(jedisPool: JedisPool): JedisCasBasedProxyManager<String> {
        return JedisCasBasedProxyManager.builderFor(jedisPool)
            .withExpirationAfterWriteStrategy(Duration.ofMinutes(5))
            .build()
    }
    
    /**
     * Key resolver for rate limiting based on user ID or IP address
     */
    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val userId = exchange.request.headers.getFirst("X-User-Id")
            if (!userId.isNullOrBlank()) {
                Mono.just("user:$userId")
            } else {
                val clientIp = getClientIpAddress(exchange.request)
                Mono.just("ip:$clientIp")
            }
        }
    }
    
    /**
     * Key resolver for rate limiting based on API endpoint
     */
    @Bean
    fun pathKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val path = exchange.request.path.value()
            val sanitizedPath = path.replace(Regex("/\\d+"), "/{id}")
            Mono.just("path:$sanitizedPath")
        }
    }
    
    /**
     * Combined key resolver using user and path
     */
    @Bean
    fun combinedKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            val userId = exchange.request.headers.getFirst("X-User-Id")
            val path = exchange.request.path.value()
            val sanitizedPath = path.replace(Regex("/\\d+"), "/{id}")
            
            if (!userId.isNullOrBlank()) {
                Mono.just("user:$userId:path:$sanitizedPath")
            } else {
                val clientIp = getClientIpAddress(exchange.request)
                Mono.just("ip:$clientIp:path:$sanitizedPath")
            }
        }
    }
    
    /**
     * Default Redis rate limiter configuration
     */
    @Bean
    fun redisRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(
            100, // replenishRate: tokens per second
            200, // burstCapacity: max tokens in bucket
            1    // requestedTokens: tokens per request
        )
    }
    
    private fun getClientIpAddress(request: org.springframework.http.server.reactive.ServerHttpRequest): String {
        val headers = listOf(
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Client-IP"
        )
        
        for (header in headers) {
            val ip = request.headers.getFirst(header)
            if (!ip.isNullOrBlank() && !"unknown".equals(ip, ignoreCase = true)) {
                return ip.split(",")[0].trim()
            }
        }
        
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }
}