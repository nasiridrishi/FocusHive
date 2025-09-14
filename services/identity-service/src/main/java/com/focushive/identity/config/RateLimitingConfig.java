package com.focushive.identity.config;

import com.focushive.identity.interceptor.RateLimitingInterceptor;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Configuration for rate limiting functionality.
 * Registers the rate limiting interceptor and configures Redis-based distributed rate limiting.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "focushive.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class RateLimitingConfig implements WebMvcConfigurer {
    
    private final @Lazy RateLimitingInterceptor rateLimitingInterceptor;
    private final JedisConnectionFactory jedisConnectionFactory;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/v1/auth/**")
                .addPathPatterns("/oauth2/**")
                .order(1); // Execute early in the interceptor chain
        
        log.info("Rate limiting interceptor registered for authentication endpoints");
    }
    
    /**
     * Creates a JedisPool for Bucket4j rate limiting.
     * Uses separate pool from Spring Data Redis to avoid interference.
     */
    @Bean
    @ConditionalOnProperty(name = "focushive.rate-limiting.redis.enabled", havingValue = "true", matchIfMissing = true)
    public JedisPool rateLimitingJedisPool() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setNumTestsPerEvictionRun(10);
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
            poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(10));
            poolConfig.setBlockWhenExhausted(false);
            poolConfig.setMaxWait(Duration.ofSeconds(2));
            
            String hostName = jedisConnectionFactory.getHostName();
            int port = jedisConnectionFactory.getPort();
            int database = jedisConnectionFactory.getDatabase();
            String password = jedisConnectionFactory.getPassword();
            
            JedisPool jedisPool;
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, hostName, port, 5000, password, database);
            } else {
                jedisPool = new JedisPool(poolConfig, hostName, port, 5000, null, database);
            }
            
            log.info("Rate limiting JedisPool created - Host: {}:{}, Database: {}", 
                    hostName, port, database);
            
            return jedisPool;
            
        } catch (Exception e) {
            log.error("Failed to create rate limiting JedisPool", e);
            throw new RuntimeException("Failed to initialize rate limiting Redis connection", e);
        }
    }
    
    /**
     * Creates a JedisBasedProxyManager for Bucket4j distributed rate limiting.
     */
    @Bean
    @ConditionalOnProperty(name = "focushive.rate-limiting.redis.enabled", havingValue = "true", matchIfMissing = true)
    public JedisBasedProxyManager jedisProxyManager(JedisPool rateLimitingJedisPool) {
        try {
            JedisBasedProxyManager proxyManager = JedisBasedProxyManager.builderFor(rateLimitingJedisPool)
                    // TODO: Add expiration strategy when available in Bucket4j version
                    .build();
            
            log.info("Bucket4j JedisBasedProxyManager created successfully");
            return proxyManager;
            
        } catch (Exception e) {
            log.error("Failed to create JedisBasedProxyManager", e);
            throw new RuntimeException("Failed to initialize Bucket4j proxy manager", e);
        }
    }
}