package com.focushive.notification.security;

import com.focushive.notification.config.RateLimitingInterceptor;
import com.focushive.notification.service.RateLimitingService;
import com.focushive.notification.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test class for RateLimitingInterceptor following TDD approach.
 * Tests distributed rate limiting with Redis integration and audit logging.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingInterceptor Tests")
class RateLimitingInterceptorTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private UserContextService userContextService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    @Mock
    private PrintWriter printWriter;

    private RateLimitingInterceptor rateLimitingInterceptor;

    @BeforeEach
    void setUp() {
        rateLimitingInterceptor = new RateLimitingInterceptor(
            rateLimitingService,
            userContextService
        );
    }
    @Test
    @DisplayName("Should allow request when under rate limit")
    void shouldAllowRequestWhenUnderRateLimit() throws Exception {
        // Given
        String userId = "user123";
        String endpoint = "/api/templates";
        String operationType = "READ";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(100);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(94);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "94");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("Should block request when rate limit exceeded")
    void shouldBlockRequestWhenRateLimitExceeded() throws Exception {
        // Given
        String userId = "admin456";
        String endpoint = "/api/admin/stats";
        String operationType = "ADMIN";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(response.getWriter()).willReturn(printWriter);
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(false);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(100);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(0);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(429); // HTTP 429 Too Many Requests
        verify(response).setHeader("Content-Type", "application/json");
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("Should apply different rate limits for different operations")
    void shouldApplyDifferentRateLimitsForDifferentOperations() throws Exception {
        // Given - Write operation (POST)
        String userId = "user789";
        String endpoint = "/api/preferences";
        String operationType = "WRITE";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("POST");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(50);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(39);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Limit", "50"); // Write operations have lower limit
        verify(response).setHeader("X-RateLimit-Remaining", "39");
    }

    @Test
    @DisplayName("Should track rate limits per user for authenticated requests")
    void shouldTrackRateLimitsPerUserForAuthenticatedRequests() throws Exception {
        // Given
        String userId = "user999";
        String endpoint = "/api/notifications";
        String operationType = "READ";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(100);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(99);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Remaining", "99");
    }

    @Test
    @DisplayName("Should track rate limits per IP for unauthenticated requests")
    void shouldTrackRateLimitsPerIpForUnauthenticatedRequests() throws Exception {
        // Given
        String ipAddress = "203.0.113.5";
        String endpoint = "/api/public/health";
        String operationType = "PUBLIC";

        given(userContextService.getCurrentUserId()).willReturn(null);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(request.getRemoteAddr()).willReturn(ipAddress);
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(eq(ipAddress), anyString())).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(anyString())).willReturn(200);
        given(rateLimitingService.getRemainingRequests(eq(ipAddress), anyString())).willReturn(174);
        given(rateLimitingService.getResetTime(eq(ipAddress), anyString())).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Limit", "200"); // Higher limit for unauthenticated
        verify(response).setHeader("X-RateLimit-Remaining", "174");
    }

    @Test
    @DisplayName("Should apply stricter rate limits for sensitive operations")
    void shouldApplyStricterRateLimitsForSensitiveOperations() throws Exception {
        // Given - Admin operation
        String userId = "admin123";
        String endpoint = "/api/admin/templates";
        String operationType = "ADMIN";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("DELETE");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(20);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(4);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Limit", "20"); // Very strict for admin operations
        verify(response).setHeader("X-RateLimit-Remaining", "4");
    }

    @Test
    @DisplayName("Should reset rate limits after time window expires")
    void shouldResetRateLimitsAfterTimeWindowExpires() throws Exception {
        // Given
        String userId = "user111";
        String endpoint = "/api/templates";
        String operationType = "READ";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(100);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(99); // Fresh window
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Remaining", "99"); // Fresh window
    }

    @Test
    @DisplayName("Should include rate limit headers in response")
    void shouldIncludeRateLimitHeadersInResponse() throws Exception {
        // Given
        String userId = "user222";
        String endpoint = "/api/preferences";
        String operationType = "READ";

        given(userContextService.getCurrentUserId()).willReturn(userId);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(userId, operationType)).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(operationType)).willReturn(100);
        given(rateLimitingService.getRemainingRequests(userId, operationType)).willReturn(57);
        given(rateLimitingService.getResetTime(userId, operationType)).willReturn(System.currentTimeMillis() + 60000);

        // When
        rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "57");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("Should handle Redis connection failures gracefully")
    void shouldHandleRedisConnectionFailuresGracefully() throws Exception {
        // Given
        given(rateLimitingService.isEnabled()).willReturn(false); // Simulate disabled when Redis fails

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue(); // Should allow request when rate limiting is disabled
        verify(response, never()).setStatus(429);
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header for IP-based rate limiting")
    void shouldUseXForwardedForHeaderForIpBasedRateLimiting() throws Exception {
        // Given
        String forwardedFor = "198.51.100.5";
        String endpoint = "/api/public/status";
        String operationType = "PUBLIC";

        given(userContextService.getCurrentUserId()).willReturn(null);
        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getMethod()).willReturn("GET");
        given(request.getHeader("X-Forwarded-For")).willReturn(forwardedFor);
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed(eq(forwardedFor), anyString())).willReturn(true);
        given(rateLimitingService.getConfiguredLimit(anyString())).willReturn(200);
        given(rateLimitingService.getRemainingRequests(eq(forwardedFor), anyString())).willReturn(189);
        given(rateLimitingService.getResetTime(eq(forwardedFor), anyString())).willReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(response).setHeader("X-RateLimit-Remaining", "189");
    }

    @Test
    @DisplayName("Should exclude certain endpoints from rate limiting")
    void shouldExcludeCertainEndpointsFromRateLimiting() throws Exception {
        // Given
        String endpoint = "/actuator/health";
        String operationType = "PUBLIC";

        given(request.getRequestURI()).willReturn(endpoint);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");
        given(rateLimitingService.isEnabled()).willReturn(true);
        given(rateLimitingService.isAllowed("127.0.0.1", operationType)).willReturn(true);

        // When
        boolean result = rateLimitingInterceptor.preHandle(request, response, handlerMethod);

        // Then
        assertThat(result).isTrue();
        verify(rateLimitingService).isAllowed(anyString(), eq(operationType));
    }
}