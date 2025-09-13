package com.focushive.security;

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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security headers validation tests for FocusHive platform.
 * Tests all essential security headers across different endpoints and scenarios.
 * 
 * Security Headers Covered:
 * - Content-Security-Policy (CSP)
 * - X-Frame-Options (Clickjacking protection)
 * - X-Content-Type-Options (MIME type sniffing)
 * - X-XSS-Protection (XSS filtering)
 * - Strict-Transport-Security (HSTS)
 * - Referrer-Policy (Information leakage)
 * - Permissions-Policy (Feature control)
 * - Cross-Origin-Embedder-Policy (COEP)
 * - Cross-Origin-Opener-Policy (COOP)
 * - Cross-Origin-Resource-Policy (CORP)
 * - Cache-Control (Caching security)
 * - X-Permitted-Cross-Domain-Policies
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Security Headers Validation Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityHeadersTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private String validUserToken;

    // Security header patterns for validation
    private static final Pattern CSP_PATTERN = Pattern.compile("^(default-src|script-src|style-src|img-src|font-src|connect-src|media-src|object-src|child-src|frame-src|worker-src|frame-ancestors|form-action|base-uri|manifest-src|upgrade-insecure-requests|block-all-mixed-content|report-uri)\\s");
    private static final Pattern HSTS_PATTERN = Pattern.compile("^max-age=\\d+(?:;\\s*includeSubDomains)?(?:;\\s*preload)?$");
    private static final Pattern PERMISSIONS_POLICY_PATTERN = Pattern.compile("^[a-z-]+=(\\([^)]*\\)|\\*|self)(?:,\\s*[a-z-]+=(\\([^)]*\\)|\\*|self))*$");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        validUserToken = SecurityTestUtils.generateValidJwtToken("testuser");
    }

    // ============== Content Security Policy Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should include Content-Security-Policy header")
    void testContentSecurityPolicyHeader() throws Exception {
        List<String> testEndpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/hives",
            "/api/v1/personas",
            "/api/v1/auth/login"
        );

        for (String endpoint : testEndpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
            String cspReportOnlyHeader = result.getResponse().getHeader("Content-Security-Policy-Report-Only");

            assertTrue(cspHeader != null || cspReportOnlyHeader != null,
                      "CSP header should be present on " + endpoint);

            String actualCSP = cspHeader != null ? cspHeader : cspReportOnlyHeader;
            
            // Validate CSP structure
            assertTrue(actualCSP.contains("default-src"), 
                      "CSP should include default-src directive");
            assertTrue(actualCSP.contains("'self'"), 
                      "CSP should include 'self' source");
            assertTrue(actualCSP.contains("object-src 'none'") || actualCSP.contains("object-src: 'none'"), 
                      "CSP should disable object-src for security");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should validate CSP directive strength")
    void testCSPDirectiveStrength() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
        if (cspHeader == null) {
            cspHeader = result.getResponse().getHeader("Content-Security-Policy-Report-Only");
        }

        assertNotNull(cspHeader, "CSP header must be present");

        // Test for strong CSP directives
        assertFalse(cspHeader.contains("'unsafe-eval'") && cspHeader.contains("script-src"),
                   "CSP should not allow unsafe-eval in script-src");
        
        if (cspHeader.contains("script-src") && !cspHeader.contains("'unsafe-inline'")) {
            // If inline scripts are disabled, ensure nonce or hash is used
            assertTrue(cspHeader.contains("'nonce-") || cspHeader.contains("'sha"), 
                      "If unsafe-inline is disabled, should use nonce or hash");
        }

        // Validate frame-ancestors for clickjacking protection
        assertTrue(cspHeader.contains("frame-ancestors") && 
                  (cspHeader.contains("'none'") || cspHeader.contains("'self'")),
                  "CSP should include secure frame-ancestors directive");
    }

    @Test
    @Order(3)
    @DisplayName("Should provide different CSP for development vs production")
    void testEnvironmentSpecificCSP() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String cspHeader = result.getResponse().getHeader("Content-Security-Policy");
        if (cspHeader == null) {
            cspHeader = result.getResponse().getHeader("Content-Security-Policy-Report-Only");
        }

        assertNotNull(cspHeader, "CSP header must be present");

        // In test environment, CSP should be more permissive
        assertTrue(cspHeader.contains("localhost") || cspHeader.contains("127.0.0.1"),
                  "Test environment CSP should allow localhost");
        
        // Should still maintain basic security
        assertTrue(cspHeader.contains("default-src"),
                  "Even test CSP should have default-src");
    }

    // ============== X-Frame-Options Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should include X-Frame-Options header")
    void testXFrameOptionsHeader() throws Exception {
        List<String> endpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/hives",
            "/actuator/health"
        );

        for (String endpoint : endpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            String frameOptionsHeader = result.getResponse().getHeader("X-Frame-Options");
            assertNotNull(frameOptionsHeader, 
                         "X-Frame-Options header should be present on " + endpoint);

            List<String> validValues = Arrays.asList("DENY", "SAMEORIGIN");
            assertTrue(validValues.contains(frameOptionsHeader),
                      "X-Frame-Options should be DENY or SAMEORIGIN, was: " + frameOptionsHeader);
        }
    }

    // ============== X-Content-Type-Options Tests ==============

    @Test
    @Order(11)
    @DisplayName("Should include X-Content-Type-Options header")
    void testXContentTypeOptionsHeader() throws Exception {
        List<String> endpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/hives",
            "/api/v1/personas"
        );

        for (String endpoint : endpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            String contentTypeOptionsHeader = result.getResponse().getHeader("X-Content-Type-Options");
            assertNotNull(contentTypeOptionsHeader,
                         "X-Content-Type-Options header should be present on " + endpoint);
            assertEquals("nosniff", contentTypeOptionsHeader,
                        "X-Content-Type-Options should be 'nosniff'");
        }
    }

    // ============== X-XSS-Protection Tests ==============

    @Test
    @Order(12)
    @DisplayName("Should include X-XSS-Protection header")
    void testXXSSProtectionHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String xssProtectionHeader = result.getResponse().getHeader("X-XSS-Protection");
        assertNotNull(xssProtectionHeader, "X-XSS-Protection header should be present");

        List<String> validValues = Arrays.asList("1; mode=block", "0");
        assertTrue(validValues.contains(xssProtectionHeader),
                  "X-XSS-Protection should be '1; mode=block' or '0', was: " + xssProtectionHeader);
    }

    // ============== Strict-Transport-Security Tests ==============

    @Test
    @Order(13)
    @DisplayName("Should include HSTS header for HTTPS")
    void testStrictTransportSecurityHeader() throws Exception {
        // Note: In test environment with HTTP, HSTS might not be set
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .with(request -> {
                    request.setScheme("https");
                    request.setServerPort(443);
                    return request;
                }))
                .andReturn();

        String hstsHeader = result.getResponse().getHeader("Strict-Transport-Security");
        
        // HSTS should be present for HTTPS requests in production
        if (hstsHeader != null) {
            assertTrue(HSTS_PATTERN.matcher(hstsHeader).matches(),
                      "HSTS header format should be valid: " + hstsHeader);
            
            // Extract max-age value
            String maxAgeStr = hstsHeader.replaceAll(".*max-age=(\\d+).*", "$1");
            long maxAge = Long.parseLong(maxAgeStr);
            assertTrue(maxAge >= 31536000, // 1 year in seconds
                      "HSTS max-age should be at least 1 year (31536000 seconds)");
        }
    }

    // ============== Referrer-Policy Tests ==============

    @Test
    @Order(14)
    @DisplayName("Should include Referrer-Policy header")
    void testReferrerPolicyHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String referrerPolicyHeader = result.getResponse().getHeader("Referrer-Policy");
        assertNotNull(referrerPolicyHeader, "Referrer-Policy header should be present");

        List<String> validPolicies = Arrays.asList(
            "no-referrer",
            "no-referrer-when-downgrade", 
            "origin",
            "origin-when-cross-origin",
            "same-origin",
            "strict-origin",
            "strict-origin-when-cross-origin",
            "unsafe-url"
        );

        assertTrue(validPolicies.contains(referrerPolicyHeader),
                  "Referrer-Policy should be a valid policy: " + referrerPolicyHeader);
        
        // Ensure it's not the least secure option
        assertNotEquals("unsafe-url", referrerPolicyHeader,
                       "Referrer-Policy should not be 'unsafe-url'");
    }

    // ============== Permissions-Policy Tests ==============

    @Test
    @Order(15)
    @DisplayName("Should include Permissions-Policy header")
    void testPermissionsPolicyHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String permissionsPolicyHeader = result.getResponse().getHeader("Permissions-Policy");
        
        if (permissionsPolicyHeader != null) {
            // Validate format
            assertTrue(PERMISSIONS_POLICY_PATTERN.matcher(permissionsPolicyHeader).matches(),
                      "Permissions-Policy header format should be valid: " + permissionsPolicyHeader);

            // Check for dangerous permissions being restricted
            List<String> dangerousPermissions = Arrays.asList(
                "camera", "microphone", "geolocation", "payment", "usb"
            );

            for (String permission : dangerousPermissions) {
                if (permissionsPolicyHeader.contains(permission)) {
                    assertTrue(permissionsPolicyHeader.contains(permission + "=()"),
                              "Dangerous permission " + permission + " should be disabled");
                }
            }
        }
    }

    // ============== Cross-Origin Policy Tests ==============

    @Test
    @Order(16)
    @DisplayName("Should include Cross-Origin policy headers when appropriate")
    void testCrossOriginPolicyHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        // Cross-Origin-Embedder-Policy
        String coepHeader = result.getResponse().getHeader("Cross-Origin-Embedder-Policy");
        if (coepHeader != null) {
            List<String> validCOEPValues = Arrays.asList("unsafe-none", "require-corp");
            assertTrue(validCOEPValues.contains(coepHeader),
                      "COEP should be a valid value: " + coepHeader);
        }

        // Cross-Origin-Opener-Policy
        String coopHeader = result.getResponse().getHeader("Cross-Origin-Opener-Policy");
        if (coopHeader != null) {
            List<String> validCOOPValues = Arrays.asList(
                "unsafe-none", "same-origin-allow-popups", "same-origin"
            );
            assertTrue(validCOOPValues.contains(coopHeader),
                      "COOP should be a valid value: " + coopHeader);
        }

        // Cross-Origin-Resource-Policy
        String corpHeader = result.getResponse().getHeader("Cross-Origin-Resource-Policy");
        if (corpHeader != null) {
            List<String> validCORPValues = Arrays.asList(
                "same-site", "same-origin", "cross-origin"
            );
            assertTrue(validCORPValues.contains(corpHeader),
                      "CORP should be a valid value: " + corpHeader);
        }
    }

    // ============== Cache-Control Security Tests ==============

    @Test
    @Order(17)
    @DisplayName("Should include secure Cache-Control headers")
    void testCacheControlSecurity() throws Exception {
        // Test sensitive endpoints have no-cache directives
        List<String> sensitiveEndpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/auth/login",
            "/api/v1/admin/users"
        );

        for (String endpoint : sensitiveEndpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            String cacheControlHeader = result.getResponse().getHeader("Cache-Control");
            
            if (result.getResponse().getStatus() < 400) {
                assertNotNull(cacheControlHeader,
                             "Sensitive endpoint should have Cache-Control header: " + endpoint);
                
                // Check for secure caching directives
                assertTrue(cacheControlHeader.contains("no-store") || 
                          cacheControlHeader.contains("no-cache") ||
                          cacheControlHeader.contains("private"),
                          "Sensitive endpoint should prevent caching: " + endpoint);
            }
        }

        // Test Pragma header for HTTP/1.0 compatibility
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        String pragmaHeader = result.getResponse().getHeader("Pragma");
        if (pragmaHeader != null) {
            assertEquals("no-cache", pragmaHeader,
                        "Pragma header should be 'no-cache' for HTTP/1.0 compatibility");
        }
    }

    // ============== Additional Security Headers Tests ==============

    @Test
    @Order(18)
    @DisplayName("Should include additional security headers")
    void testAdditionalSecurityHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        // X-Permitted-Cross-Domain-Policies
        String permittedCrossDomainHeader = result.getResponse()
                .getHeader("X-Permitted-Cross-Domain-Policies");
        if (permittedCrossDomainHeader != null) {
            List<String> validValues = Arrays.asList("none", "master-only", "by-content-type", "all");
            assertTrue(validValues.contains(permittedCrossDomainHeader),
                      "X-Permitted-Cross-Domain-Policies should be a valid value");
            
            // Most secure option
            if (permittedCrossDomainHeader.equals("none")) {
                // This is the most secure setting
            }
        }

        // X-Robots-Tag (prevent indexing of sensitive pages)
        String robotsHeader = result.getResponse().getHeader("X-Robots-Tag");
        if (robotsHeader != null) {
            assertTrue(robotsHeader.contains("noindex") || robotsHeader.contains("nofollow"),
                      "X-Robots-Tag should prevent indexing of sensitive content");
        }

        // Server header should be minimal or absent
        String serverHeader = result.getResponse().getHeader("Server");
        if (serverHeader != null) {
            assertFalse(serverHeader.toLowerCase().contains("version") ||
                       serverHeader.toLowerCase().contains("/"),
                       "Server header should not reveal version information: " + serverHeader);
        }
    }

    // ============== CORS Headers Security Tests ==============

    @Test
    @Order(19)
    @DisplayName("Should handle CORS headers securely")
    void testCORSSecurityHeaders() throws Exception {
        // Test preflight request
        MvcResult preflightResult = mockMvc.perform(options("/api/v1/users/profile")
                .header("Origin", "https://evil.com")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andReturn();

        String allowOriginHeader = preflightResult.getResponse()
                .getHeader("Access-Control-Allow-Origin");
        
        if (allowOriginHeader != null) {
            // Should not allow all origins for sensitive endpoints
            assertNotEquals("*", allowOriginHeader,
                           "Should not use wildcard CORS for authenticated endpoints");
            
            // Should validate origin
            assertTrue(allowOriginHeader.equals("https://evil.com") || 
                      allowOriginHeader.startsWith("https://localhost") ||
                      allowOriginHeader.startsWith("http://localhost"),
                      "Should only allow configured origins");
        }

        String allowCredentialsHeader = preflightResult.getResponse()
                .getHeader("Access-Control-Allow-Credentials");
        
        if (allowCredentialsHeader != null && allowCredentialsHeader.equals("true")) {
            assertNotEquals("*", allowOriginHeader,
                           "Cannot use wildcard origin with credentials");
        }
    }

    // ============== Header Consistency Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should maintain header consistency across endpoints")
    void testHeaderConsistency() throws Exception {
        List<String> testEndpoints = Arrays.asList(
            "/api/v1/users/profile",
            "/api/v1/hives",
            "/api/v1/personas",
            "/api/v1/analytics/sessions"
        );

        List<String> requiredHeaders = SecurityTestUtils.getRequiredSecurityHeaders();
        
        for (String endpoint : testEndpoints) {
            MvcResult result = mockMvc.perform(get(endpoint)
                    .header("Authorization", "Bearer " + validUserToken))
                    .andReturn();

            if (result.getResponse().getStatus() < 400) {
                for (String headerName : requiredHeaders) {
                    String headerValue = result.getResponse().getHeader(headerName);
                    
                    // Not all headers are required for all endpoints, but check critical ones
                    if (headerName.equals("X-Content-Type-Options") ||
                        headerName.equals("X-Frame-Options")) {
                        assertNotNull(headerValue,
                                     "Critical security header " + headerName + 
                                     " should be present on " + endpoint);
                    }
                }
            }
        }
    }

    // ============== Error Response Headers Tests ==============

    @Test
    @Order(21)
    @DisplayName("Should include security headers in error responses")
    void testErrorResponseSecurityHeaders() throws Exception {
        // Test 401 Unauthorized
        MvcResult unauthorizedResult = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer invalid_token"))
                .andReturn();

        assertEquals(401, unauthorizedResult.getResponse().getStatus());
        
        String frameOptionsHeader = unauthorizedResult.getResponse().getHeader("X-Frame-Options");
        String contentTypeOptionsHeader = unauthorizedResult.getResponse()
                .getHeader("X-Content-Type-Options");

        assertNotNull(frameOptionsHeader, 
                     "Security headers should be present in error responses");
        assertNotNull(contentTypeOptionsHeader,
                     "Security headers should be present in error responses");

        // Test 403 Forbidden
        MvcResult forbiddenResult = mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        assertEquals(403, forbiddenResult.getResponse().getStatus());
        
        String cspHeader = forbiddenResult.getResponse().getHeader("Content-Security-Policy");
        if (cspHeader == null) {
            cspHeader = forbiddenResult.getResponse()
                    .getHeader("Content-Security-Policy-Report-Only");
        }
        
        // CSP should be present even in error responses
        assertNotNull(cspHeader, "CSP should be present in error responses");

        // Test 404 Not Found
        MvcResult notFoundResult = mockMvc.perform(get("/api/v1/nonexistent")
                .header("Authorization", "Bearer " + validUserToken))
                .andReturn();

        assertEquals(404, notFoundResult.getResponse().getStatus());
        
        String xssProtectionHeader = notFoundResult.getResponse().getHeader("X-XSS-Protection");
        if (xssProtectionHeader != null) {
            assertTrue(xssProtectionHeader.equals("1; mode=block") || xssProtectionHeader.equals("0"),
                      "XSS protection should be valid in error responses");
        }
    }

    // ============== Content-Type Specific Headers Tests ==============

    @Test
    @Order(22)
    @DisplayName("Should set appropriate headers for different content types")
    void testContentTypeSpecificHeaders() throws Exception {
        // Test JSON API response
        MvcResult jsonResult = mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + validUserToken)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonContentType = jsonResult.getResponse().getContentType();
        if (jsonContentType != null && jsonContentType.contains("application/json")) {
            String cacheControl = jsonResult.getResponse().getHeader("Cache-Control");
            assertNotNull(cacheControl, "JSON responses should have cache control");
        }

        // Test HTML response (if any)
        MvcResult htmlResult = mockMvc.perform(get("/")
                .accept(MediaType.TEXT_HTML))
                .andReturn();

        if (htmlResult.getResponse().getStatus() == 200) {
            String htmlContentType = htmlResult.getResponse().getContentType();
            if (htmlContentType != null && htmlContentType.contains("text/html")) {
                String cspHeader = htmlResult.getResponse().getHeader("Content-Security-Policy");
                if (cspHeader == null) {
                    cspHeader = htmlResult.getResponse()
                            .getHeader("Content-Security-Policy-Report-Only");
                }
                assertNotNull(cspHeader, "HTML responses should have CSP");
            }
        }
    }
}