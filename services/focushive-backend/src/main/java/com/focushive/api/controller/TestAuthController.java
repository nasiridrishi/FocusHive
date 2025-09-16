package com.focushive.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test Authentication Controller for Security Configuration Testing
 * This controller provides endpoints for testing security configuration
 * Only enabled in test and development profiles
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Profile({"test", "dev", "security-test"})
public class TestAuthController {

    /**
     * Test login endpoint for security testing
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        log.info("Test login endpoint called with username: {}", loginRequest.get("username"));

        // Simple test implementation - always return success for testing
        Map<String, Object> response = Map.of(
                "message", "Login successful (test mode)",
                "username", loginRequest.getOrDefault("username", "test"),
                "token", "test-jwt-token"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test register endpoint for security testing
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> registerRequest) {
        log.info("Test register endpoint called with username: {}", registerRequest.get("username"));

        // Simple test implementation - always return success for testing
        Map<String, Object> response = Map.of(
                "message", "Registration successful (test mode)",
                "username", registerRequest.getOrDefault("username", "test"),
                "email", registerRequest.getOrDefault("email", "test@example.com")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Test logout endpoint for security testing
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        log.info("Test logout endpoint called");

        Map<String, String> response = Map.of(
                "message", "Logout successful (test mode)"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test CSRF token endpoint (when CSRF is enabled)
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Map<String, String>> getCsrfToken() {
        log.info("Test CSRF token endpoint called");

        // This would normally return the actual CSRF token from the repository
        // For testing purposes, return a placeholder
        Map<String, String> response = Map.of(
                "token", "test-csrf-token",
                "headerName", "X-CSRF-TOKEN"
        );

        return ResponseEntity.ok(response);
    }
}