package com.focushive.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

class LoginRequest {
    private String email;
    private String password;
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials) {
        String email = credentials.getEmail();
        String password = credentials.getPassword();
        
        // Simple demo authentication
        if ("demo@focushive.com".equals(email) && "demo123".equals(password)) {
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", "demo-jwt-token-12345");
            response.put("refreshToken", "demo-refresh-token-67890");
            response.put("user", Map.of(
                "id", "demo-user-id",
                "email", email,
                "username", "demo",
                "displayName", "Demo User"
            ));
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Demo endpoints working"));
    }
}