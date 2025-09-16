package com.focushive.backend.security;

import com.focushive.api.dto.identity.PersonaDto;
import com.focushive.api.dto.identity.TokenValidationResponse;
import com.focushive.api.service.IdentityIntegrationService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test for IdentityServiceAuthenticationFilter to verify it correctly delegates
 * JWT validation to the Identity Service and sets the security context.
 */
@ExtendWith(MockitoExtension.class)
public class IdentityServiceAuthenticationFilterUnitTest {

    @Mock
    private IdentityIntegrationService identityIntegrationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private IdentityServiceAuthenticationFilter filter;

    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private static final String INVALID_JWT = "invalid.jwt.token";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "testuser@focushive.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user with valid JWT from Identity Service")
    void testValidJwtAuthentication() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_JWT);

        TokenValidationResponse validResponse = new TokenValidationResponse();
        validResponse.setValid(true);
        validResponse.setUserId(USER_ID);
        validResponse.setEmail(EMAIL);
        validResponse.setAuthorities(Arrays.asList("ROLE_USER"));

        PersonaDto persona = new PersonaDto();
        persona.setId(UUID.randomUUID());
        persona.setName("Default");
        validResponse.setActivePersona(persona);

        when(identityIntegrationService.validateToken("Bearer " + VALID_JWT))
            .thenReturn(validResponse);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(identityIntegrationService, times(1)).validateToken("Bearer " + VALID_JWT);
        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(IdentityServicePrincipal.class);

        IdentityServicePrincipal principal = (IdentityServicePrincipal) auth.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo(EMAIL);
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    @DisplayName("Should not authenticate user with invalid JWT")
    void testInvalidJwtAuthentication() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + INVALID_JWT);

        TokenValidationResponse invalidResponse = new TokenValidationResponse();
        invalidResponse.setValid(false);
        invalidResponse.setErrorMessage("Token is expired");

        when(identityIntegrationService.validateToken("Bearer " + INVALID_JWT))
            .thenReturn(invalidResponse);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(identityIntegrationService, times(1)).validateToken("Bearer " + INVALID_JWT);
        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    @Test
    @DisplayName("Should pass through request without Authorization header")
    void testNoAuthorizationHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(identityIntegrationService, never()).validateToken(anyString());
        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    @Test
    @DisplayName("Should handle exception from Identity Service gracefully")
    void testIdentityServiceException() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_JWT);
        when(identityIntegrationService.validateToken("Bearer " + VALID_JWT))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(identityIntegrationService, times(1)).validateToken("Bearer " + VALID_JWT);
        verify(filterChain, times(1)).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull(); // Should continue without authentication
    }

    @Test
    @DisplayName("Should skip filter for public endpoints")
    void testShouldNotFilterForPublicEndpoints() throws Exception {
        // Given
        when(request.getServletPath()).thenReturn("/actuator/health");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("Should not skip filter for protected endpoints")
    void testShouldFilterForProtectedEndpoints() throws Exception {
        // Given
        when(request.getServletPath()).thenReturn("/api/v1/hives");

        // When
        boolean shouldNotFilter = filter.shouldNotFilter(request);

        // Then
        assertThat(shouldNotFilter).isFalse();
    }
}