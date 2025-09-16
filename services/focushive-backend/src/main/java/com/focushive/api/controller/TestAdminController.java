package com.focushive.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Test Admin Controller for Security Configuration Testing
 * This controller provides admin endpoints for testing security configuration
 * Only enabled in test and development profiles
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Profile({"test", "dev", "security-test"})
public class TestAdminController {

    /**
     * Test admin users endpoint
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers() {
        log.info("Test admin users endpoint called");

        Map<String, Object> response = Map.of(
                "message", "Admin users endpoint (test mode)",
                "users", List.of(
                        Map.of("id", 1, "username", "admin", "role", "ADMIN"),
                        Map.of("id", 2, "username", "user", "role", "USER")
                ),
                "total", 2
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test admin system status endpoint
     */
    @GetMapping("/system/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        log.info("Test admin system status endpoint called");

        Map<String, Object> response = Map.of(
                "message", "System status endpoint (test mode)",
                "status", "healthy",
                "services", Map.of(
                        "database", "connected",
                        "redis", "connected",
                        "identity-service", "available"
                )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test admin configuration endpoint
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        log.info("Test admin config endpoint called");

        Map<String, Object> response = Map.of(
                "message", "Admin configuration endpoint (test mode)",
                "security", Map.of(
                        "csrf_enabled", false,
                        "rate_limiting", "enabled",
                        "cors_origins", "localhost"
                ),
                "features", Map.of(
                        "hives", "enabled",
                        "timers", "enabled",
                        "analytics", "enabled"
                )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test admin action endpoint (POST)
     */
    @PostMapping("/actions/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> performTestAction(@RequestBody Map<String, Object> actionRequest) {
        log.info("Test admin action endpoint called with action: {}", actionRequest.get("action"));

        Map<String, String> response = Map.of(
                "message", "Admin action completed (test mode)",
                "action", actionRequest.getOrDefault("action", "unknown").toString(),
                "result", "success"
        );

        return ResponseEntity.ok(response);
    }
}