package com.focushive.notification.controller;

import com.focushive.notification.monitoring.NotificationMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for exposing custom business metrics.
 * Provides endpoints for monitoring and observability.
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Custom business metrics endpoints")
public class MetricsController {

    private final NotificationMetricsService metricsService;

    /**
     * Get basic metrics (public endpoint).
     */
    @GetMapping
    @Operation(summary = "Get basic metrics", description = "Returns basic service metrics")
    @ApiResponse(responseCode = "200", description = "Basic metrics retrieved successfully")
    public ResponseEntity<Map<String, Object>> getBasicMetrics() {
        log.debug("Fetching basic metrics");

        NotificationMetricsService.MetricsSummary summary = metricsService.getMetricsSummary();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("status", "UP");
        metrics.put("notificationsSent", summary.notificationsSent);
        metrics.put("notificationsFailed", summary.notificationsFailed);
        metrics.put("emailsSent", summary.emailsSent);
        metrics.put("activeNotifications", summary.activeNotifications);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get metrics summary.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get metrics summary", description = "Returns a summary of notification service metrics")
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    @PreAuthorize("hasAnyRole('ADMIN', 'MONITOR')")
    public ResponseEntity<NotificationMetricsService.MetricsSummary> getMetricsSummary() {
        log.debug("Fetching metrics summary");
        NotificationMetricsService.MetricsSummary summary = metricsService.getMetricsSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get cache metrics.
     */
    @GetMapping("/cache")
    @Operation(summary = "Get cache metrics", description = "Returns cache performance metrics")
    @ApiResponse(responseCode = "200", description = "Cache metrics retrieved successfully")
    @PreAuthorize("hasAnyRole('ADMIN', 'MONITOR')")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        log.debug("Fetching cache metrics");

        Map<String, Object> cacheMetrics = new HashMap<>();
        // Cache metrics would need to be exposed separately by the service
        cacheMetrics.put("templateCacheHitRatio", 0.0);
        cacheMetrics.put("templateCacheHitRatioPercentage", "0.00%");

        return ResponseEntity.ok(cacheMetrics);
    }

    /**
     * Get notification metrics.
     */
    @GetMapping("/notifications")
    @Operation(summary = "Get notification metrics", description = "Returns notification delivery metrics")
    @ApiResponse(responseCode = "200", description = "Notification metrics retrieved successfully")
    @PreAuthorize("hasAnyRole('ADMIN', 'MONITOR')")
    public ResponseEntity<Map<String, Object>> getNotificationMetrics() {
        log.debug("Fetching notification metrics");

        NotificationMetricsService.MetricsSummary summary = metricsService.getMetricsSummary();

        Map<String, Object> notificationMetrics = new HashMap<>();
        notificationMetrics.put("sent", summary.notificationsSent);
        notificationMetrics.put("failed", summary.notificationsFailed);
        notificationMetrics.put("created", summary.notificationsCreated);
        notificationMetrics.put("active", summary.activeNotifications);
        notificationMetrics.put("pendingDigest", summary.pendingDigestNotifications);
        notificationMetrics.put("emailsSent", summary.emailsSent);
        notificationMetrics.put("emailsFailed", summary.emailsFailed);

        // Calculate success rate
        double total = summary.notificationsSent + summary.notificationsFailed;
        double successRate = total > 0 ? (summary.notificationsSent / (double)total) * 100 : 100.0;
        notificationMetrics.put("successRate", String.format("%.2f%%", successRate));

        return ResponseEntity.ok(notificationMetrics);
    }

    /**
     * Get security metrics.
     */
    @GetMapping("/security")
    @Operation(summary = "Get security metrics", description = "Returns security-related metrics")
    @ApiResponse(responseCode = "200", description = "Security metrics retrieved successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecurityMetrics() {
        log.debug("Fetching security metrics");

        NotificationMetricsService.MetricsSummary summary = metricsService.getMetricsSummary();

        Map<String, Object> securityMetrics = new HashMap<>();
        securityMetrics.put("rateLimitViolations", summary.rateLimitViolations);
        securityMetrics.put("totalUsers", summary.totalUsers);

        return ResponseEntity.ok(securityMetrics);
    }

    /**
     * Get performance metrics.
     */
    @GetMapping("/performance")
    @Operation(summary = "Get performance metrics", description = "Returns performance-related metrics")
    @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully")
    @PreAuthorize("hasAnyRole('ADMIN', 'MONITOR')")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        log.debug("Fetching performance metrics");

        NotificationMetricsService.MetricsSummary summary = metricsService.getMetricsSummary();

        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("activeNotifications", summary.activeNotifications);
        performanceMetrics.put("pendingDigestNotifications", summary.pendingDigestNotifications);
        performanceMetrics.put("templatesRendered", summary.templatesRendered);
        performanceMetrics.put("digestsProcessed", summary.digestsProcessed);

        return ResponseEntity.ok(performanceMetrics);
    }

    /**
     * Record a custom event for testing.
     */
    @GetMapping("/test/record-event")
    @Operation(summary = "Record test event", description = "Records a test notification event for metrics testing")
    @ApiResponse(responseCode = "200", description = "Event recorded successfully")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> recordTestEvent() {
        log.info("Recording test notification event");

        // Recording metrics would need Timer.Sample objects
        // For now, just increment counters
        metricsService.incrementNotificationsSent();
        metricsService.incrementEmailsSent();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test event recorded in metrics");

        return ResponseEntity.ok(response);
    }
}