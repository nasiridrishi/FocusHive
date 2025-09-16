package com.focushive.notification.interceptor;

import com.focushive.notification.config.RateLimitingInterceptor;
import com.focushive.notification.config.SecurityProperties;
import com.focushive.notification.service.RateLimitingService;
import com.focushive.notification.service.UserContextService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitingInterceptor.
 * Tests rate limiting functionality for API endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limiting Interceptor Tests")
class RateLimitingInterceptorTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private UserContextService userContextService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private MeterRegistry meterRegistry;
    private RateLimitingInterceptor interceptor;
    private SecurityProperties.RateLimitingConfig rateLimitingConfig;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        interceptor = new RateLimitingInterceptor(
            rateLimitingService,
            userContextService
        );

        rateLimitingConfig = new SecurityProperties.RateLimitingConfig();
        rateLimitingConfig.setEnabled(true);
        rateLimitingConfig.setDefaultRequestsPerMinute(60);
        rateLimitingConfig.setAnonymousRequestsPerMinute(20);
        rateLimitingConfig.setAdminRequestsPerMinute(300);
        rateLimitingConfig.setBurstCapacity(10);
        rateLimitingConfig.setCacheTtl(Duration.ofMinutes(1));
        rateLimitingConfig.setExcludedPaths(Collections.singletonList("/health"));

        when(securityProperties.getRateLimiting()).thenReturn(rateLimitingConfig);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should allow request when rate limit not exceeded")
    void shouldAllowRequestWhenRateLimitNotExceeded() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(45);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(anyString(), anyInt(), anyInt());
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should block request when rate limit exceeded")
    void shouldBlockRequestWhenRateLimitExceeded() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(false);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertFalse(result);
        verify(response).sendError(429, "Rate limit exceeded");
        verify(response).setHeader("X-RateLimit-Limit", "0");
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(response).setHeader(eq("X-RateLimit-Retry-After"), anyString());
    }

    @Test
    @DisplayName("Should bypass rate limiting for configured paths")
    void shouldBypassRateLimitingForConfiguredPaths() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/health");

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService, never()).allowRequest(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should apply user-based rate limiting when authenticated")
    void shouldApplyUserBasedRateLimitingWhenAuthenticated() throws Exception {
        // Given
        String userId = "user123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(50);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(eq("user:" + userId), eq(60), anyInt());
    }

    @Test
    @DisplayName("Should apply IP-based rate limiting for anonymous users")
    void shouldApplyIpBasedRateLimitingForAnonymousUsers() throws Exception {
        // Given
        String ipAddress = "192.168.1.100";
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(request.getRemoteAddr()).thenReturn(ipAddress);
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(15);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(eq("anonymous:" + ipAddress), eq(20), anyInt());
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For header")
    void shouldExtractClientIpFromXForwardedForHeader() throws Exception {
        // Given
        String realIp = "203.0.113.0";
        String proxyIp = "192.168.1.1";
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(request.getHeader("X-Forwarded-For")).thenReturn(realIp + ", " + proxyIp);
        when(rateLimitingService.allowRequest("ip:" + realIp, 60, 1000)).thenReturn(true);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest("ip:" + realIp, 60, 1000);
    }

    @Test
    @DisplayName("Should add rate limit headers to response")
    void shouldAddRateLimitHeadersToResponse() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(45);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(
            Instant.now().plusSeconds(30).toEpochMilli()
        );

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(response).setHeader("X-RateLimit-Limit", "60");
        verify(response).setHeader("X-RateLimit-Remaining", "45");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }

    @Test
    @DisplayName("Should track rate limit metrics")
    void shouldTrackRateLimitMetrics() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(false);

        // When
        interceptor.preHandle(request, response, new Object());

        // Then
        assertEquals(1.0, meterRegistry.counter("rate.limit.exceeded",
            "endpoint", "/api/notifications").count());
    }

    @Test
    @DisplayName("Should handle rate limiting with Redis backend")
    void shouldHandleRateLimitingWithRedisBackend() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(request.getRemoteAddr()).thenReturn("192.168.1.200");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(19);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(anyString(), anyInt(), anyInt());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("19"));
    }

    @Test
    @DisplayName("Should initialize rate limit for new client")
    void shouldInitializeRateLimitForNewClient() throws Exception {
        // Given
        String newIp = "192.168.1.201";
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(request.getRemoteAddr()).thenReturn(newIp);
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(19); // First request consumed
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(eq("anonymous:" + newIp), anyInt(), anyInt());
        verify(response).setHeader(eq("X-RateLimit-Remaining"), eq("19"));
    }

    @Test
    @DisplayName("Should apply different limits for different user types")
    void shouldApplyDifferentLimitsForDifferentUserTypes() throws Exception {
        // Given admin user
        String adminId = "admin123";
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        when(authentication.getAuthorities()).thenReturn(Collections.singletonList(() -> "ROLE_ADMIN"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowRequest(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitingService.getRemainingRequests(anyString())).thenReturn(250);
        when(rateLimitingService.getResetTime(anyString())).thenReturn(System.currentTimeMillis() + 60000);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowRequest(eq("admin:" + adminId), eq(300), anyInt());
    }

    @Test
    @DisplayName("Should handle rate limiting when disabled")
    void shouldHandleRateLimitingWhenDisabled() throws Exception {
        // Given
        rateLimitingConfig.setEnabled(false);
        when(request.getRequestURI()).thenReturn("/api/notifications");

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService, never()).allowRequest(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should block temporarily after multiple violations")
    void shouldBlockTemporarilyAfterMultipleViolations() throws Exception {
        // Given
        String userId = "user123";
        String blockKey = "blocked:" + userId;
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId);
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(valueOperations.get(blockKey)).thenReturn("blocked");

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertFalse(result);
        verify(response).sendError(429, "Temporarily blocked due to rate limit violations");
    }

    @Test
    @DisplayName("Should apply burst capacity for sudden traffic")
    void shouldApplyBurstCapacityForSuddenTraffic() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/notifications");
        when(rateLimitingService.allowBurstRequest("global", 10)).thenReturn(true);

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(rateLimitingService).allowBurstRequest("global", 10);
    }
}