package com.focushive.notification.security;

import com.focushive.notification.service.UserContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * Test class for UserContextService.
 * Tests extraction of user information from JWT tokens.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextService Tests")
class UserContextServiceTest {

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserContextService userContextService;

    private Jwt testJwt;
    private JwtAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        testJwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claim("sub", "user123")
                .claim("email", "user@example.com")
                .claim("name", "Test User")
                .claim("roles", Arrays.asList("ROLE_USER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        authenticationToken = new JwtAuthenticationToken(testJwt, Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER")
        ));

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Should extract user ID from JWT token")
    void shouldExtractUserIdFromJwtToken() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When
        String userId = userContextService.getCurrentUserId();

        // Then
        assertThat(userId).isEqualTo("user123");
    }

    @Test
    @DisplayName("Should extract user email from JWT token")
    void shouldExtractUserEmailFromJwtToken() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When
        String email = userContextService.getCurrentUserEmail();

        // Then
        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Should extract user name from JWT token")
    void shouldExtractUserNameFromJwtToken() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When
        String name = userContextService.getCurrentUserName();

        // Then
        assertThat(name).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should extract user roles from JWT token")
    void shouldExtractUserRolesFromJwtToken() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When
        List<String> roles = userContextService.getCurrentUserRoles();

        // Then
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should check if user has specific role")
    void shouldCheckIfUserHasSpecificRole() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When & Then
        assertThat(userContextService.hasRole("ROLE_USER")).isTrue();
        assertThat(userContextService.hasRole("ROLE_ADMIN")).isFalse();
    }

    @Test
    @DisplayName("Should check if user has admin role")
    void shouldCheckIfUserHasAdminRole() {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("admin-token")
                .header("alg", "RS256")
                .claim("sub", "admin123")
                .claim("email", "admin@example.com")
                .claim("roles", Arrays.asList("ROLE_ADMIN", "ROLE_USER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken adminToken = new JwtAuthenticationToken(adminJwt, Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
        ));

        given(securityContext.getAuthentication()).willReturn(adminToken);

        // When & Then
        assertThat(userContextService.isAdmin()).isTrue();
        assertThat(userContextService.hasRole("ROLE_ADMIN")).isTrue();
    }

    @Test
    @DisplayName("Should return null when no authentication context")
    void shouldReturnNullWhenNoAuthenticationContext() {
        // Given
        given(securityContext.getAuthentication()).willReturn(null);

        // When & Then
        assertThat(userContextService.getCurrentUserId()).isNull();
        assertThat(userContextService.getCurrentUserEmail()).isNull();
        assertThat(userContextService.getCurrentUserName()).isNull();
    }

    @Test
    @DisplayName("Should handle non-JWT authentication")
    void shouldHandleNonJwtAuthentication() {
        // Given
        Authentication nonJwtAuth = org.mockito.Mockito.mock(Authentication.class);
        given(securityContext.getAuthentication()).willReturn(nonJwtAuth);

        // When & Then
        assertThat(userContextService.getCurrentUserId()).isNull();
        assertThat(userContextService.getCurrentUserEmail()).isNull();
        assertThat(userContextService.getCurrentUserName()).isNull();
    }

    @Test
    @DisplayName("Should get user context object")
    void shouldGetUserContextObject() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When
        UserContextService.UserContext userContext = userContextService.getCurrentUserContext();

        // Then
        assertThat(userContext).isNotNull();
        assertThat(userContext.userId()).isEqualTo("user123");
        assertThat(userContext.email()).isEqualTo("user@example.com");
        assertThat(userContext.name()).isEqualTo("Test User");
        assertThat(userContext.roles()).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should validate user can access own resources")
    void shouldValidateUserCanAccessOwnResources() {
        // Given
        given(securityContext.getAuthentication()).willReturn(authenticationToken);

        // When & Then
        assertThat(userContextService.canAccessResource("user123")).isTrue();
        assertThat(userContextService.canAccessResource("other-user")).isFalse();
    }

    @Test
    @DisplayName("Should allow admin to access any resources")
    void shouldAllowAdminToAccessAnyResources() {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("admin-token")
                .header("alg", "RS256")
                .claim("sub", "admin123")
                .claim("email", "admin@example.com")
                .claim("roles", Arrays.asList("ROLE_ADMIN"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken adminToken = new JwtAuthenticationToken(adminJwt, Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        given(securityContext.getAuthentication()).willReturn(adminToken);

        // When & Then
        assertThat(userContextService.canAccessResource("any-user")).isTrue();
        assertThat(userContextService.canAccessResource("admin123")).isTrue();
    }

    @Test
    @DisplayName("Should handle missing claims gracefully")
    void shouldHandleMissingClaimsGracefully() {
        // Given
        Jwt minimalJwt = Jwt.withTokenValue("minimal-token")
                .header("alg", "RS256")
                .claim("sub", "user456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken minimalToken = new JwtAuthenticationToken(minimalJwt, Arrays.asList());
        given(securityContext.getAuthentication()).willReturn(minimalToken);

        // When & Then
        assertThat(userContextService.getCurrentUserId()).isEqualTo("user456");
        assertThat(userContextService.getCurrentUserEmail()).isNull();
        assertThat(userContextService.getCurrentUserName()).isNull();
        assertThat(userContextService.getCurrentUserRoles()).isEmpty();
    }
}