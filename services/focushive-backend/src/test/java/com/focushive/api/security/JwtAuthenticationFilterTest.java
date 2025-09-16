package com.focushive.api.security;

import com.focushive.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Enhanced JWT Authentication Filter
 * These tests should FAIL initially - implementing TDD RED phase
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private JwtAuthenticationFilter authenticationFilter;
    private User testUser;

    @BeforeEach
    void setUp() {
        // This enhanced filter doesn't exist yet - will fail
        authenticationFilter = new JwtAuthenticationFilter(tokenProvider);

        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.USER);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldAuthenticateWithValidJwtToken() throws ServletException, IOException {
        // Given
        String token = "valid.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(true); // This method doesn't exist yet
        when(tokenProvider.extractUsername(token)).thenReturn("testuser");
        when(tokenProvider.extractUserId(token)).thenReturn("test-user-id");
        when(tokenProvider.extractEmail(token)).thenReturn("test@example.com");
        when(tokenProvider.extractAuthorities(token)).thenReturn(
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectBlacklistedToken() throws ServletException, IOException {
        // Given
        String token = "blacklisted.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(false); // Blacklisted

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response); // Continue without auth
    }

    @Test
    void shouldHandleExpiredToken() throws ServletException, IOException {
        // Given
        String token = "expired.jwt.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(false);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleInvalidSignature() throws ServletException, IOException {
        // Given
        String token = "invalid.signature.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(false);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenNoToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(tokenProvider);
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleMalformedAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidHeader");

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verifyNoInteractions(tokenProvider);
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipFilterForPublicEndpoints() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/actuator/health");

        // When
        boolean shouldSkip = authenticationFilter.shouldNotFilter(request);

        // Then - This method needs to be implemented
        assertThat(shouldSkip).isTrue();
    }

    @Test
    void shouldNotSkipFilterForProtectedEndpoints() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/hives");

        // When
        boolean shouldSkip = authenticationFilter.shouldNotFilter(request);

        // Then
        assertThat(shouldSkip).isFalse();
    }

    @Test
    void shouldMeetPerformanceRequirements() throws ServletException, IOException {
        // Given
        String token = "performance.test.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(true);
        when(tokenProvider.extractUsername(token)).thenReturn("testuser");
        when(tokenProvider.extractUserId(token)).thenReturn("test-user-id");
        when(tokenProvider.extractEmail(token)).thenReturn("test@example.com");
        when(tokenProvider.extractAuthorities(token)).thenReturn(
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When - Measure performance
        long startTime = System.currentTimeMillis();
        authenticationFilter.doFilterInternal(request, response, filterChain);
        long endTime = System.currentTimeMillis();

        // Then - Should complete within performance requirements
        long duration = endTime - startTime;
        assertThat(duration).isLessThan(10L); // < 10ms requirement
    }

    @Test
    void shouldHandleExceptionsGracefully() throws ServletException, IOException {
        // Given
        String token = "exception.test.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When - Should not propagate exception
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response); // Should continue
    }

    @Test
    void shouldExtractUserContextFromToken() throws ServletException, IOException {
        // Given
        String token = "context.test.token";
        String bearerToken = "Bearer " + token;

        when(request.getHeader("Authorization")).thenReturn(bearerToken);
        when(tokenProvider.validateTokenWithBlacklist(token)).thenReturn(true);
        when(tokenProvider.extractUsername(token)).thenReturn("testuser");
        when(tokenProvider.extractUserId(token)).thenReturn("test-user-id");
        when(tokenProvider.extractEmail(token)).thenReturn("test@example.com");
        when(tokenProvider.extractAuthorities(token)).thenReturn(
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When
        authenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - Should create authentication with user context
        verify(securityContext).setAuthentication(argThat(auth -> {
            assertThat(auth.getName()).isEqualTo("testuser");
            assertThat(auth.getAuthorities()).hasSize(1);
            assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            return true;
        }));
    }
}