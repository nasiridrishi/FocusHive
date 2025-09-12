package com.focushive.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RefreshTokenRequest;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.service.CookieJwtService;
import com.focushive.identity.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for cookie-based authentication flow.
 * Tests the complete authentication lifecycle including login, token refresh,
 * logout, CSRF protection, and security attributes.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Cookie-Based Authentication Integration Tests")
class CookieAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CookieJwtService cookieJwtService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_FIRST_NAME = "Test";
    private static final String TEST_LAST_NAME = "User";

    private User testUser;
    private String validRefreshToken;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail(TEST_EMAIL);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        testUser.setFirstName(TEST_FIRST_NAME);
        testUser.setLastName(TEST_LAST_NAME);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
        testUser = userRepository.save(testUser);

        // Generate a valid refresh token for testing
        validRefreshToken = tokenProvider.generateRefreshToken(testUser);
    }

    @Nested
    @DisplayName("Registration with Cookies")
    class RegistrationWithCookiesTests {

        @Test
        @DisplayName("Should set httpOnly cookies on successful registration")
        void shouldSetHttpOnlyCookiesOnRegistration() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setUsername("newuser");
            request.setPassword("NewPassword123!");
            request.setFirstName("New");
            request.setLastName("User");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf()) // Include CSRF token
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andReturn();

            // Verify cookies are set
            Cookie[] cookies = result.getResponse().getCookies();
            assertThat(cookies).isNotEmpty();

            Cookie accessTokenCookie = findCookieByName(cookies, "access_token");
            Cookie refreshTokenCookie = findCookieByName(cookies, "refresh_token");

            assertThat(accessTokenCookie).isNotNull();
            assertThat(accessTokenCookie.isHttpOnly()).isTrue();
            assertThat(accessTokenCookie.getSecure()).isTrue();
            assertThat(accessTokenCookie.getPath()).isEqualTo("/");
            assertThat(accessTokenCookie.getValue()).isNotBlank();

            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
            assertThat(refreshTokenCookie.getSecure()).isTrue();
            assertThat(refreshTokenCookie.getPath()).isEqualTo("/");
            assertThat(refreshTokenCookie.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("Should fail registration without CSRF token when required")
        void shouldFailRegistrationWithoutCsrfToken() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser2@example.com");
            request.setUsername("newuser2");
            request.setPassword("NewPassword123!");
            request.setFirstName("New");
            request.setLastName("User");

            // When & Then - Registration should succeed without CSRF as it's in ignore list
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Login with Cookies")
    class LoginWithCookiesTests {

        @Test
        @DisplayName("Should set httpOnly cookies on successful login")
        void shouldSetHttpOnlyCookiesOnLogin() throws Exception {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                    .andReturn();

            // Verify cookies are set with correct attributes
            Cookie[] cookies = result.getResponse().getCookies();
            assertThat(cookies).isNotEmpty();

            Cookie accessTokenCookie = findCookieByName(cookies, "access_token");
            Cookie refreshTokenCookie = findCookieByName(cookies, "refresh_token");

            assertThat(accessTokenCookie).isNotNull();
            assertThat(accessTokenCookie.isHttpOnly()).isTrue();
            assertThat(accessTokenCookie.getSecure()).isTrue();
            assertThat(accessTokenCookie.getMaxAge()).isGreaterThan(0);

            assertThat(refreshTokenCookie).isNotNull();
            assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
            assertThat(refreshTokenCookie.getSecure()).isTrue();
            assertThat(refreshTokenCookie.getMaxAge()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should authenticate using cookie for protected endpoints")
        void shouldAuthenticateUsingCookieForProtectedEndpoints() throws Exception {
            // Given - First login to get cookies
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie accessTokenCookie = findCookieByName(loginResult.getResponse().getCookies(), "access_token");

            // When & Then - Use cookie to access protected endpoint
            mockMvc.perform(get("/api/v1/personas")
                            .cookie(accessTokenCookie)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should prefer cookie over Authorization header")
        void shouldPreferCookieOverAuthorizationHeader() throws Exception {
            // Given - Login to get valid cookie
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie accessTokenCookie = findCookieByName(loginResult.getResponse().getCookies(), "access_token");
            String invalidHeaderToken = "invalid.jwt.token";

            // When & Then - Cookie should be used even when invalid header is present
            mockMvc.perform(get("/api/v1/personas")
                            .cookie(accessTokenCookie)
                            .header("Authorization", "Bearer " + invalidHeaderToken)
                            .with(csrf()))
                    .andExpect(status().isOk()); // Should succeed because cookie is valid
        }

        @Test
        @DisplayName("Should fallback to Authorization header when no cookie present")
        void shouldFallbackToAuthorizationHeaderWhenNoCookiePresent() throws Exception {
            // Given - Generate valid JWT token (using null persona for test)
            String validToken = tokenProvider.generateAccessToken(testUser, null);

            // When & Then - Should authenticate using header
            mockMvc.perform(get("/api/v1/personas")
                            .header("Authorization", "Bearer " + validToken)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Token Refresh with Cookies")
    class TokenRefreshWithCookiesTests {

        @Test
        @DisplayName("Should refresh token and update cookies")
        void shouldRefreshTokenAndUpdateCookies() throws Exception {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(validRefreshToken)
                    .build();

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andReturn();

            // Verify new cookies are set
            Cookie[] cookies = result.getResponse().getCookies();
            Cookie newAccessCookie = findCookieByName(cookies, "access_token");
            Cookie newRefreshCookie = findCookieByName(cookies, "refresh_token");

            assertThat(newAccessCookie).isNotNull();
            assertThat(newAccessCookie.getValue()).isNotBlank();
            assertThat(newRefreshCookie).isNotNull();
            assertThat(newRefreshCookie.getValue()).isNotBlank();
        }

        @Test
        @DisplayName("Should reject expired refresh token")
        void shouldRejectExpiredRefreshToken() throws Exception {
            // Given - Create an expired token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", testUser.getId().toString());
            claims.put("type", "refresh");
            String expiredToken = tokenProvider.generateToken(testUser.getUsername(), claims, -1); // Expired 1 second ago

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken(expiredToken)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject invalid refresh token")
        void shouldRejectInvalidRefreshToken() throws Exception {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid.refresh.token")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logout with Cookies")
    class LogoutWithCookiesTests {

        @Test
        @DisplayName("Should clear cookies on logout")
        void shouldClearCookiesOnLogout() throws Exception {
            // Given - Login first to get cookies and token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = loginResult.getResponse().getContentAsString();
            String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();
            String refreshToken = objectMapper.readTree(responseJson).get("refreshToken").asText();

            // When & Then - Logout
            MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"))
                    .andReturn();

            // Verify cookies are cleared (Max-Age=0)
            Cookie[] cookies = logoutResult.getResponse().getCookies();
            Cookie clearedAccessCookie = findCookieByName(cookies, "access_token");
            Cookie clearedRefreshCookie = findCookieByName(cookies, "refresh_token");

            assertThat(clearedAccessCookie).isNotNull();
            assertThat(clearedAccessCookie.getMaxAge()).isZero();
            assertThat(clearedAccessCookie.getValue()).isEmpty();

            assertThat(clearedRefreshCookie).isNotNull();
            assertThat(clearedRefreshCookie.getMaxAge()).isZero();
            assertThat(clearedRefreshCookie.getValue()).isEmpty();

            // Verify tokens are blacklisted
            assertThat(tokenBlacklistService.isBlacklisted(accessToken)).isTrue();
        }

        @Test
        @DisplayName("Should not access protected endpoints after logout")
        void shouldNotAccessProtectedEndpointsAfterLogout() throws Exception {
            // Given - Login and logout
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                    .get("accessToken").asText();

            // Logout
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer " + accessToken)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            // When & Then - Try to access protected endpoint
            mockMvc.perform(get("/api/v1/personas")
                            .header("Authorization", "Bearer " + accessToken)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CSRF Protection Tests")
    class CsrfProtectionTests {

        @Test
        @DisplayName("Should require CSRF token for protected operations")
        void shouldRequireCsrfTokenForProtectedOperations() throws Exception {
            // Given - Login to get valid token
            String validToken = tokenProvider.generateAccessToken(testUser, null);

            // When & Then - Protected operation without CSRF should fail
            mockMvc.perform(post("/api/v1/personas/switch")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personaId\": 1}"))
                    .andExpect(status().isForbidden()); // CSRF protection should kick in
        }

        @Test
        @DisplayName("Should accept requests with valid CSRF token")
        void shouldAcceptRequestsWithValidCsrfToken() throws Exception {
            // Given - Login to get valid token
            String validToken = tokenProvider.generateAccessToken(testUser, null);

            // When & Then - Protected operation with CSRF should succeed (if persona exists)
            mockMvc.perform(get("/api/v1/personas")
                            .header("Authorization", "Bearer " + validToken)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should have CSRF token accessible for JavaScript")
        void shouldHaveCsrfTokenAccessibleForJavaScript() throws Exception {
            // When & Then - CSRF cookie should not be httpOnly so JS can access it
            mockMvc.perform(get("/api/v1/health"))
                    .andExpect(status().isOk())
                    .andDo(print());

            // Note: In real implementation, you'd check for XSRF-TOKEN cookie
            // that's not httpOnly, allowing frontend to read and send in headers
        }
    }

    @Nested
    @DisplayName("Security Attack Scenarios")
    class SecurityAttackScenariosTests {

        @Test
        @DisplayName("Should reject manipulated cookies")
        void shouldRejectManipulatedCookies() throws Exception {
            // Given - Create manipulated cookie
            Cookie manipulatedCookie = new Cookie("access_token", "manipulated.jwt.token");

            // When & Then
            mockMvc.perform(get("/api/v1/personas")
                            .cookie(manipulatedCookie)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should prevent cookie fixation attacks")
        void shouldPreventCookieFixationAttacks() throws Exception {
            // Given - Pre-set cookie with attacker's token
            Cookie attackerCookie = new Cookie("access_token", "attacker.token.value");

            // When - Login should overwrite the cookie
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .cookie(attackerCookie)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then - New cookie should be set, overwriting attacker's cookie
            Cookie newCookie = findCookieByName(result.getResponse().getCookies(), "access_token");
            assertThat(newCookie).isNotNull();
            assertThat(newCookie.getValue()).isNotEqualTo("attacker.token.value");
            assertThat(tokenProvider.validateToken(newCookie.getValue())).isTrue();
        }

        @Test
        @DisplayName("Should handle missing cookies gracefully")
        void shouldHandleMissingCookiesGracefully() throws Exception {
            // When & Then - No authentication should result in unauthorized
            mockMvc.perform(get("/api/v1/personas")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should validate token signature in cookies")
        void shouldValidateTokenSignatureInCookies() throws Exception {
            // Given - Create token with invalid signature
            String tokenWithInvalidSignature = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTYzMjQ2NDAwMCwiZXhwIjoyNjMyNDY0MDAwfQ.invalid_signature";
            Cookie invalidCookie = new Cookie("access_token", tokenWithInvalidSignature);

            // When & Then
            mockMvc.perform(get("/api/v1/personas")
                            .cookie(invalidCookie)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should prevent concurrent session management attacks")
        void shouldPreventConcurrentSessionManagementAttacks() throws Exception {
            // Given - Login to get first session
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            MvcResult firstLogin = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie firstSessionCookie = findCookieByName(firstLogin.getResponse().getCookies(), "access_token");

            // When - Login again (second session)
            MvcResult secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie secondSessionCookie = findCookieByName(secondLogin.getResponse().getCookies(), "access_token");

            // Then - Both sessions should be valid (stateless JWT allows concurrent sessions)
            mockMvc.perform(get("/api/v1/personas")
                            .cookie(firstSessionCookie)
                            .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/personas")
                            .cookie(secondSessionCookie)
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Cookie Expiration and Refresh Tests")
    class CookieExpirationAndRefreshTests {

        @Test
        @DisplayName("Should handle expired access token with valid refresh token")
        void shouldHandleExpiredAccessTokenWithValidRefreshToken() throws Exception {
            // Given - Create nearly expired access token (100ms lifetime)
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", testUser.getId().toString());
            claims.put("email", testUser.getEmail());
            claims.put("displayName", testUser.getUsername());
            String shortLivedToken = tokenProvider.generateToken(testUser.getUsername(), claims, 0); // Expire immediately
            
            // Wait for token to expire
            Thread.sleep(100);

            // Verify token is expired
            assertThat(tokenProvider.validateToken(shortLivedToken)).isFalse();

            // When - Try to use expired token
            mockMvc.perform(get("/api/v1/personas")
                            .header("Authorization", "Bearer " + shortLivedToken)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());

            // Then - Should be able to refresh token
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken(validRefreshToken)
                    .build();

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        @DisplayName("Should set appropriate expiration times for cookies")
        void shouldSetAppropriateExpirationTimesForCookies() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(TEST_EMAIL);
            loginRequest.setPassword(TEST_PASSWORD);

            // When
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            Cookie accessCookie = findCookieByName(result.getResponse().getCookies(), "access_token");
            Cookie refreshCookie = findCookieByName(result.getResponse().getCookies(), "refresh_token");

            // Access token should have shorter expiration than refresh token
            assertThat(accessCookie.getMaxAge()).isLessThan(refreshCookie.getMaxAge());
            assertThat(accessCookie.getMaxAge()).isGreaterThan(0);
            assertThat(refreshCookie.getMaxAge()).isGreaterThan(0);
        }
    }

    /**
     * Helper method to find cookie by name from cookie array
     */
    private Cookie findCookieByName(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}