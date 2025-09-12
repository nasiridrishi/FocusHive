package com.focushive.identity.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.RateLimitExceededException;
import com.focushive.identity.service.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingInterceptorTest {
    
    @Mock
    private RedisRateLimiter rateLimiter;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private HandlerMethod handlerMethod;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private Method method;
    
    private RateLimitingInterceptor interceptor;
    private StringWriter responseWriter;
    private User testUser;
    
    @BeforeEach
    void setUp() throws Exception {
        interceptor = new RateLimitingInterceptor(rateLimiter, objectMapper);
        responseWriter = new StringWriter();
        
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    void testPreHandle_NoRateLimitAnnotation_ShouldAllowRequest() throws Exception {
        // Given
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(null);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter, never()).isAllowed(anyString(), any());
    }
    
    @Test
    void testPreHandle_NotHandlerMethod_ShouldAllowRequest() throws Exception {
        // Given
        Object handler = new Object(); // Not a HandlerMethod
        
        // When
        boolean result = interceptor.preHandle(request, response, handler);
        
        // Then
        assertTrue(result);
        verify(rateLimiter, never()).isAllowed(anyString(), any());
    }
    
    @Test
    void testPreHandle_WithRateLimit_WithinLimit_ShouldAllowRequest() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(response).setHeader("X-RateLimit-Limit", "5");
        verify(response).setHeader("X-RateLimit-Remaining", "3");
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString());
    }
    
    @Test
    void testPreHandle_RateLimitExceeded_ShouldReturnFalseAnd429() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded", 60));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"rate_limit_exceeded\"}");
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertFalse(result);
        verify(response).setStatus(429);
        verify(response).setHeader("Retry-After", "60");
        verify(response).setContentType("application/json");
    }
    
    @Test
    void testPreHandle_SkipAuthenticatedUser_ShouldAllowRequest() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimitWithSkipAuthenticated();
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter, never()).isAllowed(anyString(), any());
    }
    
    @Test
    void testPreHandle_IPRateLimitType_ShouldUseCorrectKey() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("ip:192.168.1.1"), eq(rateLimit));
    }
    
    @Test
    void testPreHandle_UserRateLimitType_WithAuthenticatedUser_ShouldUseUserKey() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.USER);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("user:" + testUser.getId().toString()), eq(rateLimit));
    }
    
    @Test
    void testPreHandle_IPAndUserRateLimitType_ShouldCombineKeys() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP_AND_USER);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("ip_user:192.168.1.1:" + testUser.getId().toString()), 
                                     eq(rateLimit));
    }
    
    @Test
    void testPreHandle_XForwardedForHeader_ShouldUseCorrectIP() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("ip:10.0.0.1"), eq(rateLimit));
    }
    
    @Test
    void testPreHandle_CloudflareIP_ShouldUseCFConnectingIP() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("ip:203.0.113.1"), eq(rateLimit));
    }
    
    @Test
    void testPreHandle_CustomKeyPrefix_ShouldUsePrefix() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimitWithKeyPrefix("auth_login");
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit))).thenReturn(true);
        when(rateLimiter.getRemainingTokens(anyString(), eq(rateLimit))).thenReturn(4L);
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result);
        verify(rateLimiter).isAllowed(contains("auth_login"), eq(rateLimit));
    }
    
    @Test
    void testPreHandle_RateLimiterException_ShouldAllowRequest() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimit(5, 1, TimeUnit.MINUTES, RateLimit.RateLimitType.IP);
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit)))
                .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertTrue(result, "Should allow request when rate limiter fails to maintain service availability");
    }
    
    @Test
    void testPreHandle_ProgressivePenalties_ShouldIncludeViolationCount() throws Exception {
        // Given
        RateLimit rateLimit = createRateLimitWithProgressivePenalties();
        when(handlerMethod.getMethodAnnotation(RateLimit.class)).thenReturn(rateLimit);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimiter.isAllowed(anyString(), eq(rateLimit)))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded", 60));
        when(rateLimiter.getViolationCount(anyString())).thenReturn(3);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"violationCount\":3}");
        
        // When
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        // Then
        assertFalse(result);
        verify(rateLimiter).getViolationCount(anyString());
        verify(objectMapper).writeValueAsString(argThat(map -> 
                ((java.util.Map<String, Object>) map).containsKey("violationCount")));
    }
    
    // Helper methods to create RateLimit instances
    private RateLimit createRateLimit(int value, long window, TimeUnit timeUnit, RateLimit.RateLimitType type) {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public int value() { return value; }
            @Override public long window() { return window; }
            @Override public TimeUnit timeUnit() { return timeUnit; }
            @Override public RateLimitType type() { return type; }
            @Override public String keyPrefix() { return ""; }
            @Override public boolean skipAuthenticated() { return false; }
            @Override public String message() { return ""; }
            @Override public boolean progressivePenalties() { return false; }
        };
    }
    
    private RateLimit createRateLimitWithSkipAuthenticated() {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public int value() { return 5; }
            @Override public long window() { return 1; }
            @Override public TimeUnit timeUnit() { return TimeUnit.MINUTES; }
            @Override public RateLimitType type() { return RateLimitType.IP; }
            @Override public String keyPrefix() { return ""; }
            @Override public boolean skipAuthenticated() { return true; }
            @Override public String message() { return ""; }
            @Override public boolean progressivePenalties() { return false; }
        };
    }
    
    private RateLimit createRateLimitWithKeyPrefix(String keyPrefix) {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public int value() { return 5; }
            @Override public long window() { return 1; }
            @Override public TimeUnit timeUnit() { return TimeUnit.MINUTES; }
            @Override public RateLimitType type() { return RateLimitType.IP; }
            @Override public String keyPrefix() { return keyPrefix; }
            @Override public boolean skipAuthenticated() { return false; }
            @Override public String message() { return ""; }
            @Override public boolean progressivePenalties() { return false; }
        };
    }
    
    private RateLimit createRateLimitWithProgressivePenalties() {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public int value() { return 5; }
            @Override public long window() { return 1; }
            @Override public TimeUnit timeUnit() { return TimeUnit.MINUTES; }
            @Override public RateLimitType type() { return RateLimitType.IP; }
            @Override public String keyPrefix() { return ""; }
            @Override public boolean skipAuthenticated() { return false; }
            @Override public String message() { return ""; }
            @Override public boolean progressivePenalties() { return true; }
        };
    }
}