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
 * Test to ensure actuator endpoints are not treated as static resources
 * and are properly handled regardless of port configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator Endpoint Routing Tests")
public class ActuatorRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Actuator health endpoint should not be treated as static resource on main port")
    public void testActuatorHealthNotStaticResource() throws Exception {
        // When accessing /actuator/health on the main application port
        // It should either redirect to management port or return proper error
        // But NOT return a NoResourceFoundException for static resources
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isNotFound()) // 404 is acceptable
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                // Should NOT contain static resource error
                if (content.contains("NoResourceFoundException") ||
                    content.contains("No static resource")) {
                    throw new AssertionError("Actuator endpoint treated as static resource");
                }
            });
    }

    @Test
    @DisplayName("Actuator paths should be excluded from static resource handling")
    public void testActuatorPathsExcludedFromStaticResources() throws Exception {
        // Test various actuator paths to ensure none are treated as static resources
        String[] actuatorPaths = {
            "/actuator",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/actuator/info",
            "/actuator/metrics"
        };

        for (String path : actuatorPaths) {
            mockMvc.perform(get(path))
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    if (content.contains("No static resource")) {
                        throw new AssertionError("Path " + path + " incorrectly handled as static resource");
                    }
                });
        }
    }

    @Test
    @DisplayName("Custom health endpoint should work on main port")
    public void testCustomHealthEndpointOnMainPort() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")))
            .andExpect(jsonPath("$.service", is("buddy-service")));
    }
}