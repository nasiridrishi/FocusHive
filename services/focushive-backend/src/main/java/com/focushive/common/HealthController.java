package com.focushive.common;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced health check controller with dependency validation.
 * Provides comprehensive health status including external dependencies.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.features.health.enabled", havingValue = "true", matchIfMissing = false)
public class HealthController {
    
    private final IdentityServiceClient identityServiceClient;
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        boolean allHealthy = true;
        
        // Basic service info
        health.put("service", "focushive-backend");
        health.put("timestamp", Instant.now().toString());
        health.put("version", getClass().getPackage().getImplementationVersion());
        
        Map<String, Object> dependencies = new HashMap<>();
        
        // Check database connectivity
        Map<String, Object> dbHealth = checkDatabaseHealth();
        dependencies.put("database", dbHealth);
        if (!"UP".equals(dbHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Check Redis connectivity
        Map<String, Object> redisHealth = checkRedisHealth();
        dependencies.put("redis", redisHealth);
        if (!"UP".equals(redisHealth.get("status"))) {
            allHealthy = false;
        }
        
        // Check Identity Service connectivity
        Map<String, Object> identityHealth = checkIdentityServiceHealth();
        dependencies.put("identity-service", identityHealth);
        // Don't fail overall health if identity service is down (graceful degradation)
        
        health.put("status", allHealthy ? "UP" : "DOWN");
        health.put("dependencies", dependencies);
        
        HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
    
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Basic info
        health.put("service", "focushive-backend");
        health.put("timestamp", Instant.now().toString());
        health.put("version", getClass().getPackage().getImplementationVersion());
        
        // System information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> system = new HashMap<>();
        system.put("processors", runtime.availableProcessors());
        system.put("memory.max", runtime.maxMemory());
        system.put("memory.total", runtime.totalMemory());
        system.put("memory.free", runtime.freeMemory());
        system.put("memory.used", runtime.totalMemory() - runtime.freeMemory());
        health.put("system", system);
        
        // Detailed dependency checks
        Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("database", checkDatabaseHealthDetailed());
        dependencies.put("redis", checkRedisHealthDetailed());
        dependencies.put("identity-service", checkIdentityServiceHealthDetailed());
        
        health.put("dependencies", dependencies);
        
        // Determine overall status
        boolean allCriticalDependenciesUp = 
            "UP".equals(((Map<?, ?>) dependencies.get("database")).get("status")) &&
            "UP".equals(((Map<?, ?>) dependencies.get("redis")).get("status"));
        
        health.put("status", allCriticalDependenciesUp ? "UP" : "DOWN");
        
        HttpStatus status = allCriticalDependenciesUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(health);
    }
    
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
                long responseTime = System.currentTimeMillis() - startTime;
                
                health.put("status", isValid ? "UP" : "DOWN");
                health.put("responseTime", responseTime + "ms");
                
                if (isValid) {
                    health.put("database", connection.getMetaData().getDatabaseProductName());
                }
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkDatabaseHealthDetailed() {
        Map<String, Object> health = checkDatabaseHealth();
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                var metaData = connection.getMetaData();
                health.put("url", metaData.getURL());
                health.put("version", metaData.getDatabaseProductVersion());
                health.put("driver", metaData.getDriverName());
                health.put("driverVersion", metaData.getDriverVersion());
            }
        } catch (Exception e) {
            // Keep this debug log for troubleshooting database connectivity issues
            log.debug("Could not retrieve detailed database information", e);
        }
        
        return health;
    }
    
    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            redisTemplate.opsForValue().set("health-check", "ping");
            String result = (String) redisTemplate.opsForValue().get("health-check");
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.put("status", "ping".equals(result) ? "UP" : "DOWN");
            health.put("responseTime", responseTime + "ms");
            
            // Clean up
            redisTemplate.delete("health-check");
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }
    
    private Map<String, Object> checkRedisHealthDetailed() {
        Map<String, Object> health = checkRedisHealth();
        
        try {
            // Add Redis-specific information
            var info = redisTemplate.getConnectionFactory().getConnection().info();
            health.put("info", info.getProperty("redis_version"));
        } catch (Exception e) {
            // Keep this debug log for troubleshooting Redis connectivity issues
            log.debug("Could not retrieve detailed Redis information", e);
        }
        
        return health;
    }
    
    private Map<String, Object> checkIdentityServiceHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            HealthResponse response = identityServiceClient.checkHealth();
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.put("status", "UP".equals(response.getStatus()) ? "UP" : "DOWN");
            health.put("responseTime", responseTime + "ms");
            health.put("version", response.getVersion());
            
        } catch (Exception e) {
            log.warn("Identity Service health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("note", "Service operating in degraded mode without identity features");
        }
        return health;
    }
    
    private Map<String, Object> checkIdentityServiceHealthDetailed() {
        return checkIdentityServiceHealth(); // Same for now, could be enhanced with more details
    }
}