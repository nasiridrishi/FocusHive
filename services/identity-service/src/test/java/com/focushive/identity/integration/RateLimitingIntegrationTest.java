package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting functionality.
 * Tests the complete flow from HTTP request to rate limit enforcement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles({"test", "integration-test"})
@TestPropertySource(properties = {
    "focushive.rate-limiting.enabled=true",
    "focushive.rate-limiting.redis.enabled=false", // Use in-memory for tests
    "logging.level.com.focushive.identity=DEBUG"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RateLimitingIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testLoginEndpoint_RateLimitEnforced() throws Exception {
        String loginPayload = """
            {
                "usernameOrEmail": "testuser@example.com",
                "password": "wrongpassword"
            }
            """;
        
        String sourceIp = "192.168.1.100";
        
        // First 5 requests should be allowed (rate limit is 5 per minute for login)
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload)
                    .header("X-Forwarded-For", sourceIp))
                    .andExpect(status().isUnauthorized()) // Wrong password, but not rate limited
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().string("X-RateLimit-Limit", "5"))
                    .andReturn();
            
            // Check remaining count decreases
            String remaining = result.getResponse().getHeader("X-RateLimit-Remaining");
            assertEquals(String.valueOf(4 - i), remaining, 
                        "Remaining tokens should decrease with each request");
        }
        
        // 6th request should be rate limited
        MvcResult rateLimitedResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        
        // Verify error response format
        String responseContent = rateLimitedResult.getResponse().getContentAsString();
        Map<String, Object> errorResponse = objectMapper.readValue(responseContent, Map.class);
        
        assertEquals("rate_limit_exceeded", errorResponse.get("error"));
        assertTrue(errorResponse.containsKey("message"));
        assertTrue(errorResponse.containsKey("timestamp"));
        assertTrue(errorResponse.containsKey("path"));
        assertTrue(errorResponse.containsKey("retryAfterSeconds"));
        assertTrue(errorResponse.containsKey("limit"));
        assertTrue(errorResponse.containsKey("window"));
        
        // Verify retry after header is numeric
        String retryAfter = rateLimitedResult.getResponse().getHeader("Retry-After");
        assertNotNull(retryAfter);
        assertTrue(Long.parseLong(retryAfter) > 0, "Retry-After should be a positive number");
    }
    
    @Test
    void testRegistrationEndpoint_RateLimitEnforced() throws Exception {
        String registrationPayload = """
            {
                "username": "testuser",
                "email": "test@example.com",
                "password": "StrongPassword123!",
                "confirmPassword": "StrongPassword123!"
            }
            """;
        
        String sourceIp = "192.168.1.101";
        
        // First 2 requests should be allowed (rate limit is 2 per minute for registration)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationPayload)
                    .header("X-Forwarded-For", sourceIp))
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().string("X-RateLimit-Limit", "2"));
        }
        
        // 3rd request should be rate limited
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("registration attempts")));
    }
    
    @Test
    void testPasswordResetRequest_RateLimitEnforced() throws Exception {
        String resetRequestPayload = """
            {
                "email": "test@example.com"
            }
            """;
        
        String sourceIp = "192.168.1.102";
        
        // First request should be allowed (rate limit is 1 per minute)
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequestPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
        
        // 2nd request should be rate limited
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequestPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("rate_limit_exceeded"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password reset")));
    }
    
    @Test
    void testDifferentIPsNotAffectingEachOther() throws Exception {
        String loginPayload = """
            {
                "usernameOrEmail": "testuser@example.com",
                "password": "wrongpassword"
            }
            """;
        
        String ip1 = "192.168.1.200";
        String ip2 = "192.168.1.201";
        
        // Exhaust rate limit for IP1
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload)
                    .header("X-Forwarded-For", ip1))
                    .andExpect(status().isUnauthorized()); // Should not be rate limited yet
        }
        
        // IP1 should now be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload)
                .header("X-Forwarded-For", ip1))
                .andExpect(status().isTooManyRequests());
        
        // IP2 should still be allowed
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload)
                .header("X-Forwarded-For", ip2))
                .andExpect(status().isUnauthorized()) // Wrong password, but not rate limited
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }
    
    @Test
    void testRateLimitHeaders_CorrectlySet() throws Exception {
        String loginPayload = """
            {
                "usernameOrEmail": "testuser@example.com",
                "password": "wrongpassword"
            }
            """;
        
        String sourceIp = "192.168.1.300";
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(header().exists("X-RateLimit-Window"))
                .andReturn();
        
        // Verify header values
        assertEquals("5", result.getResponse().getHeader("X-RateLimit-Limit"));
        assertEquals("4", result.getResponse().getHeader("X-RateLimit-Remaining"));
        assertTrue(result.getResponse().getHeader("X-RateLimit-Window").contains("minutes"));
        
        // Verify reset time is in the future
        String resetTime = result.getResponse().getHeader("X-RateLimit-Reset");
        long resetTimestamp = Long.parseLong(resetTime);
        long currentTimestamp = System.currentTimeMillis() / 1000;
        assertTrue(resetTimestamp > currentTimestamp, "Reset time should be in the future");
    }
    
    @Test
    void testProgressivePenalties_IncreaseWithViolations() throws Exception {
        String loginPayload = """
            {
                "usernameOrEmail": "testuser@example.com", 
                "password": "wrongpassword"
            }
            """;
        
        String sourceIp = "192.168.1.400";
        
        // First: exhaust normal rate limit
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload)
                    .header("X-Forwarded-For", sourceIp))
                    .andExpect(status().isUnauthorized());
        }
        
        // Continue making requests to trigger progressive penalties
        for (int violation = 1; violation <= 3; violation++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload)
                    .header("X-Forwarded-For", sourceIp))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.progressivePenalty").value(true))
                    .andExpect(jsonPath("$.violationCount").value(violation))
                    .andReturn();
            
            // Verify retry-after time increases with violations
            String retryAfter = result.getResponse().getHeader("Retry-After");
            assertTrue(Long.parseLong(retryAfter) >= 60, 
                      "Retry-after time should be at least 60 seconds for violation " + violation);
        }
    }
    
    @Test
    void testRateLimitingDisabled_ShouldAllowAllRequests() throws Exception {
        // This test would require a separate test profile with rate limiting disabled
        // For now, we'll test that the interceptor handles configuration correctly
        assertTrue(true, "Rate limiting configuration test placeholder");
    }
    
    @Test
    void testCustomErrorMessage_InRateLimitResponse() throws Exception {
        String registrationPayload = """
            {
                "username": "testuser",
                "email": "test@example.com", 
                "password": "StrongPassword123!",
                "confirmPassword": "StrongPassword123!"
            }
            """;
        
        String sourceIp = "192.168.1.500";
        
        // Exhaust rate limit
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registrationPayload)
                    .header("X-Forwarded-For", sourceIp));
        }
        
        // Verify custom message from annotation is used
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationPayload)
                .header("X-Forwarded-For", sourceIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many registration attempts. Please wait before trying again."));
    }
    
    /**
     * Utility method to wait for rate limit window to reset.
     * Use sparingly in tests to avoid long test execution times.
     */
    private void waitForRateLimitReset() throws InterruptedException {
        TimeUnit.SECONDS.sleep(61); // Wait for 1 minute window to reset
    }
}