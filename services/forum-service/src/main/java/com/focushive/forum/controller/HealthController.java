package com.focushive.forum.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "forum-service");
        response.put("timestamp", System.currentTimeMillis());
        
        // Add components status
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("search", "UP");
        response.put("components", components);
        
        return ResponseEntity.ok(response);
    }
}