package com.focushive.identity.security;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.service.CustomUserDetailsService;
import com.focushive.identity.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests JWT token validation and Spring Security authentication context setup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private String validToken;
    private String authHeader;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .emailVerified(true)
                .createdAt(Instant.now())
                .personas(new ArrayList<>())
                .build();

        validToken = "valid.jwt.token";
        authHeader = "Bearer " + validToken;

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should authenticate user with valid token")
    void doFilterInternal_ValidToken_ShouldAuthenticateUser() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("testuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with missing authorization header")
    void doFilterInternal_MissingHeader_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with invalid token format")
    void doFilterInternal_InvalidTokenFormat_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with empty authorization header")
    void doFilterInternal_EmptyHeader_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with Bearer prefix only")
    void doFilterInternal_BearerPrefixOnly_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with invalid token")
    void doFilterInternal_InvalidToken_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate with blacklisted token")
    void doFilterInternal_BlacklistedToken_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).extractUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate when user not found")
    void doFilterInternal_UserNotFound_ShouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("testuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle exception during authentication gracefully")
    void doFilterInternal_ExceptionDuringAuth_ShouldHandleGracefully() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenThrow(new RuntimeException("Token validation error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle UserDetailsService exception gracefully")
    void doFilterInternal_UserDetailsException_ShouldHandleGracefully() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("testuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new RuntimeException("User service error"));

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract JWT from request header correctly")
    void getJwtFromRequest_ValidBearerToken_ShouldExtractToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken123");
        when(tokenProvider.validateToken("validtoken123")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("validtoken123")).thenReturn(false);
        when(tokenProvider.extractUsername("validtoken123")).thenReturn("testuser");
        when(tokenProvider.extractUserId("validtoken123")).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken("validtoken123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle case-sensitive Bearer prefix")
    void doFilterInternal_CaseSensitiveBearer_ShouldNotAuthenticate() throws Exception {
        // Given - lowercase bearer
        when(request.getHeader("Authorization")).thenReturn("bearer " + validToken);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext, never()).setAuthentication(any());
        verify(tokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should call filterChain even when authentication fails")
    void doFilterInternal_AuthenticationFails_ShouldCallFilterChain() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Invalid header");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should set authentication details from request")
    void doFilterInternal_ValidToken_ShouldSetAuthenticationDetails() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("testuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(securityContext).setAuthentication(argThat(auth -> {
            assertThat(auth.getPrincipal()).isEqualTo(testUser);
            assertThat(auth.getCredentials()).isNull();
            assertThat(auth.getAuthorities()).isEqualTo(testUser.getAuthorities());
            assertThat(auth.getDetails()).isNotNull();
            return true;
        }));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract username and user ID from token")
    void doFilterInternal_ValidToken_ShouldExtractUserInfo() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("specificuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername("specificuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).extractUsername(validToken);
        verify(tokenProvider).extractUserId(validToken);
        verify(userDetailsService).loadUserByUsername("specificuser");
        verify(securityContext).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle token with extra spaces in Bearer header")
    void doFilterInternal_BearerWithSpaces_ShouldExtractToken() throws Exception {
        // Given
        String tokenWithSpaces = "  " + validToken;
        when(request.getHeader("Authorization")).thenReturn("Bearer   " + validToken);
        when(tokenProvider.validateToken(tokenWithSpaces)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(tokenWithSpaces)).thenReturn(false);
        when(tokenProvider.extractUsername(tokenWithSpaces)).thenReturn("testuser");
        when(tokenProvider.extractUserId(tokenWithSpaces)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(tokenWithSpaces);
        verify(securityContext).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should validate all security checks in correct order")
    void doFilterInternal_SecurityChecks_ShouldFollowCorrectOrder() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(false);
        when(tokenProvider.extractUsername(validToken)).thenReturn("testuser");
        when(tokenProvider.extractUserId(validToken)).thenReturn(testUser.getId());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then - verify order of security checks
        var inOrder = inOrder(tokenProvider, tokenBlacklistService, userDetailsService, securityContext);
        inOrder.verify(tokenProvider).validateToken(validToken);
        inOrder.verify(tokenBlacklistService).isBlacklisted(validToken);
        inOrder.verify(tokenProvider).extractUsername(validToken);
        inOrder.verify(tokenProvider).extractUserId(validToken);
        inOrder.verify(userDetailsService).loadUserByUsername("testuser");
        inOrder.verify(securityContext).setAuthentication(any());
    }

    @Test
    @DisplayName("Should not call subsequent checks if token validation fails")
    void doFilterInternal_TokenValidationFails_ShouldStopSecurityChecks() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(validToken);
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(tokenProvider, never()).extractUsername(anyString());
        verify(tokenProvider, never()).extractUserId(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not call user service if token is blacklisted")
    void doFilterInternal_TokenBlacklisted_ShouldStopAtBlacklistCheck() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(tokenProvider.validateToken(validToken)).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted(validToken)).thenReturn(true);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(tokenProvider).validateToken(validToken);
        verify(tokenBlacklistService).isBlacklisted(validToken);
        verify(tokenProvider, never()).extractUsername(anyString());
        verify(tokenProvider, never()).extractUserId(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }
}