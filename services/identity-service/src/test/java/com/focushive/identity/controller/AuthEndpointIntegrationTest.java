package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.service.CookieJwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for AuthController using pure Mockito.
 * Tests controller logic without Spring Boot integration complexity.
 */
@ExtendWith(MockitoExtension.class)
class AuthEndpointIntegrationTest {
    
    @Mock
    private AuthenticationService authenticationService;
    
    @Mock
    private CookieJwtService cookieJwtService;
    
    @Mock
    private HttpServletResponse httpServletResponse;
    
    @InjectMocks
    private AuthController authController;
    
    @Test
    void shouldLoginSuccessfully_WhenValidCredentialsProvided() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
        
        AuthenticationResponse expectedResponse = AuthenticationResponse.builder()
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .username("testuser")
            .email("test@example.com")
            .build();
        
        when(authenticationService.login(any(LoginRequest.class)))
            .thenReturn(expectedResponse);
        
        // When
        ResponseEntity<AuthenticationResponse> response = authController.login(
            loginRequest, httpServletResponse);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getBody().getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        
        // Verify that service was called and cookies were set
        verify(authenticationService).login(eq(loginRequest));
        verify(cookieJwtService).setAccessTokenCookie(eq(httpServletResponse), eq("mock-access-token"));
        verify(cookieJwtService).setRefreshTokenCookie(eq(httpServletResponse), eq("mock-refresh-token"));
    }
}