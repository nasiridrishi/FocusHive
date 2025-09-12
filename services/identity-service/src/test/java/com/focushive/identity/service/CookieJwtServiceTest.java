package com.focushive.identity.service;

import com.focushive.identity.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CookieJwtService.
 * Tests all cookie operations, security attributes, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CookieJwtService Tests")
class CookieJwtServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CookieJwtService cookieJwtService;

    private static final String TEST_ACCESS_TOKEN = "test.access.token";
    private static final String TEST_REFRESH_TOKEN = "test.refresh.token";
    private static final String ACCESS_COOKIE_NAME = "access_token";
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long ACCESS_TOKEN_EXPIRY = 900L; // 15 minutes
    private static final int REFRESH_TOKEN_EXPIRY = 2592000; // 30 days

    @BeforeEach
    void setUp() {
        // Set up default configuration values using reflection
        ReflectionTestUtils.setField(cookieJwtService, "accessTokenCookieName", ACCESS_COOKIE_NAME);
        ReflectionTestUtils.setField(cookieJwtService, "refreshTokenCookieName", REFRESH_COOKIE_NAME);
        ReflectionTestUtils.setField(cookieJwtService, "cookieDomain", null);
        ReflectionTestUtils.setField(cookieJwtService, "cookiePath", "/");
        ReflectionTestUtils.setField(cookieJwtService, "secureCookies", true);
        ReflectionTestUtils.setField(cookieJwtService, "sameSite", "Strict");
    }

    @Nested
    @DisplayName("Access Token Cookie Operations")
    class AccessTokenCookieTests {

        @Test
        @DisplayName("Should set access token cookie with correct security attributes")
        void shouldSetAccessTokenCookieWithSecurityAttributes() {
            // Given
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo(ACCESS_COOKIE_NAME);
            assertThat(capturedCookie.getValue()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo((int) ACCESS_TOKEN_EXPIRY);

            // Verify Set-Cookie header includes SameSite
            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).contains("access_token=" + TEST_ACCESS_TOKEN);
            assertThat(headerValue).contains("HttpOnly");
            assertThat(headerValue).contains("Secure");
            assertThat(headerValue).contains("SameSite=Strict");
            assertThat(headerValue).contains("Max-Age=" + ACCESS_TOKEN_EXPIRY);
            assertThat(headerValue).contains("Path=/");
        }

        @Test
        @DisplayName("Should set access token cookie with domain when configured")
        void shouldSetAccessTokenCookieWithDomain() {
            // Given
            String testDomain = "focushive.com";
            ReflectionTestUtils.setField(cookieJwtService, "cookieDomain", testDomain);
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getDomain()).isEqualTo(testDomain);

            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).contains("Domain=" + testDomain);
        }

        @Test
        @DisplayName("Should not set secure flag when secureCookies is false")
        void shouldNotSetSecureFlagWhenDisabled() {
            // Given
            ReflectionTestUtils.setField(cookieJwtService, "secureCookies", false);
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getSecure()).isFalse();

            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).doesNotContain("Secure");
        }

        @Test
        @DisplayName("Should get access token from cookie")
        void shouldGetAccessTokenFromCookie() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, TEST_ACCESS_TOKEN);
            Cookie[] cookies = {accessCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            String token = cookieJwtService.getAccessTokenFromCookie(request);

            // Then
            assertThat(token).isEqualTo(TEST_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should return null when no cookies present")
        void shouldReturnNullWhenNoCookiesPresent() {
            // Given
            when(request.getCookies()).thenReturn(null);

            // When
            String token = cookieJwtService.getAccessTokenFromCookie(request);

            // Then
            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Should return null when access token cookie not found")
        void shouldReturnNullWhenAccessTokenCookieNotFound() {
            // Given
            Cookie otherCookie = new Cookie("other_cookie", "other_value");
            Cookie[] cookies = {otherCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            String token = cookieJwtService.getAccessTokenFromCookie(request);

            // Then
            assertThat(token).isNull();
        }

        @Test
        @DisplayName("Should clear access token cookie")
        void shouldClearAccessTokenCookie() {
            // Given
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.clearAccessTokenCookie(response);

            // Then
            verify(response).addCookie(cookieCaptor.capture());
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo(ACCESS_COOKIE_NAME);
            assertThat(capturedCookie.getValue()).isEmpty();
            assertThat(capturedCookie.getMaxAge()).isZero();
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();

            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).contains(ACCESS_COOKIE_NAME + "=");
            assertThat(headerValue).contains("Max-Age=0");
        }
    }

    @Nested
    @DisplayName("Refresh Token Cookie Operations")
    class RefreshTokenCookieTests {

        @Test
        @DisplayName("Should set refresh token cookie with correct security attributes")
        void shouldSetRefreshTokenCookieWithSecurityAttributes() {
            // Given
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.setRefreshTokenCookie(response, TEST_REFRESH_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo(REFRESH_COOKIE_NAME);
            assertThat(capturedCookie.getValue()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(capturedCookie.isHttpOnly()).isTrue();
            assertThat(capturedCookie.getSecure()).isTrue();
            assertThat(capturedCookie.getPath()).isEqualTo("/");
            assertThat(capturedCookie.getMaxAge()).isEqualTo(REFRESH_TOKEN_EXPIRY);

            // Verify Set-Cookie header includes SameSite
            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).contains("refresh_token=" + TEST_REFRESH_TOKEN);
            assertThat(headerValue).contains("HttpOnly");
            assertThat(headerValue).contains("Secure");
            assertThat(headerValue).contains("SameSite=Strict");
            assertThat(headerValue).contains("Max-Age=" + REFRESH_TOKEN_EXPIRY);
        }

        @Test
        @DisplayName("Should get refresh token from cookie")
        void shouldGetRefreshTokenFromCookie() {
            // Given
            Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN);
            Cookie[] cookies = {refreshCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            String token = cookieJwtService.getRefreshTokenFromCookie(request);

            // Then
            assertThat(token).isEqualTo(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Should clear refresh token cookie")
        void shouldClearRefreshTokenCookie() {
            // Given
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

            // When
            cookieJwtService.clearRefreshTokenCookie(response);

            // Then
            verify(response).addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getName()).isEqualTo(REFRESH_COOKIE_NAME);
            assertThat(capturedCookie.getValue()).isEmpty();
            assertThat(capturedCookie.getMaxAge()).isZero();
        }
    }

    @Nested
    @DisplayName("Cookie Validation Tests")
    class CookieValidationTests {

        @Test
        @DisplayName("Should return true for valid access token cookie")
        void shouldReturnTrueForValidAccessTokenCookie() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, TEST_ACCESS_TOKEN);
            Cookie[] cookies = {accessCookie};
            when(request.getCookies()).thenReturn(cookies);
            when(tokenProvider.validateToken(TEST_ACCESS_TOKEN)).thenReturn(true);

            // When
            boolean isValid = cookieJwtService.hasValidAccessTokenCookie(request);

            // Then
            assertThat(isValid).isTrue();
            verify(tokenProvider).validateToken(TEST_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should return false for invalid access token cookie")
        void shouldReturnFalseForInvalidAccessTokenCookie() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, "invalid.token");
            Cookie[] cookies = {accessCookie};
            when(request.getCookies()).thenReturn(cookies);
            when(tokenProvider.validateToken("invalid.token")).thenReturn(false);

            // When
            boolean isValid = cookieJwtService.hasValidAccessTokenCookie(request);

            // Then
            assertThat(isValid).isFalse();
            verify(tokenProvider).validateToken("invalid.token");
        }

        @Test
        @DisplayName("Should return false when no access token cookie present")
        void shouldReturnFalseWhenNoAccessTokenCookie() {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[0]);

            // When
            boolean isValid = cookieJwtService.hasValidAccessTokenCookie(request);

            // Then
            assertThat(isValid).isFalse();
            verify(tokenProvider, never()).validateToken(any());
        }

        @Test
        @DisplayName("Should return false when access token cookie is empty")
        void shouldReturnFalseWhenAccessTokenCookieIsEmpty() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, "");
            Cookie[] cookies = {accessCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            boolean isValid = cookieJwtService.hasValidAccessTokenCookie(request);

            // Then
            assertThat(isValid).isFalse();
            verify(tokenProvider, never()).validateToken(any());
        }
    }

    @Nested
    @DisplayName("JWT Extraction Tests")
    class JwtExtractionTests {

        @Test
        @DisplayName("Should extract JWT from cookie when present")
        void shouldExtractJwtFromCookieWhenPresent() {
            // Given
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, TEST_ACCESS_TOKEN);
            Cookie[] cookies = {accessCookie};
            when(request.getCookies()).thenReturn(cookies);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + "header.token");

            // When
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            // Then - Cookie should take precedence over header
            assertThat(jwt).isEqualTo(TEST_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should extract JWT from Authorization header when cookie not present")
        void shouldExtractJwtFromHeaderWhenCookieNotPresent() {
            // Given
            String headerToken = "header.token.value";
            when(request.getCookies()).thenReturn(new Cookie[0]);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + headerToken);

            // When
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            // Then
            assertThat(jwt).isEqualTo(headerToken);
        }

        @Test
        @DisplayName("Should return null when neither cookie nor header present")
        void shouldReturnNullWhenNeitherCookieNorHeaderPresent() {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[0]);
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            // Then
            assertThat(jwt).isNull();
        }

        @Test
        @DisplayName("Should return null when Authorization header has invalid format")
        void shouldReturnNullWhenAuthHeaderHasInvalidFormat() {
            // Given
            when(request.getCookies()).thenReturn(new Cookie[0]);
            when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");

            // When
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            // Then
            assertThat(jwt).isNull();
        }

        @Test
        @DisplayName("Should handle null cookies array")
        void shouldHandleNullCookiesArray() {
            // Given
            when(request.getCookies()).thenReturn(null);
            when(request.getHeader("Authorization")).thenReturn("Bearer fallback.token");

            // When
            String jwt = cookieJwtService.extractJwtFromRequest(request);

            // Then
            assertThat(jwt).isEqualTo("fallback.token");
        }
    }

    @Nested
    @DisplayName("Clear All Cookies Tests")
    class ClearAllCookiesTests {

        @Test
        @DisplayName("Should clear both access and refresh token cookies")
        void shouldClearBothAccessAndRefreshTokenCookies() {
            // Given
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

            // When
            cookieJwtService.clearAllCookies(response);

            // Then
            verify(response, times(2)).addCookie(cookieCaptor.capture());
            verify(response, times(2)).addHeader(eq("Set-Cookie"), any(String.class));

            var capturedCookies = cookieCaptor.getAllValues();
            assertThat(capturedCookies).hasSize(2);

            // Verify access token cookie is cleared
            Cookie accessCookie = capturedCookies.stream()
                    .filter(c -> ACCESS_COOKIE_NAME.equals(c.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(accessCookie.getValue()).isEmpty();
            assertThat(accessCookie.getMaxAge()).isZero();

            // Verify refresh token cookie is cleared
            Cookie refreshCookie = capturedCookies.stream()
                    .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                    .findFirst()
                    .orElseThrow();
            assertThat(refreshCookie.getValue()).isEmpty();
            assertThat(refreshCookie.getMaxAge()).isZero();
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use custom cookie names when configured")
        void shouldUseCustomCookieNamesWhenConfigured() {
            // Given
            String customAccessName = "custom_access";
            String customRefreshName = "custom_refresh";
            ReflectionTestUtils.setField(cookieJwtService, "accessTokenCookieName", customAccessName);
            ReflectionTestUtils.setField(cookieJwtService, "refreshTokenCookieName", customRefreshName);
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);
            cookieJwtService.setRefreshTokenCookie(response, TEST_REFRESH_TOKEN);

            // Then
            verify(response, times(2)).addCookie(cookieCaptor.capture());

            var capturedCookies = cookieCaptor.getAllValues();
            assertThat(capturedCookies.get(0).getName()).isEqualTo(customAccessName);
            assertThat(capturedCookies.get(1).getName()).isEqualTo(customRefreshName);
        }

        @Test
        @DisplayName("Should use custom path when configured")
        void shouldUseCustomPathWhenConfigured() {
            // Given
            String customPath = "/api/auth";
            ReflectionTestUtils.setField(cookieJwtService, "cookiePath", customPath);
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getPath()).isEqualTo(customPath);
        }

        @Test
        @DisplayName("Should use custom SameSite attribute")
        void shouldUseCustomSameSiteAttribute() {
            // Given
            String customSameSite = "Lax";
            ReflectionTestUtils.setField(cookieJwtService, "sameSite", customSameSite);
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addHeader(eq("Set-Cookie"), headerCaptor.capture());

            String headerValue = headerCaptor.getValue();
            assertThat(headerValue).contains("SameSite=" + customSameSite);
        }

        @Test
        @DisplayName("Should handle empty domain configuration")
        void shouldHandleEmptyDomainConfiguration() {
            // Given
            ReflectionTestUtils.setField(cookieJwtService, "cookieDomain", "");
            when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

            // When
            cookieJwtService.setAccessTokenCookie(response, TEST_ACCESS_TOKEN);

            // Then
            verify(response).addCookie(cookieCaptor.capture());

            Cookie capturedCookie = cookieCaptor.getValue();
            assertThat(capturedCookie.getDomain()).isNull();
        }
    }

    @Nested
    @DisplayName("Multiple Cookies Handling Tests")
    class MultipleCookiesHandlingTests {

        @Test
        @DisplayName("Should find correct token among multiple cookies")
        void shouldFindCorrectTokenAmongMultipleCookies() {
            // Given
            Cookie sessionCookie = new Cookie("JSESSIONID", "session123");
            Cookie accessCookie = new Cookie(ACCESS_COOKIE_NAME, TEST_ACCESS_TOKEN);
            Cookie refreshCookie = new Cookie(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN);
            Cookie otherCookie = new Cookie("other", "value");
            Cookie[] cookies = {sessionCookie, accessCookie, refreshCookie, otherCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            String accessToken = cookieJwtService.getAccessTokenFromCookie(request);
            String refreshToken = cookieJwtService.getRefreshTokenFromCookie(request);

            // Then
            assertThat(accessToken).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(refreshToken).isEqualTo(TEST_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("Should handle duplicate cookie names by returning first match")
        void shouldHandleDuplicateCookieNamesByReturningFirstMatch() {
            // Given
            Cookie firstAccessCookie = new Cookie(ACCESS_COOKIE_NAME, "first.token");
            Cookie secondAccessCookie = new Cookie(ACCESS_COOKIE_NAME, "second.token");
            Cookie[] cookies = {firstAccessCookie, secondAccessCookie};
            when(request.getCookies()).thenReturn(cookies);

            // When
            String token = cookieJwtService.getAccessTokenFromCookie(request);

            // Then - Should return the first matching cookie
            assertThat(token).isEqualTo("first.token");
        }
    }
}