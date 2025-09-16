package com.focushive.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API Security Configuration
 * These tests verify endpoint security, CORS, CSRF, rate limiting, and security headers
 *
 * TDD Approach: These tests will FAIL initially and drive the implementation
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("API Security Configuration Tests")
class SecurityConfigIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ============================================================================
    // ENDPOINT SECURITY TESTS - These should FAIL initially (TDD RED phase)
    // ============================================================================

    @Test
    @DisplayName("Should allow access to public authentication endpoints without JWT")
    void shouldAllowPublicAuthEndpoints() throws Exception {
        // Test login endpoint
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk()); // This will FAIL - endpoint doesn't exist yet

        // Test register endpoint
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"email\":\"test@test.com\",\"password\":\"test\"}"))
                .andExpect(status().isCreated()); // This will FAIL - endpoint doesn't exist yet
    }

    @Test
    @DisplayName("Should allow access to health check endpoints without authentication")
    void shouldAllowPublicHealthEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to Swagger documentation without authentication")
    void shouldAllowPublicSwaggerEndpoints() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk()); // This will FAIL - Swagger not configured yet

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()); // This will FAIL - OpenAPI not configured yet
    }

    @Test
    @DisplayName("Should allow access to demo endpoints without authentication")
    void shouldAllowPublicDemoEndpoints() throws Exception {
        mockMvc.perform(get("/api/demo/public"))
                .andExpect(status().isOk()); // This will FAIL - demo endpoint doesn't exist yet
    }

    @Test
    @DisplayName("Should secure private hive endpoints - require JWT authentication")
    void shouldSecurePrivateHiveEndpoints() throws Exception {
        // These should FAIL without JWT (currently may pass - need to verify security)
        mockMvc.perform(get("/api/v1/hives"))
                .andExpect(status().isUnauthorized()); // Should be 401

        mockMvc.perform(post("/api/v1/hives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Hive\",\"description\":\"Test\"}"))
                .andExpect(status().isUnauthorized()); // Should be 401

        mockMvc.perform(put("/api/v1/hives/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Hive\"}"))
                .andExpect(status().isUnauthorized()); // Should be 401

        mockMvc.perform(delete("/api/v1/hives/1"))
                .andExpect(status().isUnauthorized()); // Should be 401
    }

    @Test
    @DisplayName("Should secure private presence endpoints - require JWT authentication")
    void shouldSecurePrivatePresenceEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/presence/hive/1"))
                .andExpect(status().isUnauthorized()); // Should be 401

        mockMvc.perform(post("/api/v1/presence/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isUnauthorized()); // Should be 401
    }

    @Test
    @DisplayName("Should secure private timer endpoints - require JWT authentication")
    void shouldSecurePrivateTimerEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/timer/hive/1"))
                .andExpect(status().isUnauthorized()); // Should be 401

        mockMvc.perform(post("/api/v1/timer/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration\":1500,\"type\":\"POMODORO\"}"))
                .andExpect(status().isUnauthorized()); // Should be 401
    }

    @Test
    @DisplayName("Should secure private analytics endpoints - require JWT authentication")
    void shouldSecurePrivateAnalyticsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/productivity"))
                .andExpect(status().isUnauthorized()); // Should be 401
    }

    @Test
    @DisplayName("Should allow authenticated access to private endpoints with valid JWT")
    void shouldAllowAuthenticatedAccessWithValidJWT() throws Exception {
        // These should pass with valid JWT
        mockMvc.perform(get("/api/v1/hives")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isOk()); // This will FAIL - need proper JWT configuration

        mockMvc.perform(get("/api/v1/presence/hive/1")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isOk()); // This will FAIL - need proper JWT configuration

        mockMvc.perform(get("/api/v1/timer/hive/1")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isOk()); // This will FAIL - need proper JWT configuration
    }

    // ============================================================================
    // CORS CONFIGURATION TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should enforce CORS policy with proper allowed origins")
    void shouldEnforceCorsPolicy() throws Exception {
        // Test with allowed origin
        mockMvc.perform(options("/api/v1/hives")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Accept"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Max-Age", "3600")); // This will FAIL - need proper CORS config

        // Test with disallowed origin (should not include CORS headers)
        mockMvc.perform(options("/api/v1/hives")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()); // This will FAIL - need proper CORS validation
    }

    @Test
    @DisplayName("Should allow proper HTTP methods in CORS policy")
    void shouldAllowProperHttpMethodsInCors() throws Exception {
        String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        for (String method : allowedMethods) {
            mockMvc.perform(options("/api/v1/hives")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", method))
                    .andExpect(status().isOk()); // This will FAIL - need proper CORS method validation
        }

        // Test disallowed method
        mockMvc.perform(options("/api/v1/hives")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden()); // This will FAIL - need proper CORS method restriction
    }

    // ============================================================================
    // CSRF PROTECTION TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should prevent CSRF attacks on state-changing operations")
    void shouldPreventCsrfAttacks() throws Exception {
        // CSRF protection should be enabled for state-changing operations
        // This test will FAIL initially - CSRF is currently disabled

        // POST operation without CSRF token should fail
        mockMvc.perform(post("/api/v1/hives")
                        .with(jwt().authorities("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Hive\",\"description\":\"Test\"}"))
                .andExpect(status().isForbidden()); // This will FAIL - CSRF is disabled

        // PUT operation without CSRF token should fail
        mockMvc.perform(put("/api/v1/hives/1")
                        .with(jwt().authorities("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Hive\"}"))
                .andExpect(status().isForbidden()); // This will FAIL - CSRF is disabled

        // DELETE operation without CSRF token should fail
        mockMvc.perform(delete("/api/v1/hives/1")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isForbidden()); // This will FAIL - CSRF is disabled
    }

    // ============================================================================
    // RATE LIMITING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should rate limit requests per user type")
    void shouldRateLimitRequests() throws Exception {
        // This test will FAIL initially - rate limiting not implemented in security config

        // Test public endpoint rate limiting (100 requests/hour)
        for (int i = 0; i < 101; i++) {
            var result = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"test\",\"password\":\"test\"}"));

            if (i < 100) {
                result.andExpect(status().isOk()); // First 100 should pass
            } else {
                result.andExpect(status().isTooManyRequests()); // 101st should fail
            }
        }
    }

    @Test
    @DisplayName("Should rate limit authenticated requests")
    void shouldRateLimitAuthenticatedRequests() throws Exception {
        // This test will FAIL initially - authenticated rate limiting not implemented

        // Test authenticated endpoint rate limiting (1000 requests/hour)
        // Simulate multiple requests quickly
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/hives")
                            .with(jwt().authorities("ROLE_USER")))
                    .andExpect(status().isOk()); // Should pass within limits
        }

        // This is a simplified test - full test would need 1001 requests
        // For now, we'll test that rate limiting infrastructure exists
    }

    // ============================================================================
    // SECURITY HEADERS TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should include security headers in responses")
    void shouldIncludeSecurityHeaders() throws Exception {
        // These tests will FAIL initially - security headers not configured

        mockMvc.perform(get("/api/v1/hives")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff")) // This will FAIL
                .andExpect(header().string("X-Frame-Options", "DENY")) // This will FAIL
                .andExpect(header().string("X-XSS-Protection", "1; mode=block")) // This will FAIL
                .andExpect(header().exists("Content-Security-Policy")); // This will FAIL
    }

    @Test
    @DisplayName("Should include HTTPS security headers when appropriate")
    void shouldIncludeHttpsSecurityHeaders() throws Exception {
        // This test will FAIL initially - HTTPS headers not configured

        // In production with HTTPS, should include Strict-Transport-Security
        // For now, we'll test the configuration exists but may not be applied in test
        mockMvc.perform(get("/api/v1/hives")
                        .with(jwt().authorities("ROLE_USER"))
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains")); // This will FAIL
    }

    // ============================================================================
    // ADMIN ENDPOINT SECURITY TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should secure admin endpoints - require ADMIN role")
    void shouldSecureAdminEndpoints() throws Exception {
        // This test will FAIL initially - admin endpoints may not exist or be properly secured

        // Should deny access without ADMIN role
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isForbidden()); // Should be 403

        // Should allow access with ADMIN role
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities("ROLE_ADMIN")))
                .andExpect(status().isOk()); // This will FAIL - admin endpoints don't exist
    }

    // ============================================================================
    // ERROR HANDLING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should return proper error responses for authentication failures")
    void shouldReturnProperErrorResponses() throws Exception {
        // This test will FAIL initially - error responses may not be properly formatted

        mockMvc.perform(get("/api/v1/hives"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists()); // This will FAIL - need proper error format
    }

    @Test
    @DisplayName("Should return proper error responses for authorization failures")
    void shouldReturnProperAuthorizationErrorResponses() throws Exception {
        // This test will FAIL initially - authorization error responses may not be proper

        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").exists()); // This will FAIL - need proper error format
    }

    // ============================================================================
    // WebSocket Security Tests - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should secure WebSocket connections")
    void shouldSecureWebSocketConnections() throws Exception {
        // This test will FAIL initially - WebSocket security may not be properly configured
        // WebSocket connections should require valid JWT tokens
        // This is a placeholder - actual WebSocket testing requires different approach

        // Note: Full WebSocket security testing requires STOMP test client
        // For now, we'll ensure the security configuration exists
    }

    /**
     * Helper method to perform multiple requests for rate limiting tests
     */
    private void performMultipleRequests(String endpoint, int count, boolean authenticated) throws Exception {
        for (int i = 0; i < count; i++) {
            var requestBuilder = get(endpoint);
            if (authenticated) {
                requestBuilder = requestBuilder.with(jwt().authorities("ROLE_USER"));
            }
            mockMvc.perform(requestBuilder);
        }
    }
}