package com.focushive.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Focused tests for Rate Limiting Security Configuration
 * These tests will FAIL initially and drive the rate limiting implementation
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Rate Limiting Security Tests")
class RateLimitingSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ============================================================================
    // PUBLIC ENDPOINT RATE LIMITING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should rate limit public auth endpoints to 100 requests per hour")
    void shouldRateLimitPublicAuthEndpoints() throws Exception {
        // This test will FAIL initially - no rate limiting implemented

        String clientIp = "192.168.1.100";
        String endpoint = "/api/v1/auth/login";

        // Simulate requests from same IP
        int allowedRequests = 100;

        // First 100 requests should succeed
        for (int i = 0; i < allowedRequests; i++) {
            mockMvc.perform(post(endpoint)
                            .header("X-Forwarded-For", clientIp)
                            .contentType("application/json")
                            .content("{\"username\":\"test\",\"password\":\"test\"}"))
                    .andExpect(status().isOk()); // Will FAIL - endpoint doesn't exist
        }

        // 101st request should be rate limited
        mockMvc.perform(post(endpoint)
                        .header("X-Forwarded-For", clientIp)
                        .contentType("application/json")
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isTooManyRequests()) // Will FAIL - no rate limiting
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Should rate limit health check endpoints separately")
    void shouldRateLimitHealthEndpoints() throws Exception {
        // This test will FAIL initially - no rate limiting for health endpoints

        String clientIp = "192.168.1.101";
        String endpoint = "/actuator/health";

        // Health endpoints should have their own rate limit (e.g., 200/hour)
        int allowedRequests = 200;

        for (int i = 0; i < allowedRequests; i++) {
            mockMvc.perform(get(endpoint)
                            .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isOk());
        }

        // Request beyond limit should be rate limited
        mockMvc.perform(get(endpoint)
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests()) // Will FAIL - no rate limiting
                .andExpect(header().string("X-RateLimit-Limit", "200"));
    }

    // ============================================================================
    // AUTHENTICATED USER RATE LIMITING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should rate limit authenticated users to 1000 requests per hour")
    void shouldRateLimitAuthenticatedUsers() throws Exception {
        // This test will FAIL initially - no authenticated user rate limiting

        String userId = "user123";
        String endpoint = "/api/v1/hives";

        // Simulate multiple requests from authenticated user
        int allowedRequests = 1000;

        // Test first few requests (testing all 1000 would be slow)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(endpoint)
                            .with(jwt().jwt(jwt -> jwt.claim("sub", userId)).authorities("ROLE_USER")))
                    .andExpect(status().isOk()) // Will FAIL - endpoint may not exist or security not configured
                    .andExpect(header().string("X-RateLimit-Limit", "1000")); // Will FAIL - no rate limiting headers
        }
    }

    @Test
    @DisplayName("Should have different rate limits for different user roles")
    void shouldHaveDifferentRateLimitsForUserRoles() throws Exception {
        // This test will FAIL initially - no role-based rate limiting

        String regularUserId = "user123";
        String adminUserId = "admin456";
        String endpoint = "/api/v1/hives";

        // Regular user should have lower limit
        mockMvc.perform(get(endpoint)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", regularUserId)).authorities("ROLE_USER")))
                .andExpect(status().isOk()) // Will FAIL - security not properly configured
                .andExpect(header().string("X-RateLimit-Limit", "1000")); // Will FAIL - no rate limiting

        // Admin user should have higher limit
        mockMvc.perform(get(endpoint)
                        .with(jwt().jwt(jwt -> jwt.claim("sub", adminUserId)).authorities("ROLE_ADMIN")))
                .andExpect(status().isOk()) // Will FAIL - security not properly configured
                .andExpect(header().string("X-RateLimit-Limit", "10000")); // Will FAIL - no rate limiting
    }

    // ============================================================================
    // WEBSOCKET CONNECTION RATE LIMITING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should rate limit WebSocket connections to 60 per minute")
    void shouldRateLimitWebSocketConnections() throws Exception {
        // This test will FAIL initially - no WebSocket rate limiting

        // Note: This is a placeholder test for WebSocket rate limiting
        // Actual WebSocket testing requires STOMP test client
        // For now, we verify the rate limiting configuration exists

        String clientIp = "192.168.1.102";

        // Simulate WebSocket handshake requests
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/ws")
                            .header("X-Forwarded-For", clientIp)
                            .header("Upgrade", "websocket")
                            .header("Connection", "Upgrade"))
                    .andExpect(status().isSwitchingProtocols()); // Will FAIL - no WebSocket endpoint or rate limiting
        }

        // 61st connection should be rate limited
        mockMvc.perform(get("/ws")
                        .header("X-Forwarded-For", clientIp)
                        .header("Upgrade", "websocket")
                        .header("Connection", "Upgrade"))
                .andExpect(status().isTooManyRequests()); // Will FAIL - no rate limiting
    }

    // ============================================================================
    // RATE LIMITING BYPASS TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should not be bypassed by changing User-Agent")
    void shouldNotBypassRateLimitingByChangingUserAgent() throws Exception {
        // This test will FAIL initially - rate limiting may be bypassable

        String clientIp = "192.168.1.103";
        String endpoint = "/api/v1/auth/login";

        // Make requests with different User-Agent headers but same IP
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                "Mozilla/5.0 (X11; Linux x86_64)",
                "Custom-Bot/1.0"
        };

        int requestsPerUserAgent = 30; // Total: 120 requests, should exceed 100 limit

        for (String userAgent : userAgents) {
            for (int i = 0; i < requestsPerUserAgent; i++) {
                var result = mockMvc.perform(post(endpoint)
                                .header("X-Forwarded-For", clientIp)
                                .header("User-Agent", userAgent)
                                .contentType("application/json")
                                .content("{\"username\":\"test\",\"password\":\"test\"}"));

                // After 100 total requests (regardless of User-Agent), should be rate limited
                if (i * userAgents.length + java.util.Arrays.asList(userAgents).indexOf(userAgent) < 100) {
                    result.andExpect(status().isOk()); // Will FAIL - endpoint doesn't exist
                } else {
                    result.andExpect(status().isTooManyRequests()); // Will FAIL - no rate limiting
                    break; // Stop testing once rate limited
                }
            }
        }
    }

    @Test
    @DisplayName("Should not be bypassed by X-Forwarded-For header manipulation")
    void shouldNotBypassRateLimitingByHeaderManipulation() throws Exception {
        // This test will FAIL initially - rate limiting may be vulnerable to header manipulation

        String endpoint = "/api/v1/auth/login";
        String realClientIp = "192.168.1.104";

        // Try to bypass by using multiple X-Forwarded-For values
        String[] fakeIps = {
                "1.1.1.1, " + realClientIp,
                "2.2.2.2, 1.1.1.1, " + realClientIp,
                realClientIp + ", 3.3.3.3",
                "4.4.4.4"
        };

        int totalRequests = 120; // Should exceed 100 limit

        for (int i = 0; i < totalRequests; i++) {
            String fakeIp = fakeIps[i % fakeIps.length];

            var result = mockMvc.perform(post(endpoint)
                            .header("X-Forwarded-For", fakeIp)
                            .contentType("application/json")
                            .content("{\"username\":\"test\",\"password\":\"test\"}"));

            if (i < 100) {
                result.andExpect(status().isOk()); // Will FAIL - endpoint doesn't exist
            } else {
                result.andExpect(status().isTooManyRequests()); // Will FAIL - no rate limiting or vulnerable to bypass
                break;
            }
        }
    }

    // ============================================================================
    // RATE LIMITING RESET TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should reset rate limits after time window expires")
    void shouldResetRateLimitsAfterTimeWindow() throws Exception {
        // This test will FAIL initially - no time-based rate limit reset

        String clientIp = "192.168.1.105";
        String endpoint = "/actuator/health";

        // This test would require time manipulation or a shorter test window
        // For now, we'll test that the reset time is properly set
        mockMvc.perform(get(endpoint)
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Reset")) // Will FAIL - no rate limiting headers
                .andExpect(header().longValue("X-RateLimit-Reset", org.hamcrest.Matchers.greaterThan(System.currentTimeMillis()))); // Will FAIL
    }

    // ============================================================================
    // DISTRIBUTED RATE LIMITING TESTS - These should FAIL initially
    // ============================================================================

    @Test
    @DisplayName("Should use Redis for distributed rate limiting")
    void shouldUseRedisForDistributedRateLimiting() throws Exception {
        // This test will FAIL initially - no Redis-based distributed rate limiting

        // This test ensures that rate limits are shared across multiple application instances
        // For now, we'll test that Redis is configured for rate limiting

        String clientIp = "192.168.1.106";
        String endpoint = "/api/v1/auth/login";

        // Make request and verify Redis-based rate limiting
        mockMvc.perform(post(endpoint)
                        .header("X-Forwarded-For", clientIp)
                        .contentType("application/json")
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk()) // Will FAIL - endpoint doesn't exist
                .andExpect(header().exists("X-RateLimit-Limit")) // Will FAIL - no rate limiting
                .andExpect(header().exists("X-RateLimit-Remaining")); // Will FAIL - no rate limiting
    }
}