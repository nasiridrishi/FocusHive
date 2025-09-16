package com.focushive.identity.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

import static java.time.Duration.ofMinutes;

/**
 * Separate configuration for Redis-based rate limiting beans.
 * This avoids circular dependencies by separating Redis configuration from WebMvcConfigurer.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "focushive.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Profile("!test")
public class RateLimitingRedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

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

            JedisPool jedisPool;
            if (redisPassword != null && !redisPassword.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 5000, redisPassword, redisDatabase);
            } else {
                jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 5000, null, redisDatabase);
            }

            log.info("Rate limiting JedisPool created - Host: {}:{}, Database: {}",
                    redisHost, redisPort, redisDatabase);

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
    public JedisBasedProxyManager<String> jedisProxyManager(JedisPool rateLimitingJedisPool) {
        try {
            JedisBasedProxyManager<String> proxyManager = JedisBasedProxyManager.builderFor(rateLimitingJedisPool)
                    .withKeyMapper(Mapper.STRING)
                    .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofMinutes(60)))
                    .build();

            log.info("Bucket4j JedisBasedProxyManager created successfully");
            return proxyManager;

        } catch (Exception e) {
            log.error("Failed to create JedisBasedProxyManager", e);
            throw new RuntimeException("Failed to initialize Bucket4j proxy manager", e);
        }
    }
}