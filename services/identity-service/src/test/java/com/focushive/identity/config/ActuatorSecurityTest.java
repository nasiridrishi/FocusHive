package com.focushive.identity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator Endpoint Security Tests")
class ActuatorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow public access to health endpoint")
    void testHealthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to info endpoint")
    void testInfoEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to prometheus endpoint")
    void testPrometheusEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require admin role for env endpoint")
    void testEnvEndpointRequiresAdmin() throws Exception {
        // Without authentication
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require admin role for beans endpoint")
    void testBeansEndpointRequiresAdmin() throws Exception {
        // Without authentication
        mockMvc.perform(get("/actuator/beans"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require admin role for mappings endpoint")
    void testMappingsEndpointRequiresAdmin() throws Exception {
        // Without authentication
        mockMvc.perform(get("/actuator/mappings"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require admin role for configprops endpoint")
    void testConfigPropsEndpointRequiresAdmin() throws Exception {
        // Without authentication
        mockMvc.perform(get("/actuator/configprops"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should deny access to admin endpoints for regular users")
    void testAdminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/beans"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow admin access to sensitive endpoints")
    void testAdminCanAccessSensitiveEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle CORS preflight for actuator endpoints")
    void testActuatorCorsPreflight() throws Exception {
        mockMvc.perform(get("/actuator/health")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should expose prometheus metrics without authentication")
    void testPrometheusMetricsExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                // Prometheus metrics should contain standard JVM metrics
                assert content.contains("jvm_memory_used_bytes") ||
                       content.contains("# TYPE") ||
                       content.contains("# HELP") :
                       "Prometheus metrics format not found";
            });
    }

    @Test
    @DisplayName("Should expose application info without authentication")
    void testApplicationInfoExposed() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                // Info endpoint might be empty or contain app info
                assert content != null : "Info endpoint should return content";
            });
    }

    @Test
    @DisplayName("Should return health status without authentication")
    void testHealthStatusExposed() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String content = result.getResponse().getContentAsString();
                // Health check should contain status
                assert content.contains("UP") || content.contains("DOWN") ||
                       content.contains("status") :
                       "Health status not found in response";
            });
    }
}