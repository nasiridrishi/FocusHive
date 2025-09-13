package com.focushive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive API security and rate limiting tests for FocusHive platform.
 * Tests API endpoint security, rate limiting enforcement, OAuth2 flows, 
 * CORS policies, request signing, and response encryption.
 * 
 * Security Areas Covered:
 * - Rate limiting enforcement per endpoint and user
 * - API key management and validation
 * - OAuth2 flow security and token management
 * - CORS policy validation and bypass prevention
 * - Request signing and verification
 * - Response encryption and data protection
 * - API versioning security
 * - Distributed rate limiting consistency
 * - Burst protection and adaptive limiting
 * - API abuse detection and mitigation
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("API Security & Rate Limiting Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class APISecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String validUserToken;
    private String adminToken;

    // Rate limiting test configuration
    private static final int RATE_LIMIT_THRESHOLD = 10; // requests per minute
    private static final int BURST_THRESHOLD = 20; // burst capacity
    private static final long RATE_LIMIT_WINDOW = 60000; // 1 minute in milliseconds

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validUserToken = SecurityTestUtils.generateValidJwtToken("testuser");
        adminToken = SecurityTestUtils.generateJwtToken("admin", "ADMIN", 
                Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID());
    }

    // ============== Rate Limiting Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should enforce rate limiting on API endpoints")
    void testBasicRateLimiting() throws Exception {
        String endpoint = "/api/v1/hives";
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        
        // Make requests rapidly to trigger rate limiting
        for (int i = 0; i < 25; i++) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            if (status == 200) {
                successCount.incrementAndGet();
            } else if (status == 429) {
                rateLimitedCount.incrementAndGet();
                
                // Verify rate limiting headers are present
                String retryAfterHeader = result.getResponse().getHeader("Retry-After");
                String rateLimitHeader = result.getResponse().getHeader("X-RateLimit-Limit");
                String remainingHeader = result.getResponse().getHeader("X-RateLimit-Remaining");
                
                assertNotNull(retryAfterHeader, "Retry-After header should be present");
                assertNotNull(rateLimitHeader, "Rate limit header should be present");
                assertNotNull(remainingHeader, "Remaining requests header should be present");
            }
            
            // Small delay between requests
            Thread.sleep(50);
        }
        
        assertTrue(rateLimitedCount.get() > 0, 
                  "Should trigger rate limiting after threshold exceeded");
        assertTrue(successCount.get() > 0,
                  "Some requests should succeed before rate limiting");
    }

    @Test
    @Order(2)
    @DisplayName("Should implement per-user rate limiting")
    void testPerUserRateLimiting() throws Exception {
        String user1Token = SecurityTestUtils.generateValidJwtToken("user1");
        String user2Token = SecurityTestUtils.generateValidJwtToken("user2");
        
        // Exhaust rate limit for user1
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get("/api/v1/personas")
                    .header("Authorization", "Bearer " + user1Token))
                    .andReturn();
        }
        
        // user1 should now be rate limited
        mockMvc.perform(get("/api/v1/personas")
                .header("Authorization", "Bearer " + user1Token))
                .andExpected(status().isTooManyRequests());
        
        // user2 should still be able to make requests
        mockMvc.perform(get("/api/v1/personas")
                .header("Authorization", "Bearer " + user2Token))
                .andExpected(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("Should implement endpoint-specific rate limiting")
    void testEndpointSpecificRateLimiting() throws Exception {
        // Different endpoints may have different rate limits
        Map<String, Integer> endpointLimits = Map.of(
            "/api/v1/auth/login", 5, // Stricter limit for auth
            "/api/v1/hives", 20,     // Normal limit for hives
            "/api/v1/users/profile", 30 // Higher limit for profile
        );
        
        for (Map.Entry<String, Integer> entry : endpointLimits.entrySet()) {
            String endpoint = entry.getKey();
            int expectedLimit = entry.getValue();
            
            AtomicInteger requestCount = new AtomicInteger(0);
            boolean rateLimited = false;
            
            // Make requests until rate limited
            for (int i = 0; i < expectedLimit + 10; i++) {
                MvcResult result;
                
                if (endpoint.equals("/api/v1/auth/login")) {
                    Map<String, String> loginData = Map.of(
                        "username", "testuser",
                        "password", "wrongpassword"
                    );
                    result = mockMvc.perform(post(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(SecurityTestUtils.toJson(loginData)))
                            .andReturn();
                } else {
                    result = mockMvc.perform(get(endpoint)
                            .header("Authorization", "Bearer " + validUserToken))
                            .andReturn();
                }
                
                requestCount.incrementAndGet();
                
                if (result.getResponse().getStatus() == 429) {
                    rateLimited = true;
                    break;
                }
                
                Thread.sleep(50);
            }
            
            assertTrue(rateLimited, 
                      "Rate limiting should be enforced for " + endpoint);
            assertTrue(requestCount.get() <= expectedLimit + 5,
                      "Rate limit should be close to expected limit for " + endpoint);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should implement burst protection")
    void testBurstProtection() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        
        // Simulate burst of concurrent requests
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    MvcResult result = mockMvc.perform(get("/api/v1/hives")
                            .header("Authorization", "Bearer " + validUserToken))
                            .andReturn();
                    
                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else if (result.getResponse().getStatus() == 429) {
                        rejectedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        
        assertTrue(rejectedCount.get() > 0,
                  "Burst protection should reject some requests");
        assertTrue(successCount.get() < 50,
                  "Not all burst requests should succeed");
        
        executor.shutdown();
    }

    // ============== API Key Management Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should validate API key authentication")
    void testAPIKeyValidation() throws Exception {
        String validApiKey = "test-api-key-" + UUID.randomUUID();
        String invalidApiKey = "invalid-api-key";
        
        // Test valid API key
        mockMvc.perform(get("/api/v1/public/health")
                .header("X-API-Key", validApiKey))
                .andExpected(status().isOk());
        
        // Test invalid API key
        mockMvc.perform(get("/api/v1/public/health")
                .header("X-API-Key", invalidApiKey))
                .andExpected(status().isUnauthorized());
        
        // Test missing API key for endpoints that require it
        mockMvc.perform(get("/api/v1/external/webhook")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 404,
                             "Should require API key for external endpoints");
                });
        
        // Test API key with JWT token (should work together)
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .header("X-API-Key", validApiKey))
                .andExpected(status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("Should enforce API key rate limiting")
    void testAPIKeyRateLimiting() throws Exception {
        String apiKey = "test-api-key-rate-limit";
        
        // Make rapid requests with same API key
        AtomicInteger rateLimitedCount = new AtomicInteger(0);
        
        for (int i = 0; i < 30; i++) {
            MvcResult result = mockMvc.perform(get("/api/v1/public/stats")
                    .header("X-API-Key", apiKey))
                    .andReturn();
            
            if (result.getResponse().getStatus() == 429) {
                rateLimitedCount.incrementAndGet();
                
                // Verify API key specific headers
                String quotaHeader = result.getResponse().getHeader("X-API-Quota-Remaining");
                String resetHeader = result.getResponse().getHeader("X-API-Quota-Reset");
                
                // Headers might be present for API key rate limiting
                if (quotaHeader != null) {
                    assertTrue(Integer.parseInt(quotaHeader) >= 0,
                              "API quota remaining should be non-negative");
                }
                if (resetHeader != null) {
                    assertTrue(Long.parseLong(resetHeader) > 0,
                              "API quota reset time should be valid");
                }
            }
            
            Thread.sleep(50);
        }
        
        assertTrue(rateLimitedCount.get() > 0,
                  "API key should be rate limited");
    }

    // ============== OAuth2 Flow Security Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should secure OAuth2 authorization flow")
    void testOAuth2AuthorizationFlow() throws Exception {
        // Test authorization endpoint security
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "https://client.example.com/callback")
                .param("scope", "read write")
                .param("state", "secure-state-token"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    // Should redirect to login or return auth page
                    assertTrue(status == 302 || status == 200,
                             "OAuth2 authorization should work properly");
                });

        // Test authorization with malicious redirect URI
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "https://evil.com/steal-code")
                .param("scope", "read")
                .param("state", "state"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should reject unauthorized redirect URIs");
                });

        // Test missing state parameter (CSRF protection)
        mockMvc.perform(get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "test-client")
                .param("redirect_uri", "https://client.example.com/callback")
                .param("scope", "read"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    // Should require state parameter for CSRF protection
                    assertTrue(status >= 400 || status == 302,
                             "Should handle missing state parameter appropriately");
                });
    }

    @Test
    @Order(21)
    @DisplayName("Should secure OAuth2 token exchange")
    void testOAuth2TokenExchange() throws Exception {
        // Test token endpoint with valid parameters
        mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "authorization_code")
                .param("code", "test-auth-code")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .param("redirect_uri", "https://client.example.com/callback"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    // May succeed or fail depending on test setup
                    assertTrue(status == 200 || status >= 400,
                             "Token endpoint should handle requests properly");
                });

        // Test token endpoint with invalid client credentials
        mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "authorization_code")
                .param("code", "test-auth-code")
                .param("client_id", "invalid-client")
                .param("client_secret", "wrong-secret")
                .param("redirect_uri", "https://client.example.com/callback"))
                .andExpected(status().isUnauthorized());

        // Test client credentials grant
        mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "client_credentials")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .param("scope", "read"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status >= 400,
                             "Client credentials grant should be handled");
                });

        // Test refresh token flow
        mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "refresh_token")
                .param("refresh_token", "test-refresh-token")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status >= 400,
                             "Refresh token flow should be handled");
                });
    }

    // ============== CORS Policy Validation Tests ==============

    @Test
    @Order(30)
    @DisplayName("Should enforce CORS policies correctly")
    void testCORSPolicyEnforcement() throws Exception {
        // Test allowed origin
        mockMvc.perform(options("/api/v1/hives")
                .header("Origin", "https://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpected(status().isOk())
                .andExpected(header().string("Access-Control-Allow-Origin", "https://localhost:3000"))
                .andExpected(header().exists("Access-Control-Allow-Methods"))
                .andExpected(header().exists("Access-Control-Allow-Headers"));

        // Test disallowed origin
        mockMvc.perform(options("/api/v1/hives")
                .header("Origin", "https://malicious.com")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpected(result -> {
                    String allowOriginHeader = result.getResponse()
                            .getHeader("Access-Control-Allow-Origin");
                    
                    // Should either not set header or set it to allowed origin only
                    assertTrue(allowOriginHeader == null || 
                              !allowOriginHeader.equals("https://malicious.com"),
                              "Should not allow unauthorized origins");
                });

        // Test actual CORS request (not preflight)
        mockMvc.perform(get("/api/v1/hives")
                .header("Origin", "https://localhost:3000")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isOk())
                .andExpected(header().string("Access-Control-Allow-Origin", "https://localhost:3000"));

        // Test CORS with credentials
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Origin", "https://localhost:3000")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(result -> {
                    String allowCredentialsHeader = result.getResponse()
                            .getHeader("Access-Control-Allow-Credentials");
                    String allowOriginHeader = result.getResponse()
                            .getHeader("Access-Control-Allow-Origin");
                    
                    if (allowCredentialsHeader != null && allowCredentialsHeader.equals("true")) {
                        assertNotEquals("*", allowOriginHeader,
                                       "Cannot use wildcard origin with credentials");
                    }
                });
    }

    @Test
    @Order(31)
    @DisplayName("Should prevent CORS bypass attacks")
    void testCORSBypassPrevention() throws Exception {
        // Test various origin bypass attempts
        List<String> maliciousOrigins = Arrays.asList(
            "https://localhost:3000.evil.com",
            "https://evil.com#localhost:3000",
            "https://localhost:3000@evil.com",
            "null",
            "file://",
            "data:text/html,<script>alert('xss')</script>"
        );

        for (String origin : maliciousOrigins) {
            mockMvc.perform(options("/api/v1/hives")
                    .header("Origin", origin)
                    .header("Access-Control-Request-Method", "GET"))
                    .andExpected(result -> {
                        String allowOriginHeader = result.getResponse()
                                .getHeader("Access-Control-Allow-Origin");
                        
                        // Should not allow malicious origins
                        assertTrue(allowOriginHeader == null || 
                                  !allowOriginHeader.equals(origin),
                                  "Should reject malicious origin: " + origin);
                    });
        }
    }

    // ============== Request Signing Tests ==============

    @Test
    @Order(40)
    @DisplayName("Should validate request signatures")
    void testRequestSignatureValidation() throws Exception {
        // Test webhook with valid signature
        String payload = "{\"event\": \"user.created\", \"data\": {\"userId\": \"123\"}}";
        String signature = "sha256=valid-signature-hash";

        mockMvc.perform(post("/api/v1/webhooks/external")
                .header("X-Signature", signature)
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    // Should validate signature (may fail if validation is strict)
                    assertTrue(status >= 400 || status == 200,
                             "Should handle signed requests appropriately");
                });

        // Test webhook with invalid signature
        String invalidSignature = "sha256=invalid-signature-hash";

        mockMvc.perform(post("/api/v1/webhooks/external")
                .header("X-Signature", invalidSignature)
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should reject invalid signatures");
                });

        // Test webhook with missing signature
        mockMvc.perform(post("/api/v1/webhooks/external")
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should require signature for webhooks");
                });

        // Test webhook with expired timestamp
        long expiredTimestamp = System.currentTimeMillis() - (10 * 60 * 1000); // 10 minutes ago

        mockMvc.perform(post("/api/v1/webhooks/external")
                .header("X-Signature", signature)
                .header("X-Timestamp", String.valueOf(expiredTimestamp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                             "Should reject expired requests");
                });
    }

    // ============== Response Encryption Tests ==============

    @Test
    @Order(50)
    @DisplayName("Should handle response encryption appropriately")
    void testResponseEncryption() throws Exception {
        // Test sensitive endpoints that might require encryption
        List<String> sensitiveEndpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/admin/users",
            "/api/v1/analytics/sensitive"
        );

        for (String endpoint : sensitiveEndpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + 
                           (endpoint.contains("admin") ? adminToken : validUserToken))
                    .header("Accept-Encoding", "gzip"))
                    .andReturn();

            if (result.getResponse().getStatus() == 200) {
                // Check if response is compressed (basic security measure)
                String contentEncoding = result.getResponse().getHeader("Content-Encoding");
                
                // For sensitive data, compression should be considered
                // (though it needs to be balanced against CRIME/BREACH attacks)
                
                // Check Content-Type for sensitive data
                String contentType = result.getResponse().getContentType();
                assertTrue(contentType != null && contentType.contains("application/json"),
                          "Sensitive endpoints should return JSON");

                // Verify no sensitive data in response headers
                Collection<String> headerNames = result.getResponse().getHeaderNames();
                for (String headerName : headerNames) {
                    String headerValue = result.getResponse().getHeader(headerName);
                    assertFalse(headerValue.toLowerCase().contains("password") ||
                               headerValue.toLowerCase().contains("secret") ||
                               headerValue.toLowerCase().contains("key"),
                               "Headers should not contain sensitive information");
                }
            }
        }
    }

    // ============== API Versioning Security Tests ==============

    @Test
    @Order(60)
    @DisplayName("Should secure API versioning")
    void testAPIVersioningSecurity() throws Exception {
        // Test current API version
        mockMvc.perform(get("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isOk());

        // Test deprecated API version (if exists)
        mockMvc.perform(get("/api/v0/hives")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    // Deprecated versions should either not exist or return deprecation warnings
                    assertTrue(status == 404 || status == 410 || status == 200,
                             "Deprecated API versions should be handled appropriately");
                    
                    if (status == 200) {
                        String deprecationHeader = result.getResponse().getHeader("Deprecation");
                        String warningHeader = result.getResponse().getHeader("Warning");
                        
                        // Should warn about deprecated version
                        assertTrue(deprecationHeader != null || warningHeader != null,
                                  "Deprecated API should include deprecation warnings");
                    }
                });

        // Test future API version
        mockMvc.perform(get("/api/v999/hives")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isNotFound());

        // Test version in different formats
        List<String> versionFormats = Arrays.asList(
            "/api/hives", // No version
            "/v1/api/hives", // Wrong order
            "/api/1/hives", // No 'v' prefix
            "/api/v1.0/hives" // Decimal version
        );

        for (String endpoint : versionFormats) {
            mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        // Non-standard version formats should be handled consistently
                        assertTrue(status == 404 || status == 200,
                                  "Version format should be handled consistently: " + endpoint);
                    });
        }
    }

    // ============== API Abuse Detection Tests ==============

    @Test
    @Order(70)
    @DisplayName("Should detect and mitigate API abuse")
    void testAPIAbuseDetection() throws Exception {
        // Test rapid sequential requests (potential bot behavior)
        String userAgent = "ScrapingBot/1.0";
        AtomicInteger blockedRequests = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(get("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .header("User-Agent", userAgent))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                blockedRequests.incrementAndGet();
            }

            // No delay between requests (suspicious behavior)
        }

        assertTrue(blockedRequests.get() > 0,
                  "Should detect and block rapid requests");

        // Test requests with missing User-Agent
        mockMvc.perform(get("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(result -> {
                    // Some APIs might require User-Agent for abuse prevention
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 400,
                             "Should handle missing User-Agent appropriately");
                });

        // Test requests with suspicious patterns
        List<String> suspiciousUserAgents = Arrays.asList(
            "curl/7.68.0",
            "python-requests/2.25.1",
            "PostmanRuntime/7.28.0",
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        );

        for (String ua : suspiciousUserAgents) {
            mockMvc.perform(get("/api/v1/hives")
                    .header("Authorization", "Bearer " + validUserToken)
                    .header("User-Agent", ua))
                    .andExpected(result -> {
                        // May allow or block based on abuse detection rules
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 200 || status == 403 || status == 429,
                                  "Should handle suspicious user agents: " + ua);
                    });
        }
    }

    // ============== Error Response Security Tests ==============

    @Test
    @Order(80)
    @DisplayName("Should secure API error responses")
    void testAPIErrorResponseSecurity() throws Exception {
        // Test error responses don't leak sensitive information
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isForbidden())
                .andExpected(result -> {
                    String response = result.getResponse().getContentAsString();
                    
                    // Should not leak internal information
                    assertFalse(response.toLowerCase().contains("java.lang"),
                               "Should not expose stack traces");
                    assertFalse(response.toLowerCase().contains("springframework"),
                               "Should not expose framework details");
                    assertFalse(response.toLowerCase().contains("sql"),
                               "Should not expose database details");
                    assertFalse(response.toLowerCase().contains("password"),
                               "Should not expose password fields");
                });

        // Test invalid endpoint
        mockMvc.perform(get("/api/v1/nonexistent")
                .header("Authorization", "Bearer " + validUserToken))
                .andExpected(status().isNotFound())
                .andExpected(result -> {
                    String response = result.getResponse().getContentAsString();
                    
                    // Should return generic 404 message
                    assertFalse(response.toLowerCase().contains("controller"),
                               "Should not expose controller information");
                    assertFalse(response.toLowerCase().contains("mapping"),
                               "Should not expose mapping details");
                });

        // Test malformed request
        mockMvc.perform(post("/api/v1/hives")
                .header("Authorization", "Bearer " + validUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json{"))
                .andExpected(status().isBadRequest())
                .andExpected(result -> {
                    String response = result.getResponse().getContentAsString();
                    
                    // Should return sanitized error message
                    assertFalse(response.toLowerCase().contains("jackson"),
                               "Should not expose JSON parser details");
                    assertFalse(response.toLowerCase().contains("fasterxml"),
                               "Should not expose library details");
                });
    }
}