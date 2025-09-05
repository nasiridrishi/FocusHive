package com.focushive.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.annotation.Profile;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@Profile("security-test")
public class SimpleAuthController {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Hardcoded demo account for testing
    private final String DEMO_USERNAME = "demo_user";
    private final String DEMO_EMAIL = "demo@focushive.com";
    private final String DEMO_PASSWORD_HASH = "$2a$10$bGx1Y7LbI7oZg7qhj8VZF.JVyHrX1St7bV0eQ3D5X.mZrDqNwJmNa"; // Demo123!
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        System.out.println("Login request received: " + loginRequest);
        String username = loginRequest.get("username");
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);
        System.out.println("Password received: " + (password != null ? "***" : "null"));
        
        // Check if it's the demo account
        boolean isValidUser = false;
        if (username != null && username.equals(DEMO_USERNAME)) {
            System.out.println("Checking password for username: " + username);
            isValidUser = passwordEncoder.matches(password, DEMO_PASSWORD_HASH);
            System.out.println("Password match result: " + isValidUser);
        } else if (email != null && email.equals(DEMO_EMAIL)) {
            System.out.println("Checking password for email: " + email);
            isValidUser = passwordEncoder.matches(password, DEMO_PASSWORD_HASH);
            System.out.println("Password match result: " + isValidUser);
        }
        
        if (isValidUser) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("user", Map.of(
                "id", "demo-user-id",
                "username", DEMO_USERNAME,
                "email", DEMO_EMAIL,
                "displayName", "Demo User"
            ));
            response.put("token", "demo-jwt-token-for-testing");
            response.put("refreshToken", "demo-refresh-token");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Invalid credentials"
            ));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Registration disabled in minimal mode. Use demo account."
        ));
    }
    
    @GetMapping(value = "/check", produces = "application/json")
    public ResponseEntity<?> checkAuth() {
        return ResponseEntity.ok(Map.of(
            "authenticated", false,
            "message", "Auth check endpoint"
        ));
    }
    
    @GetMapping("/test-hash")
    public ResponseEntity<?> testHash() {
        String password = "Demo123!";
        String newHash = passwordEncoder.encode(password);
        boolean oldHashMatches = passwordEncoder.matches(password, DEMO_PASSWORD_HASH);
        boolean newHashMatches = passwordEncoder.matches(password, newHash);
        
        return ResponseEntity.ok(Map.of(
            "password", password,
            "newHash", newHash,
            "oldHash", DEMO_PASSWORD_HASH,
            "oldHashMatches", oldHashMatches,
            "newHashMatches", newHashMatches
        ));
    }
}