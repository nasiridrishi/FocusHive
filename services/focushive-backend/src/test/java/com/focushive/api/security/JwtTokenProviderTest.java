package com.focushive.api.security;

import com.focushive.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("test-jwt-signing-key-at-least-256-bits-long-for-validation", 3600000L);
        
        testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.UserRole.USER);
    }

    @Test
    void generateToken_withValidUser_returnsToken() {
        String token = tokenProvider.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void generateToken_withAuthentication_returnsToken() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(testUser);

        String token = tokenProvider.generateToken(auth);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void extractUserId_withValidToken_returnsUserId() {
        String token = tokenProvider.generateToken(testUser);

        String userId = tokenProvider.extractUserId(token);

        assertThat(userId).isEqualTo("test-user-id");
    }

    @Test
    void extractUsername_withValidToken_returnsUsername() {
        String token = tokenProvider.generateToken(testUser);

        String username = tokenProvider.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void extractEmail_withValidToken_returnsEmail() {
        String token = tokenProvider.generateToken(testUser);

        String email = tokenProvider.extractEmail(token);

        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    void extractRole_withValidToken_returnsRole() {
        String token = tokenProvider.generateToken(testUser);

        String role = tokenProvider.extractRole(token);

        assertThat(role).isEqualTo("USER");
    }

    @Test
    void extractAuthorities_withValidToken_returnsAuthorities() {
        String token = tokenProvider.generateToken(testUser);

        List<SimpleGrantedAuthority> authorities = tokenProvider.extractAuthorities(token);

        assertThat(authorities).hasSize(1);
        assertThat(authorities.get(0).getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = tokenProvider.generateToken(testUser);

        boolean isValid = tokenProvider.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        String invalidToken = "invalid.token.here";

        boolean isValid = tokenProvider.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider("test-jwt-signing-key-at-least-256-bits-long-for-validation", 1L);
        String token = shortLivedProvider.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isValid = shortLivedProvider.validateToken(token);

        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenExpired_withExpiredToken_returnsTrue() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider("test-jwt-signing-key-at-least-256-bits-long-for-validation", 1L);
        String token = shortLivedProvider.generateToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isExpired = shortLivedProvider.isTokenExpired(token);

        assertThat(isExpired).isTrue();
    }

    @Test
    void isTokenExpired_withValidToken_returnsFalse() {
        String token = tokenProvider.generateToken(testUser);

        boolean isExpired = tokenProvider.isTokenExpired(token);

        assertThat(isExpired).isFalse();
    }

    @Test
    void extractClaims_withValidToken_returnsClaims() {
        String token = tokenProvider.generateToken(testUser);

        Claims claims = tokenProvider.extractAllClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId", String.class)).isEqualTo("test-user-id");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void extractExpiration_withValidToken_returnsExpirationDate() {
        String token = tokenProvider.generateToken(testUser);

        Date expiration = tokenProvider.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void generateRefreshToken_withUser_returnsToken() {
        String refreshToken = tokenProvider.generateRefreshToken(testUser);

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    void canTokenBeRefreshed_withValidToken_returnsTrue() {
        String token = tokenProvider.generateToken(testUser);

        boolean canRefresh = tokenProvider.canTokenBeRefreshed(token);

        assertThat(canRefresh).isTrue();
    }

    @Test
    void refreshToken_withValidToken_returnsNewToken() {
        String oldToken = tokenProvider.generateToken(testUser);
        
        // Wait a bit to ensure different timestamps
        try {
            Thread.sleep(1000); // Wait 1 second to ensure different issuedAt timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String newToken = tokenProvider.refreshToken(oldToken);

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(tokenProvider.extractUsername(newToken)).isEqualTo("testuser");
    }
}