package com.focushive.buddy.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis connectivity
 * Verifies Redis is accessible and can perform basic operations
 */
@Component("redis")
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            return checkRedisHealth();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkRedisHealth() {
        long startTime = System.currentTimeMillis();

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            // Perform a simple PING command
            String pong = connection.ping();
            long responseTime = System.currentTimeMillis() - startTime;

            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("database", "Redis")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("status", "Available")
                        .build();
            }

            return Health.down()
                    .withDetail("error", "Unexpected response from Redis: " + pong)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Cannot connect to Redis: " + e.getMessage())
                    .build();
        }
    }
}