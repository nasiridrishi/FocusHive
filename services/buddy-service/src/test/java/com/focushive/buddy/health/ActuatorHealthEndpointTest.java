package com.focushive.buddy.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Production test for Actuator health endpoints.
 * Ensures all health check endpoints are properly accessible without authentication.
 *
 * This test verifies the fix for the production issue where /actuator/health
 * returns HTTP 500 due to security misconfiguration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator Health Endpoint Security Tests")
public class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Main health endpoint should be accessible without authentication")
    public void testMainHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v3+json"))
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.components", notNullValue()))
            .andExpect(jsonPath("$.components.db.status", is("UP")))
            .andExpect(jsonPath("$.components.diskSpace.status", is("UP")))
            .andExpect(jsonPath("$.components.ping.status", is("UP")));
    }

    @Test
    @DisplayName("Health liveness endpoint should be accessible without authentication")
    public void testHealthLivenessEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Health readiness endpoint should be accessible without authentication")
    public void testHealthReadinessEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Custom API health endpoint should be accessible without authentication")
    public void testCustomHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.service", is("buddy-service")))
            .andExpect(jsonPath("$.timestamp", notNullValue()))
            .andExpect(jsonPath("$.components.database", is("UP")))
            .andExpect(jsonPath("$.components.matching", is("UP")));
    }

    @Test
    @DisplayName("Actuator info endpoint should be accessible without authentication")
    public void testActuatorInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Health endpoint should work in different profiles")
    public void testHealthEndpointConsistency() throws Exception {
        // Test that health endpoint returns consistent structure
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.status").value(anyOf(is("UP"), is("DOWN"), is("UNKNOWN"))))
            .andExpect(content().string(not(containsString("error"))))
            .andExpect(content().string(not(containsString("exception"))))
            .andExpect(content().string(not(containsString("NoResourceFoundException"))));
    }

    @Test
    @DisplayName("Health endpoint should not require JWT token")
    public void testHealthEndpointWithoutToken() throws Exception {
        // Explicitly test without any authorization header
        mockMvc.perform(get("/actuator/health")
                .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Health endpoint should work with invalid JWT token")
    public void testHealthEndpointWithInvalidToken() throws Exception {
        // Even with an invalid token, health should be accessible
        mockMvc.perform(get("/actuator/health")
                .header("Authorization", "Bearer invalid.jwt.token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("All actuator health subpaths should be accessible")
    public void testAllHealthSubpaths() throws Exception {
        // Test various health subpaths that might be used by monitoring tools
        String[] healthPaths = {
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/actuator/health/db",
            "/actuator/health/diskSpace",
            "/actuator/health/ping"
        };

        for (String path : healthPaths) {
            mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").value(anyOf(is("UP"), is("DOWN"), is("UNKNOWN"))));
        }
    }

    @Test
    @DisplayName("Health endpoint should return proper content type")
    public void testHealthEndpointContentType() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(header().exists("Content-Type"));
    }
}