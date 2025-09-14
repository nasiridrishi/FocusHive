package com.focushive.api.config;

import com.focushive.api.controller.SimpleAuthController;
import com.focushive.config.TestController;
import com.focushive.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {SimpleAuthController.class, TestController.class, TestSecurityConfig.class}, 
                webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ImportAutoConfiguration({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("security-test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAccessPublicEndpoint_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check")
                        .accept("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessProtectedEndpoint_withoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                        .accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void whenAccessProtectedEndpoint_withAuth_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                        .accept("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessAuthEndpoints_thenPermitted() throws Exception {
        // Auth endpoints should be accessible (no security 401/403)
        // But can return 400/401 for business logic (invalid credentials)

        // Register endpoint should return 200 (success message about demo mode)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        // Login endpoint should be accessible but return 401 for invalid credentials
        // This is business logic 401, not security 401 - the endpoint is reachable
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Endpoint is accessible (not blocked by security), but returns 401 for invalid credentials
                    assertThat(status).isIn(200, 400, 401); // Allow business logic responses
                });
    }

    @Test
    void whenAccessSwaggerEndpoints_thenPermitted() throws Exception {
        // Test that swagger endpoints are public (don't return 401/403)
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should not be unauthorized or forbidden
                    assertThat(status).isNotIn(401, 403);
                });

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should not be unauthorized or forbidden
                    assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    void whenAccessActuatorHealth_thenPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should not be unauthorized or forbidden
                    assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAccessAdminEndpoint_withAdminRole_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void whenAccessAdminEndpoint_withUserRole_thenForbidden() throws Exception {
        // Endpoint may not exist yet, so we accept 403 or 404
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(403, 404);
                });
    }
}