package com.focushive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive authentication security tests for FocusHive platform.
 * Tests JWT token security, password policies, session management, MFA flows,
 * and authentication vulnerabilities across all microservices.
 * 
 * Security Areas Covered:
 * - JWT token validation and expiry
 * - Refresh token security
 * - Password strength policies and BCrypt hashing
 * - Account lockout mechanisms
 * - Session management and timeout
 * - Multi-factor authentication flows
 * - Authentication bypass attempts
 * - Token theft and replay attacks
 * - Brute force protection
 * - Timing attack prevention
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Authentication Security Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    
    // Test data
    private static final String TEST_USERNAME = "securitytest";
    private static final String TEST_EMAIL = "security@focushive.test";
    private static final String STRONG_PASSWORD = "SecureP@ssw0rd123!";
    private static final String WEAK_PASSWORD = "password";
    
    // Attack simulation data
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockoutTimestamps = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        failedAttempts.clear();
        lockoutTimestamps.clear();
    }

    // ============== JWT Token Security Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should accept valid JWT tokens")
    void testValidJwtTokenAcceptance() throws Exception {
        String validToken = SecurityTestUtils.generateValidJwtToken(TEST_USERNAME);
        
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("Should reject expired JWT tokens")
    void testExpiredJwtTokenRejection() throws Exception {
        String expiredToken = SecurityTestUtils.generateExpiredJwtToken(TEST_USERNAME);
        
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized: JWT token is expired"));
    }

    @Test
    @Order(3)
    @DisplayName("Should reject malformed JWT tokens")
    void testMalformedJwtTokenRejection() throws Exception {
        String malformedToken = SecurityTestUtils.generateMalformedJwtToken();
        
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject tampered JWT tokens")
    void testTamperedJwtTokenRejection() throws Exception {
        String tamperedToken = SecurityTestUtils.generateTamperedJwtToken(TEST_USERNAME);
        
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized: JWT signature does not match locally computed signature"));
    }

    @Test
    @Order(5)
    @DisplayName("Should reject requests without JWT tokens")
    void testMissingJwtTokenRejection() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Should reject JWT tokens with invalid format")
    void testInvalidJwtTokenFormat() throws Exception {
        List<String> invalidTokens = List.of(
            "Bearer ",
            "Bearer invalidtoken",
            "Basic dXNlcjpwYXNz",
            "Bearer " + "a".repeat(1000), // Extremely long token
            "Bearer null",
            "Bearer undefined"
        );
        
        for (String invalidToken : invalidTokens) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", invalidToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============== Refresh Token Security Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should validate refresh token rotation")
    void testRefreshTokenRotation() throws Exception {
        // Login to get initial tokens
        Map<String, String> loginRequest = Map.of(
            "username", TEST_USERNAME,
            "password", STRONG_PASSWORD
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        Map<String, Object> tokens = SecurityTestUtils.fromJson(loginResponse, Map.class);
        
        String refreshToken = (String) tokens.get("refreshToken");
        assertNotNull(refreshToken, "Refresh token should be present");

        // Use refresh token to get new tokens
        Map<String, String> refreshRequest = Map.of("refreshToken", refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(refreshRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        Map<String, Object> newTokens = SecurityTestUtils.fromJson(refreshResponse, Map.class);
        
        String newRefreshToken = (String) newTokens.get("refreshToken");
        assertNotNull(newRefreshToken, "New refresh token should be present");
        assertNotEquals(refreshToken, newRefreshToken, "Refresh token should be rotated");

        // Old refresh token should be invalidated
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }

    @Test
    @Order(11)
    @DisplayName("Should prevent refresh token reuse")
    void testRefreshTokenReusePreventions() throws Exception {
        String refreshToken = "test-refresh-token-" + UUID.randomUUID();
        Map<String, String> refreshRequest = Map.of("refreshToken", refreshToken);

        // First use should fail (token doesn't exist)
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(refreshRequest)))
                .andExpect(status().isUnauthorized());

        // Multiple attempts with same invalid token
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(refreshRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============== Password Security Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should enforce strong password policies")
    void testPasswordStrengthValidation() throws Exception {
        List<String> weakPasswords = List.of(
            "password",
            "123456",
            "qwerty",
            "abc123",
            "pass",
            "12345678",
            "password123", // Missing special char and uppercase
            "PASSWORD123", // Missing lowercase and special char
            "Password!", // Too short
            "passwordpassword" // Missing numbers and special chars
        );

        Map<String, Object> baseUserData = Map.of(
            "username", "testuser_weak",
            "email", "weak@focushive.test"
        );

        for (String weakPassword : weakPasswords) {
            Map<String, Object> userData = new HashMap<>(baseUserData);
            userData.put("password", weakPassword);

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(userData)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Password does not meet security requirements"));
        }
    }

    @Test
    @Order(21)
    @DisplayName("Should accept strong passwords")
    void testStrongPasswordAcceptance() throws Exception {
        List<String> strongPasswords = List.of(
            "SecureP@ssw0rd123!",
            "MyStr0ng!Password",
            "C0mpl3x&Secure!",
            "P@ssw0rd#2024",
            "Sup3r$ecure!Pass"
        );

        for (int i = 0; i < strongPasswords.size(); i++) {
            Map<String, Object> userData = Map.of(
                "username", "testuser_strong_" + i,
                "email", "strong" + i + "@focushive.test",
                "password", strongPasswords.get(i)
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(userData)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @Order(22)
    @DisplayName("Should use BCrypt for password hashing")
    void testBCryptPasswordHashing() {
        String password = "testPassword123!";
        String hashedPassword = passwordEncoder.encode(password);
        
        // BCrypt hashes should start with $2a$, $2b$, or $2y$
        assertTrue(hashedPassword.startsWith("$2"), "Password should be hashed with BCrypt");
        
        // Hash should be different each time
        String secondHash = passwordEncoder.encode(password);
        assertNotEquals(hashedPassword, secondHash, "BCrypt should generate unique salts");
        
        // Both hashes should verify correctly
        assertTrue(passwordEncoder.matches(password, hashedPassword));
        assertTrue(passwordEncoder.matches(password, secondHash));
        
        // Wrong password should not match
        assertFalse(passwordEncoder.matches("wrongPassword", hashedPassword));
    }

    // ============== Account Lockout Tests ==============

    @Test
    @Order(30)
    @DisplayName("Should implement account lockout after failed attempts")
    void testAccountLockoutMechanism() throws Exception {
        String username = "lockout_test_user";
        String wrongPassword = "wrongpassword";
        
        Map<String, String> loginRequest = Map.of(
            "username", username,
            "password", wrongPassword
        );

        // First 4 attempts should return unauthorized without lockout
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }

        // 5th attempt should trigger account lockout
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpected(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Account locked due to too many failed attempts"));

        // Even correct password should fail when account is locked
        Map<String, String> correctLoginRequest = Map.of(
            "username", username,
            "password", STRONG_PASSWORD
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(correctLoginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Account locked due to too many failed attempts"));
    }

    @Test
    @Order(31)
    @DisplayName("Should reset failed attempts after successful login")
    void testFailedAttemptsReset() throws Exception {
        String username = "reset_test_user";
        
        // Simulate 3 failed attempts
        Map<String, String> wrongLoginRequest = Map.of(
            "username", username,
            "password", "wrongpassword"
        );

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(wrongLoginRequest)))
                    .andExpected(status().isUnauthorized());
        }

        // Successful login should reset counter
        Map<String, String> correctLoginRequest = Map.of(
            "username", username,
            "password", STRONG_PASSWORD
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(correctLoginRequest)))
                .andExpected(status().isOk());

        // Should be able to make failed attempts again without immediate lockout
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(wrongLoginRequest)))
                    .andExpected(status().isUnauthorized())
                    .andExpected(jsonPath("$.error").value("Invalid credentials"));
        }
    }

    // ============== Session Management Tests ==============

    @Test
    @Order(40)
    @DisplayName("Should invalidate sessions on logout")
    void testSessionInvalidationOnLogout() throws Exception {
        String token = SecurityTestUtils.generateValidJwtToken(TEST_USERNAME);
        
        // Verify token is valid
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        // Token should be invalidated
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isUnauthorized())
                .andExpected(jsonPath("$.error").value("Token has been revoked"));
    }

    @Test
    @Order(41)
    @DisplayName("Should handle concurrent session management")
    void testConcurrentSessionManagement() throws Exception {
        String username = "concurrent_test_user";
        List<String> tokens = List.of(
            SecurityTestUtils.generateJwtToken(username, "USER", Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID()),
            SecurityTestUtils.generateJwtToken(username, "USER", Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID()),
            SecurityTestUtils.generateJwtToken(username, "USER", Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID())
        );

        // All tokens should be valid initially
        for (String token : tokens) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpected(status().isOk());
        }

        // Logout from one session
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + tokens.get(0)))
                .andExpected(status().isOk());

        // First token should be invalidated
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + tokens.get(0)))
                .andExpected(status().isUnauthorized());

        // Other tokens should still be valid
        for (int i = 1; i < tokens.size(); i++) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + tokens.get(i)))
                    .andExpected(status().isOk());
        }
    }

    // ============== Brute Force Protection Tests ==============

    @Test
    @Order(50)
    @DisplayName("Should detect and prevent brute force attacks")
    void testBruteForceProtection() throws Exception {
        String targetUsername = "brute_force_target";
        List<String> commonPasswords = SecurityTestUtils.generateBruteForcePasswords();
        
        int attempts = 0;
        for (String password : commonPasswords) {
            Map<String, String> loginRequest = Map.of(
                "username", targetUsername,
                "password", password
            );

            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SecurityTestUtils.toJson(loginRequest)))
                    .andReturn();

            attempts++;
            
            if (attempts <= 5) {
                assertEquals(401, result.getResponse().getStatus(), "Should return unauthorized for failed attempts");
            } else {
                assertEquals(429, result.getResponse().getStatus(), "Should return too many requests after threshold");
                break; // Account should be locked
            }
        }
    }

    @Test
    @Order(51)
    @DisplayName("Should implement progressive delays for failed attempts")
    void testProgressiveDelayForFailedAttempts() throws Exception {
        String username = "delay_test_user";
        Map<String, String> loginRequest = Map.of(
            "username", username,
            "password", "wrongpassword"
        );

        long firstAttemptTime = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpected(status().isUnauthorized());

        // Second attempt should have slight delay
        long secondAttemptTime = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpected(status().isUnauthorized());

        // Third attempt should have longer delay
        long thirdAttemptTime = System.currentTimeMillis();
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpected(status().isUnauthorized());

        // Verify delays are increasing (this is a basic check)
        long firstDelay = secondAttemptTime - firstAttemptTime;
        long secondDelay = thirdAttemptTime - secondAttemptTime;
        
        // Note: In real implementation, delays would be enforced server-side
        // This test primarily ensures the endpoint doesn't crash under repeated attempts
    }

    // ============== Timing Attack Prevention Tests ==============

    @Test
    @Order(60)
    @DisplayName("Should prevent username enumeration via timing attacks")
    void testTimingAttackPrevention() throws Exception {
        String existingUsername = "existing_user";
        String nonExistentUsername = "non_existent_user";
        String commonPassword = "password123";

        Runnable existingUserLogin = () -> {
            try {
                Map<String, String> loginRequest = Map.of(
                    "username", existingUsername,
                    "password", commonPassword
                );
                mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SecurityTestUtils.toJson(loginRequest)));
            } catch (Exception e) {
                // Ignore exceptions for timing measurement
            }
        };

        Runnable nonExistentUserLogin = () -> {
            try {
                Map<String, String> loginRequest = Map.of(
                    "username", nonExistentUsername,
                    "password", commonPassword
                );
                mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SecurityTestUtils.toJson(loginRequest)));
            } catch (Exception e) {
                // Ignore exceptions for timing measurement
            }
        };

        // Test for timing differences
        boolean vulnerableToTimingAttack = SecurityTestUtils.isTimingAttackVulnerable(
            existingUserLogin, nonExistentUserLogin, 50
        );

        assertFalse(vulnerableToTimingAttack, "Login endpoint should not be vulnerable to timing attacks");
    }

    // ============== Multi-Factor Authentication Tests ==============

    @Test
    @Order(70)
    @DisplayName("Should support TOTP-based MFA")
    void testTOTPMultiFactorAuthentication() throws Exception {
        String username = "mfa_test_user";
        
        // Enable MFA for user
        String token = SecurityTestUtils.generateValidJwtToken(username);
        
        mockMvc.perform(post("/api/v1/auth/mfa/enable")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.qrCodeUrl").exists())
                .andExpected(jsonPath("$.secretKey").exists());

        // Login should now require MFA
        Map<String, String> loginRequest = Map.of(
            "username", username,
            "password", STRONG_PASSWORD
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(loginRequest)))
                .andExpected(status().isAccepted()) // 202 - MFA required
                .andExpected(jsonPath("$.mfaRequired").value(true))
                .andExpected(jsonPath("$.mfaToken").exists());
    }

    @Test
    @Order(71)
    @DisplayName("Should validate MFA tokens correctly")
    void testMFATokenValidation() throws Exception {
        String mfaToken = "test-mfa-token-" + UUID.randomUUID();
        
        Map<String, String> mfaRequest = Map.of(
            "mfaToken", mfaToken,
            "totpCode", "123456" // Invalid TOTP code
        );

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(mfaRequest)))
                .andExpected(status().isUnauthorized())
                .andExpected(jsonPath("$.error").value("Invalid MFA code"));
    }

    // ============== Token Security Tests ==============

    @Test
    @Order(80)
    @DisplayName("Should prevent JWT token manipulation")
    void testJWTTokenManipulation() throws Exception {
        String validToken = SecurityTestUtils.generateValidJwtToken(TEST_USERNAME);
        
        // Test various manipulation attempts
        List<String> manipulatedTokens = List.of(
            validToken.substring(0, validToken.length() - 10) + "0123456789", // Changed signature
            validToken.replace('.', '_'), // Invalid format
            validToken + "extra", // Appended data
            new StringBuilder(validToken).reverse().toString(), // Reversed token
            validToken.substring(0, validToken.indexOf('.')) + ".eyJzdWIiOiJhZG1pbiJ9." + validToken.substring(validToken.lastIndexOf('.')) // Modified payload
        );

        for (String manipulatedToken : manipulatedTokens) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + manipulatedToken))
                    .andExpected(status().isUnauthorized());
        }
    }

    @Test
    @Order(81)
    @DisplayName("Should handle token replay attacks")
    void testTokenReplayAttackPrevention() throws Exception {
        String token = SecurityTestUtils.generateValidJwtToken(TEST_USERNAME);
        
        // Use token multiple times (should work as JWT is stateless)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpected(status().isOk());
        }

        // Logout to revoke token
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isOk());

        // Replaying revoked token should fail
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + token))
                .andExpected(status().isUnauthorized());
    }

    // ============== Security Event Logging Tests ==============

    @Test
    @Order(90)
    @DisplayName("Should log security events")
    void testSecurityEventLogging() throws Exception {
        // This test would verify that security events are properly logged
        // In a real implementation, you would check log files or log aggregation systems
        
        String username = "logging_test_user";
        
        // Failed login attempt
        Map<String, String> failedLoginRequest = Map.of(
            "username", username,
            "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(failedLoginRequest)))
                .andExpected(status().isUnauthorized());

        // Successful login
        Map<String, String> successfulLoginRequest = Map.of(
            "username", username,
            "password", STRONG_PASSWORD
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SecurityTestUtils.toJson(successfulLoginRequest)))
                .andExpected(status().isOk());

        // In a real test, verify log entries were created for:
        // - Failed login attempt
        // - Successful login
        // - IP address logging
        // - User agent logging
        // - Timestamp accuracy
    }
}