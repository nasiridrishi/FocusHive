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
import java.util.Map;

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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // Setup security properties with default values
        SecurityProperties.AuthenticationConfig authConfig = new SecurityProperties.AuthenticationConfig();
        authConfig.setMaxFailedAttempts(5);
        authConfig.setFailedAttemptsWindow(Duration.ofMinutes(5));
        authConfig.setLockoutDuration(Duration.ofMinutes(15));
        authConfig.setTrackByIp(true);
        authConfig.setAutoUnlock(true);

        SecurityProperties.HeadersConfig headersConfig = new SecurityProperties.HeadersConfig();
        headersConfig.setEnabled(true);

        given(securityProperties.getAuthentication()).willReturn(authConfig);
        given(securityProperties.getHeaders()).willReturn(headersConfig);

        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtDecoder, securityAuditService, securityProperties, tokenBlacklistService);
    }

    @Test
    @DisplayName("Should process valid JWT token and set authentication")
    void shouldProcessValidJwtTokenAndSetAuthentication() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String ipAddress = "127.0.0.1";

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn(ipAddress);

        given(jwt.getSubject()).willReturn("user123");
        given(jwt.getClaim("roles")).willReturn(List.of("ROLE_USER"));
        given(jwtDecoder.decode(token)).willReturn(jwt);

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

        given(request.getHeader("Authorization")).willReturn(authHeader);
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
    @DisplayName("Should handle expired JWT token")
    void shouldHandleExpiredJwtToken() throws ServletException, IOException {
        // Given
        String token = "expired.jwt.token";
        String authHeader = "Bearer " + token;
        String ipAddress = "10.0.0.1";

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn(ipAddress);
        given(response.getWriter()).willReturn(printWriter);
        given(jwtDecoder.decode(token)).willThrow(new JwtException("Token expired"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityAuditService).logAuthenticationFailure(anyString(), eq(ipAddress), eq("Token expired"));
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should proceed without authentication when no token provided")
    void shouldProceedWithoutAuthenticationWhenNoTokenProvided() throws ServletException, IOException {
        // Given
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

        given(request.getHeader("Authorization")).willReturn(authHeader);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(securityAuditService, never()).logAuthenticationSuccess(any());
        verify(securityAuditService, never()).logAuthenticationFailure(any(), any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should extract and set user authorities from JWT roles")
    void shouldExtractAndSetUserAuthoritiesFromJwtRoles() throws ServletException, IOException {
        // Given
        String token = "admin.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        given(jwt.getSubject()).willReturn("admin123");
        given(jwt.getClaim("roles")).willReturn(List.of("ROLE_ADMIN", "ROLE_USER"));
        given(jwtDecoder.decode(token)).willReturn(jwt);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).hasSize(2);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("Should handle JWT without roles claim")
    void shouldHandleJwtWithoutRolesClaim() throws ServletException, IOException {
        // Given
        String token = "noroles.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        given(jwt.getSubject()).willReturn("user456");
        given(jwt.getClaim("roles")).willReturn(null);
        given(jwtDecoder.decode(token)).willReturn(jwt);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
        verify(securityAuditService).logAuthenticationSuccess("127.0.0.1");
    }

    @Test
    @DisplayName("Should log suspicious activity for multiple failed attempts")
    void shouldLogSuspiciousActivityForMultipleFailedAttempts() throws ServletException, IOException {
        // Given
        String token = "malicious.token";
        String authHeader = "Bearer " + token;
        String ipAddress = "192.168.1.999";

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn(ipAddress);
        given(request.getHeader("User-Agent")).willReturn("MaliciousBot/1.0");
        given(response.getWriter()).willReturn(printWriter);
        given(jwtDecoder.decode(token)).willThrow(new JwtException("Malicious token detected"));
        given(tokenBlacklistService.isBlacklisted(any(Jwt.class))).willReturn(false);
        given(tokenBlacklistService.isUserBlacklisted(anyString())).willReturn(false);

        // When - Simulate multiple failed attempts to trigger suspicious activity
        for (int i = 0; i < 5; i++) {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
            // Reset the writer for each call
            reset(printWriter);
        }

        // Then
        verify(securityAuditService).logAuthenticationFailure(anyString(), eq(ipAddress), eq("Malicious token detected"));
        verify(securityAuditService).logSuspiciousActivity(eq("MULTIPLE_FAILED_AUTH_ATTEMPTS"), eq(ipAddress), any(Map.class));
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header when available")
    void shouldUseXForwardedForHeaderWhenAvailable() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String forwardedFor = "203.0.113.1";
        String remoteAddr = "10.0.0.1";

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getHeader("X-Forwarded-For")).willReturn(forwardedFor);

        given(jwt.getSubject()).willReturn("user123");
        given(jwt.getClaim("roles")).willReturn(null);
        given(jwtDecoder.decode(token)).willReturn(jwt);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityAuditService).logAuthenticationSuccess(forwardedFor);
    }

    @Test
    @DisplayName("Should handle correlation ID from request headers")
    void shouldHandleCorrelationIdFromRequestHeaders() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;
        String correlationId = "req-123-456";
        String ipAddress = "127.0.0.1";

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getHeader("X-Correlation-ID")).willReturn(correlationId);
        given(request.getRemoteAddr()).willReturn(ipAddress);

        given(jwt.getSubject()).willReturn("user123");
        given(jwt.getClaim("roles")).willReturn(null);
        given(jwtDecoder.decode(token)).willReturn(jwt);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityAuditService).logAuthenticationSuccessWithCorrelation(ipAddress, correlationId);
    }

    @Test
    @DisplayName("Should set security response headers")
    void shouldSetSecurityResponseHeaders() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String authHeader = "Bearer " + token;

        given(request.getHeader("Authorization")).willReturn(authHeader);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");

        given(jwt.getSubject()).willReturn("user123");
        given(jwt.getClaim("roles")).willReturn(null);
        given(jwtDecoder.decode(token)).willReturn(jwt);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(filterChain).doFilter(request, response);
    }
}