package com.focushive.identity.integration;

import com.focushive.identity.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple integration test that tests a basic endpoint without complex dependencies.
 * This test uses @WebMvcTest to load only the web layer and excludes all configuration
 * classes that might cause dependency issues.
 */
@WebMvcTest(
    controllers = HealthController.class,
    excludeAutoConfiguration = {
        // Exclude OAuth2 server configuration that causes dependency issues
        org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = "com.focushive.identity.controller",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.identity.config.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.identity.service.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.focushive.identity.interceptor.*")
    }
)
@ActiveProfiles("test")
class SimpleOAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that the health endpoint works correctly.
     * This is the simplest possible integration test that verifies:
     * - The controller can be loaded
     * - The endpoint returns the expected response
     * - The basic Spring MVC setup works
     */
    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("identity-service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}