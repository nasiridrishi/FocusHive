package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for CSRF configuration.
 * Verifies that CSRF protection is properly disabled for API endpoints.
 *
 * Requirements:
 * - API endpoints should not require CSRF tokens (stateless JWT authentication)
 * - Authentication endpoints at /api/auth/** should work without CSRF
 * - OAuth2 endpoints should maintain their configured security
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesTestOnly",
    "jwt.use-rsa=false",
    "jwt.issuer=http://localhost:8081/identity",
    "jwt.access-token-expiration-ms=3600000",
    "jwt.refresh-token-expiration-ms=86400000",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
    "focushive.rate-limiting.enabled=false",
    "app.admin.auto-create=false",
    "app.admin.username=testadmin",
    "app.admin.password=testpass123",
    "app.admin.email=admin@test.com",
    "app.admin.first-name=Test",
    "app.admin.last-name=Admin",
    "app.encryption.master-key=testMasterKey12345678901234567890123456789012",
    "app.encryption.salt=testSalt1234567890123456789012345678901234567890",
    "auth.issuer=http://localhost:8081/auth",
    "notification.service.url=http://localhost:8083",
    "notification.service.auth.enabled=false",
    "security.cors.allowed-origins=http://localhost:3000,http://localhost:3001",
    "spring.flyway.enabled=false",
    "spring.cache.type=simple",
    "DB_HOST=localhost",
    "DB_PORT=5432",
    "DB_NAME=testdb",
    "DB_USER=sa",
    "DB_PASSWORD=",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=",
    "ADMIN_USERNAME=testadmin",
    "ADMIN_PASSWORD=testpass123",
    "ADMIN_EMAIL=admin@test.com",
    "ADMIN_FIRST_NAME=Test",
    "ADMIN_LAST_NAME=Admin",
    "ADMIN_AUTO_CREATE=false",
    "JWT_SECRET=testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesTestOnly",
    "JWT_ISSUER=http://localhost:8081/identity",
    "AUTH_ISSUER=http://localhost:8081/auth",
    "OAUTH2_ISSUER=http://localhost:8081/oauth2",
    "CORS_ORIGINS=http://localhost:3000,http://localhost:3001",
    "ENCRYPTION_MASTER_KEY=testMasterKey12345678901234567890123456789012",
    "ENCRYPTION_SALT=testSalt1234567890123456789012345678901234567890"
})
public class SecurityConfigCsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegistrationEndpointWithoutCsrf() throws Exception {
        // Given: A valid registration request
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("TestPassword123!");
        request.setConfirmPassword("TestPassword123!");
        request.setFirstName("Test");
        request.setLastName("User");

        // When: POST to /api/auth/register without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // We expect either 200/201 for success or 400/409 for validation errors
                // but NOT 403 which indicates CSRF protection is active
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/register";
                });
    }

    @Test
    public void testLoginEndpointWithoutCsrf() throws Exception {
        // Given: A login request
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("TestPassword123!");

        // When: POST to /api/auth/login without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/login";
                });
    }

    @Test
    public void testRefreshEndpointWithoutCsrf() throws Exception {
        // Given: A refresh token request
        String refreshTokenRequest = "{\"refreshToken\":\"test-refresh-token\"}";

        // When: POST to /api/auth/refresh without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/refresh";
                });
    }

    @Test
    public void testLogoutEndpointWithoutCsrf() throws Exception {
        // When: POST to /api/auth/logout without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/logout";
                });
    }

    @Test
    public void testForgotPasswordEndpointWithoutCsrf() throws Exception {
        // Given: A password reset request
        String passwordResetRequest = "{\"email\":\"test@example.com\"}";

        // When: POST to /api/auth/forgot-password without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordResetRequest))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/forgot-password";
                });
    }

    @Test
    public void testResetPasswordEndpointWithoutCsrf() throws Exception {
        // Given: A password reset confirmation request
        String resetPasswordRequest = "{\"token\":\"reset-token\",\"password\":\"NewPassword123!\",\"confirmPassword\":\"NewPassword123!\"}";

        // When: POST to /api/auth/reset-password without CSRF token
        // Then: Should not return 403 Forbidden (CSRF error)
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequest))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Assert that status is NOT 403 (CSRF protection would return 403)
                    assert status != 403 : "CSRF protection should be disabled for /api/auth/reset-password";
                });
    }
}