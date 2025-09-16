package com.focushive.notification.controller;

import com.focushive.notification.monitoring.ConnectionPoolMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Monitoring endpoints for service health and metrics.
 * Provides access to connection pool and other monitoring data.
 */
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Service monitoring and metrics endpoints")
public class MonitoringController {

    private final ConnectionPoolMonitor connectionPoolMonitor;

    /**
     * Get connection pool statistics.
     */
    @GetMapping("/pool")
    @Operation(summary = "Get connection pool statistics",
            description = "Returns current HikariCP connection pool metrics and status")
    public ResponseEntity<Map<String, Object>> getPoolStatistics() {
        Map<String, Object> stats = connectionPoolMonitor.getPoolStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get connection pool health status.
     */
    @GetMapping("/pool/health")
    @Operation(summary = "Get connection pool health",
            description = "Returns health status of the connection pool")
    public ResponseEntity<Health> getPoolHealth() {
        Health health = connectionPoolMonitor.health();
        return ResponseEntity.ok(health);
    }
}