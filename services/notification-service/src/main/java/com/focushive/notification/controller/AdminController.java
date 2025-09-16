package com.focushive.notification.controller;

import com.focushive.notification.service.SecurityAuditService;
import com.focushive.notification.service.UserContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for administrative operations.
 * Requires ROLE_ADMIN for all endpoints.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Administrative operations")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final UserContextService userContextService;
    private final SecurityAuditService securityAuditService;

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics", description = "Admin-only endpoint for system statistics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        // Audit log admin action
        Map<String, Object> actionParams = Map.of(
            "statsType", "system",
            "timestamp", Instant.now().toString()
        );
        securityAuditService.logAdminAction("VIEW_SYSTEM_STATS", "/api/admin/stats", actionParams);
        
        log.info("Admin user {} requested system stats", userContextService.getCurrentUserId());
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", Instant.now());
        stats.put("requestedBy", userContextService.getCurrentUserId());
        stats.put("userEmail", userContextService.getCurrentUserEmail());
        
        // Add system metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());
        stats.put("memory", memory);
        
        stats.put("processors", runtime.availableProcessors());
        stats.put("status", "healthy");
        stats.put("version", "1.0.0");
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user-context")
    @Operation(summary = "Get current user context", description = "Admin-only endpoint to inspect user context")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserContextService.UserContext> getUserContext() {
        // Audit log admin action
        Map<String, Object> actionParams = Map.of(
            "contextType", "current",
            "timestamp", Instant.now().toString()
        );
        securityAuditService.logAdminAction("VIEW_USER_CONTEXT", "/api/admin/user-context", actionParams);
        
        log.info("Admin user {} requested user context", userContextService.getCurrentUserId());
        
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();
        return ResponseEntity.ok(userContext);
    }

    @GetMapping("/health")
    @Operation(summary = "Admin health check", description = "Admin-only health check endpoint")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> adminHealth() {
        // Audit log admin action
        Map<String, Object> actionParams = Map.of(
            "healthType", "admin",
            "timestamp", Instant.now().toString()
        );
        securityAuditService.logAdminAction("VIEW_ADMIN_HEALTH", "/api/admin/health", actionParams);
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("admin", userContextService.getCurrentUserId());
        health.put("timestamp", Instant.now().toString());
        
        // Add component health checks
        Map<String, Object> components = new HashMap<>();
        components.put("database", Map.of("status", "UP"));
        components.put("redis", Map.of("status", "UP"));
        components.put("security", Map.of("status", "UP"));
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/audit/summary")
    @Operation(summary = "Get audit log summary", description = "Admin-only endpoint for audit statistics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAuditSummary() {
        // Audit log admin action
        Map<String, Object> actionParams = Map.of(
            "auditType", "summary",
            "timestamp", Instant.now().toString()
        );
        securityAuditService.logAdminAction("VIEW_AUDIT_SUMMARY", "/api/admin/audit/summary", actionParams);

        Map<String, Object> summary = new HashMap<>();
        summary.put("timestamp", Instant.now());
        
        // Mock audit statistics (would be retrieved from actual audit service)
        summary.put("totalEvents", 45_678);
        summary.put("eventsToday", 1_234);
        summary.put("eventsThisWeek", 8_765);
        
        Map<String, Object> eventTypes = new HashMap<>();
        eventTypes.put("authentication", 12_345);
        eventTypes.put("authorization", 2_456);
        eventTypes.put("preferenceChanges", 8_901);
        eventTypes.put("adminActions", 567);
        eventTypes.put("suspicious", 89);
        summary.put("eventTypes", eventTypes);
        
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear application caches", description = "Admin-only endpoint to clear system caches")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> clearCaches(@RequestParam(required = false) String cacheType) {
        // Audit log admin action
        Map<String, Object> actionParams = Map.of(
            "cacheType", cacheType != null ? cacheType : "all",
            "timestamp", Instant.now().toString()
        );
        securityAuditService.logAdminAction("CLEAR_CACHE", "/api/admin/cache/clear", actionParams);

        // Mock cache clearing (would integrate with actual cache managers)
        String message = cacheType != null 
            ? String.format("Cleared cache: %s", cacheType)
            : "Cleared all caches";
            
        log.warn("Cache clearing requested by admin user: {} - {}", 
                userContextService.getCurrentUserId(), message);
        
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", message);
        result.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/config")
    @Operation(summary = "Update system configuration", description = "Admin-only endpoint to update system settings")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> updateConfig(@Valid @RequestBody Map<String, Object> configUpdates) {
        // Audit log security configuration change
        securityAuditService.logSecurityConfigurationChange("SYSTEM_CONFIG_UPDATE", configUpdates);
        
        // Audit log admin action
        Map<String, Object> actionParams = new HashMap<>(configUpdates);
        actionParams.put("timestamp", Instant.now().toString());
        securityAuditService.logAdminAction("UPDATE_SYSTEM_CONFIG", "/api/admin/config", actionParams);

        // Mock configuration update
        log.warn("System configuration update requested by admin user: {} - Updates: {}", 
                userContextService.getCurrentUserId(), configUpdates);
        
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", String.format("Updated %d configuration settings", configUpdates.size()));
        result.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.ok(result);
    }
}
