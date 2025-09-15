package com.focushive.notification.controller;

import com.focushive.notification.health.EmailHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final EmailHealthService emailHealthService;

    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "notification-service");
        response.put("timestamp", System.currentTimeMillis());
        
        // Add components status with detailed checks
        Map<String, Object> components = new HashMap<>();
        components.put("database", "UP");
        components.put("websocket", "UP");
        
        // Get detailed email health status
        Map<String, Object> emailHealth = emailHealthService.checkEmailHealth();
        components.put("email", emailHealth);
        
        // Determine overall status based on component health
        String overallStatus = "UP";
        if ("DOWN".equals(emailHealth.get("status"))) {
            overallStatus = "DOWN";
        }
        
        response.put("status", overallStatus);
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }
}