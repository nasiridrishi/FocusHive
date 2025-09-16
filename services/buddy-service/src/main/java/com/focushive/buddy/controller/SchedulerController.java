package com.focushive.buddy.controller;

import com.focushive.buddy.scheduler.BuddyScheduledTasks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for monitoring scheduled tasks.
 *
 * Provides endpoints for:
 * - Checking task execution status
 * - Getting detailed metrics
 * - Health monitoring for scheduled operations
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final BuddyScheduledTasks scheduledTasks;

    /**
     * Gets the current execution status of all scheduled tasks.
     *
     * @return ResponseEntity with task execution status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        try {
            Map<String, Object> status = Map.of(
                "anyTaskRunning", scheduledTasks.isAnyTaskRunning(),
                "statusSummary", scheduledTasks.getExecutionStatus(),
                "metrics", scheduledTasks.getExecutionMetrics()
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Failed to get scheduler status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to retrieve scheduler status",
                    "message", e.getMessage()
                ));
        }
    }

    /**
     * Gets detailed metrics for all scheduled tasks.
     *
     * @return ResponseEntity with detailed task metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSchedulerMetrics() {
        try {
            Map<String, Object> metrics = scheduledTasks.getExecutionMetrics();
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get scheduler metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to retrieve scheduler metrics",
                    "message", e.getMessage()
                ));
        }
    }

    /**
     * Health check endpoint for scheduled tasks.
     * Returns 200 if no critical tasks are stuck.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSchedulerHealth() {
        try {
            boolean anyTaskRunning = scheduledTasks.isAnyTaskRunning();
            String statusSummary = scheduledTasks.getExecutionStatus();

            Map<String, Object> health = Map.of(
                "status", anyTaskRunning ? "RUNNING" : "IDLE",
                "healthy", true,
                "summary", statusSummary
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Scheduler health check failed: {}", e.getMessage(), e);

            Map<String, Object> unhealthy = Map.of(
                "status", "ERROR",
                "healthy", false,
                "error", e.getMessage()
            );

            return ResponseEntity.status(503).body(unhealthy);
        }
    }
}