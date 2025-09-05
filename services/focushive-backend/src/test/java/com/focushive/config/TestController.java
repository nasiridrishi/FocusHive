package com.focushive.config;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Test controller for security testing with protected endpoints.
 */
@RestController
@RequestMapping("/api/v1")
@TestComponent
public class TestController {
    
    @GetMapping(value = "/users/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getUserProfile() {
        return ResponseEntity.ok(Map.of(
            "id", "test-user",
            "username", "testuser", 
            "email", "test@example.com"
        ));
    }
    
    @GetMapping(value = "/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAdminUsers() {
        return ResponseEntity.ok(Map.of(
            "users", "admin data",
            "total", 10
        ));
    }
    
    @GetMapping(value = "/actuator/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP"
        ));
    }
}