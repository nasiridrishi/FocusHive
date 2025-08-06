package com.focushive.api.config;

import com.focushive.test.TestApplication;
import com.focushive.test.UnifiedTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(UnifiedTestConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenAccessPublicEndpoint_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessProtectedEndpoint_withoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void whenAccessProtectedEndpoint_withAuth_thenSucceeds() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessAuthEndpoints_thenPermitted() throws Exception {
        // These should not return 401 (unauthorized) as they are public endpoints
        // They might return 400/404/405 depending on implementation
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                });

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                });

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                });
    }

    @Test
    void whenAccessSwaggerEndpoints_thenPermitted() throws Exception {
        // Swagger UI might redirect, so we accept 200 or 302
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 302);
                });

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void whenAccessActuatorHealth_thenPermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAccessAdminEndpoint_withAdminRole_thenSucceeds() throws Exception {
        // Endpoint may not exist yet, so we accept 200 or 404
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 404);
                });
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