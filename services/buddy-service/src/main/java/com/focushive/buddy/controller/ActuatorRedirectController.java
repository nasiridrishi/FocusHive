package com.focushive.buddy.controller;

import com.focushive.buddy.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to handle actuator endpoint requests on the main application port.
 * Actuator endpoints are served on the management port (8088), not the main port (8087).
 * This controller provides proper error responses instead of NoResourceFoundException.
 */
@RestController
@RequestMapping("/actuator")
public class ActuatorRedirectController {

    @Value("${management.server.port:8088}")
    private int managementPort;

    /**
     * Handle all actuator endpoint requests with informative error message.
     * This prevents Spring from treating these as static resources.
     */
    @GetMapping("/**")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleActuatorRequest(HttpServletRequest request) {
        String path = request.getRequestURI();

        Map<String, Object> info = new HashMap<>();
        info.put("error", "Actuator endpoints are not available on this port");
        info.put("management_port", managementPort);
        info.put("requested_path", path);
        info.put("correct_url", "http://localhost:" + managementPort + path);
        info.put("note", "Actuator endpoints are served on the management port for security isolation");

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
            .success(false)
            .message("Actuator endpoints are available on management port " + managementPort)
            .data(info)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Specific handler for health endpoint to provide clearer guidance.
     */
    @GetMapping({"/health", "/health/**"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleHealthRequest(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();
        info.put("note", "Health check endpoints are available on two locations:");
        info.put("application_health", "http://localhost:8087/api/v1/health");
        info.put("actuator_health", "http://localhost:" + managementPort + "/actuator/health");
        info.put("actuator_liveness", "http://localhost:" + managementPort + "/actuator/health/liveness");
        info.put("actuator_readiness", "http://localhost:" + managementPort + "/actuator/health/readiness");

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
            .success(false)
            .message("Use /api/v1/health on this port or /actuator/health on port " + managementPort)
            .data(info)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}