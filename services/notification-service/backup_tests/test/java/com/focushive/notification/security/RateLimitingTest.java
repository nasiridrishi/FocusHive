package com.focushive.notification.security;

import com.focushive.notification.config.BaseWebMvcTest;
import com.focushive.notification.config.ControllerTestConfiguration;
import com.focushive.notification.service.RateLimitingService;
import com.focushive.notification.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import org.springframework.context.annotation.Import;
import com.focushive.notification.config.WebConfig;
import com.focushive.notification.config.RateLimitingInterceptor;

/**
 * Test class for API rate limiting functionality.
 * Tests rate limiting based on user ID and IP address.
 */
@BaseWebMvcTest
@Import({ControllerTestConfiguration.class, WebConfig.class, RateLimitingInterceptor.class})
@DisplayName("Rate Limiting Tests")
class RateLimitingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private RateLimitingService rateLimitingService;

    @MockBean
    private UserContextService userContextService;

    private Jwt validJwt;

    @BeforeEach
    void setUp() {
        validJwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .claim("sub", "user123")
                .claim("email", "user@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Enable rate limiting by default for all tests
        given(rateLimitingService.isEnabled()).willReturn(true);

        // Mock user context service
        given(userContextService.getCurrentUserId()).willReturn("user123");
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "READ")).willReturn(true);
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject requests that exceed rate limit")
    void shouldRejectRequestsThatExceedRateLimit() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "READ")).willReturn(false);
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Should have different rate limits for different operations")
    void shouldHaveDifferentRateLimitsForDifferentOperations() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "READ")).willReturn(true);
        given(rateLimitingService.isAllowed("user123", "WRITE")).willReturn(false);
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then - READ operations should be allowed
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk());

        // WRITE operations should be rate limited
        mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt().jwt(validJwt))
                        .contentType("application/json")
                        .content("{\"userId\":\"user123\",\"type\":\"SYSTEM_NOTIFICATION\",\"title\":\"Test\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should apply stricter rate limits for sensitive operations")
    void shouldApplyStricterRateLimitsForSensitiveOperations() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "ADMIN")).willReturn(false);
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then
        mockMvc.perform(post("/api/preferences/user/user123/defaults")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should track rate limits per user")
    void shouldTrackRateLimitsPerUser() throws Exception {
        // Given
        Jwt user1Jwt = Jwt.withTokenValue("user1-token")
                .header("alg", "RS256")
                .claim("sub", "user1")
                .claim("email", "user1@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Jwt user2Jwt = Jwt.withTokenValue("user2-token")
                .header("alg", "RS256")
                .claim("sub", "user2")
                .claim("email", "user2@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        given(rateLimitingService.isAllowed("user1", "READ")).willReturn(false);
        given(rateLimitingService.isAllowed("user2", "READ")).willReturn(true);
        given(jwtDecoder.decode("user1-token")).willReturn(user1Jwt);
        given(jwtDecoder.decode("user2-token")).willReturn(user2Jwt);

        // Mock UserContextService to return different user IDs
        given(userContextService.getCurrentUserId()).willReturn("user1", "user2");

        // When & Then - User1 should be rate limited
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user1")
                        .with(jwt().jwt(user1Jwt)))
                .andExpect(status().isTooManyRequests());

        // User2 should not be rate limited
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user2")
                        .with(jwt().jwt(user2Jwt)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle rate limiting for unauthenticated requests by IP")
    void shouldHandleRateLimitingForUnauthenticatedRequestsByIP() throws Exception {
        // Given
        given(rateLimitingService.isAllowed(eq("127.0.0.1"), eq("PUBLIC"))).willReturn(false);

        // When & Then
        mockMvc.perform(get("/actuator/health")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should include rate limit headers in response")
    void shouldIncludeRateLimitHeadersInResponse() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "READ")).willReturn(true);
        given(rateLimitingService.getConfiguredLimit("READ")).willReturn(100);
        given(rateLimitingService.getRemainingRequests("user123", "READ")).willReturn(95);
        given(rateLimitingService.getResetTime("user123", "READ")).willReturn(System.currentTimeMillis() + 3600000);
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "100"))
                .andExpect(header().string("X-RateLimit-Remaining", "95"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    @DisplayName("Should reset rate limits after time window")
    void shouldResetRateLimitsAfterTimeWindow() throws Exception {
        // Given
        given(rateLimitingService.isAllowed("user123", "READ"))
                .willReturn(false)  // First call - rate limited
                .willReturn(true);  // After reset - allowed

        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // When & Then - First request should be rate limited
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isTooManyRequests());

        // Simulate time passing and rate limit reset
        // Second request should be allowed
        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", "user123")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk());
    }
}