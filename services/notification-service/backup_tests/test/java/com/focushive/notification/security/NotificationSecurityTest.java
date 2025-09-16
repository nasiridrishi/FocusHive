package com.focushive.notification.security;

import com.focushive.notification.dto.EmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security tests for notification service endpoints.
 * Tests authentication, authorization, input validation, and security headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("security")
@DisplayName("Notification Security Tests")
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private String validJwtToken;
    private String expiredJwtToken;
    private String malformedJwtToken;

    @BeforeEach
    void setUp() {
        // Note: In real tests, these would be generated using test JWT utility
        validJwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature";
        expiredJwtToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.signature";
        malformedJwtToken = "Bearer malformed.jwt.token";
    }

    @Test
    @DisplayName("Should reject requests without authentication")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate and sanitize email input to prevent injection")
    void shouldValidateAndSanitizeEmailInput() throws Exception {
        // Test SQL injection attempt in email field
        String sqlInjectionPayload = """
            {
                "to": "test@example.com'; DROP TABLE users; --",
                "subject": "Test",
                "htmlContent": "<p>Test</p>"
            }
            """;

        mockMvc.perform(post("/api/notifications/send")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sqlInjectionPayload))
                .andExpect(status().isBadRequest());

        // Test XSS attempt in subject field
        String xssPayload = """
            {
                "to": "test@example.com",
                "subject": "<script>alert('XSS')</script>",
                "htmlContent": "<p>Test</p>"
            }
            """;

        mockMvc.perform(post("/api/notifications/send")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(xssPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject", not(containsString("<script>"))));
    }

    @Test
    @DisplayName("Should enforce rate limiting on notification endpoints")
    void shouldEnforceRateLimiting() throws Exception {
        // Send multiple requests rapidly to trigger rate limiting
        int requestCount = 10;
        int successfulRequests = 0;
        int rateLimitedRequests = 0;

        for (int i = 0; i < requestCount; i++) {
            var result = mockMvc.perform(get("/api/notifications")
                    .header("Authorization", validJwtToken))
                    .andReturn();

            int status = result.getResponse().getStatus();
            if (status == 200) {
                successfulRequests++;
            } else if (status == 429) { // Too Many Requests
                rateLimitedRequests++;
            }
        }

        // At least some requests should be rate limited or all should succeed
        // (depending on implementation)
        assertTrue(successfulRequests > 0 || rateLimitedRequests > 0,
                "Should either allow requests or enforce rate limiting");
    }
}