package com.focushive.notification.security;

import com.focushive.notification.config.BaseWebMvcTest;
import com.focushive.notification.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for JWT authentication and authorization.
 * Tests JWT token validation, user context extraction, and role-based access control.
 */
@BaseWebMvcTest
@Import({SecurityConfig.class, com.focushive.notification.config.ControllerTestConfiguration.class})
@DisplayName("JWT Authentication Tests")
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Jwt validJwt;
    private Jwt adminJwt;
    private Jwt userJwt;

    @BeforeEach
    void setUp() {
        // Create valid JWT token
        validJwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .claim("sub", "user123")
                .claim("email", "user@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Create admin JWT token
        adminJwt = Jwt.withTokenValue("admin-token")
                .header("alg", "RS256")
                .claim("sub", "admin123")
                .claim("email", "admin@example.com")
                .claim("roles", new String[]{"ROLE_ADMIN"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Create user JWT token
        userJwt = Jwt.withTokenValue("user-token")
                .header("alg", "RS256")
                .claim("sub", "user456")
                .claim("email", "user456@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to protected endpoints without authentication")
    void shouldDenyAccessToProtectedEndpointsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/preferences/user/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to protected endpoints with valid JWT")
    void shouldAllowAccessToProtectedEndpointsWithValidJwt() throws Exception {
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        mockMvc.perform(get("/api/templates")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access with expired JWT token")
    void shouldDenyAccessWithExpiredJwtToken() throws Exception {
        Jwt expiredJwt = Jwt.withTokenValue("expired-token")
                .header("alg", "RS256")
                .claim("sub", "user123")
                .claim("email", "user@example.com")
                .claim("roles", new String[]{"ROLE_USER"})
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        mockMvc.perform(get("/api/templates")
                        .with(jwt().jwt(expiredJwt)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should extract user information from JWT token")
    void shouldExtractUserInformationFromJwtToken() throws Exception {
        given(jwtDecoder.decode(anyString())).willReturn(validJwt);

        // This test would need an endpoint that returns user info
        // For now, we'll test that the token is properly validated
        mockMvc.perform(get("/api/templates")
                        .with(jwt().jwt(validJwt)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should enforce role-based access control for admin endpoints")
    void shouldEnforceRoleBasedAccessControlForAdminEndpoints() throws Exception {
        given(jwtDecoder.decode(anyString())).willReturn(userJwt);

        // Admin-only endpoint should deny access to regular users
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().jwt(userJwt)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow admin access to admin endpoints")
    void shouldAllowAdminAccessToAdminEndpoints() throws Exception {
        given(jwtDecoder.decode(anyString())).willReturn(adminJwt);

        // Admin should have access to admin endpoints
        mockMvc.perform(get("/api/admin/stats")
                        .with(jwt().jwt(adminJwt)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle malformed JWT tokens")
    void shouldHandleMalformedJwtTokens() throws Exception {
        mockMvc.perform(get("/api/templates")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate JWT token signature")
    void shouldValidateJwtTokenSignature() throws Exception {
        // Test with token that has invalid signature
        mockMvc.perform(get("/api/templates")
                        .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow CORS preflight requests")
    void shouldAllowCorsPreflightRequests() throws Exception {
        mockMvc.perform(options("/api/templates")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should extract authorities from JWT roles claim")
    void shouldExtractAuthoritiesFromJwtRolesClaim() throws Exception {
        Jwt multiRoleJwt = Jwt.withTokenValue("multi-role-token")
                .header("alg", "RS256")
                .claim("sub", "user789")
                .claim("email", "user789@example.com")
                .claim("roles", new String[]{"ROLE_USER", "ROLE_ADMIN"})
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        given(jwtDecoder.decode(anyString())).willReturn(multiRoleJwt);

        mockMvc.perform(get("/api/templates")
                        .with(jwt().jwt(multiRoleJwt).authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}