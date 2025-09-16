package com.focushive.notification.security;

import com.focushive.notification.config.SecurityProperties;
import com.focushive.notification.service.SecurityAuditService;
import com.focushive.notification.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test class for JwtAuthenticationFilter following TDD approach.
 * Tests JWT token validation, user context extraction, and audit logging integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jwt jwt;

    @Mock
    private PrintWriter printWriter;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SecurityProperties.AuthenticationConfig authConfig;
    private SecurityProperties.HeadersConfig headersConfig;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // Setup security properties with default values
        authConfig = new SecurityProperties.AuthenticationConfig();
        authConfig.setMaxFailedAttempts(5);
        authConfig.setFailedAttemptsWindow(Duration.ofMinutes(5));
        authConfig.setLockoutDuration(Duration.ofMinutes(15));
        authConfig.setTrackByIp(true);
        authConfig.setAutoUnlock(true);

        headersConfig = new SecurityProperties.HeadersConfig();
        headersConfig.setEnabled(true);
        headersConfig.setContentTypeOptions("nosniff");
        headersConfig.setFrameOptions("DENY");
        headersConfig.setXssProtection("1; mode=block");
        headersConfig.setReferrerPolicy("strict-origin-when-cross-origin");
        headersConfig.setContentSecurityPolicy("default-src 'self'");
        headersConfig.setStrictTransportSecurity("max-age=31536000; includeSubDomains");

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtDecoder, securityAuditService, securityProperties, tokenBlacklistService);
    }

    @Test
    @DisplayName("Should process valid JWT token and set authentication")
    void shouldProcessValidJwtTokenAndSetAuthentication() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String ipAddress = "127.0.0.1";

        given(securityProperties.getHeaders()).willReturn(headersConfig);
        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getHeader("X-Correlation-ID")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(ipAddress);

        given(jwt.getSubject()).willReturn("user123");
        given(jwt.getClaim("roles")).willReturn(List.of("ROLE_USER"));
        given(jwtDecoder.decode(token)).willReturn(jwt);
        given(tokenBlacklistService.isBlacklisted(jwt)).willReturn(false);
        given(tokenBlacklistService.isUserBlacklisted("user123")).willReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityAuditService).logAuthenticationSuccess(ipAddress);
        verify(filterChain).doFilter(request, response);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user123");
    }

    @Test
    @DisplayName("Should handle invalid JWT token and log failure")
    void shouldHandleInvalidJwtTokenAndLogFailure() throws ServletException, IOException {
        // Given
        String token = "invalid.jwt.token";
        String authHeader = "Bearer " + token;
        String ipAddress = "192.168.1.100";

        given(securityProperties.getAuthentication()).willReturn(authConfig);
        given(securityProperties.getHeaders()).willReturn(headersConfig);
        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getHeader("X-Forwarded-For")).willReturn(null);
        given(request.getHeader("X-Real-IP")).willReturn(null);
        given(request.getRemoteAddr()).willReturn(ipAddress);
        given(response.getWriter()).willReturn(printWriter);
        given(jwtDecoder.decode(token)).willThrow(new JwtException("Invalid token signature"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityAuditService).logAuthenticationFailure(anyString(), eq(ipAddress), eq("Invalid token signature"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should proceed without authentication when no token provided")
    void shouldProceedWithoutAuthenticationWhenNoTokenProvided() throws ServletException, IOException {
        // Given
        given(securityProperties.getHeaders()).willReturn(headersConfig);
        given(request.getHeader("Authorization")).willReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(securityAuditService, never()).logAuthenticationSuccess(any());
        verify(securityAuditService, never()).logAuthenticationFailure(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should handle malformed authorization header")
    void shouldHandleMalformedAuthorizationHeader() throws ServletException, IOException {
        // Given
        String authHeader = "InvalidFormat token";

        given(securityProperties.getHeaders()).willReturn(headersConfig);
        given(request.getHeader("Authorization")).willReturn(authHeader);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(securityAuditService, never()).logAuthenticationSuccess(any());
        verify(securityAuditService, never()).logAuthenticationFailure(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
