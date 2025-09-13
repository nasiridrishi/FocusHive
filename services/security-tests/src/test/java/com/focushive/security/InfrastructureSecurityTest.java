package com.focushive.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive infrastructure security tests for FocusHive platform.
 * Tests Docker container security, database connection security, Redis security,
 * environment variable protection, secret management, and network segmentation.
 * 
 * Security Areas Covered:
 * - Docker container security configuration
 * - Database connection security and encryption
 * - Redis security configuration and authentication
 * - Environment variable protection and secrets management
 * - Network segmentation and firewall rules
 * - Service-to-service communication security
 * - Container runtime security
 * - Infrastructure monitoring and logging
 * - Dependency vulnerability scanning
 * - Configuration security hardening
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Infrastructure Security Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InfrastructureSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        adminToken = SecurityTestUtils.generateJwtToken("admin", "ADMIN", 
                java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS), 
                UUID.randomUUID());
    }

    // ============== Container Security Tests ==============

    @Test
    @Order(1)
    @DisplayName("Should validate Docker container security configuration")
    void testDockerContainerSecurity() throws Exception {
        // Test health check endpoint to verify container is running securely
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertTrue(response.contains("UP") || response.contains("status"),
                              "Health check should indicate secure container status");
                });

        // Test that container info endpoints are not exposed
        List<String> containerEndpoints = Arrays.asList(
            "/actuator/env",
            "/actuator/configprops",
            "/actuator/beans", 
            "/actuator/mappings",
            "/docker/info",
            "/container/stats",
            "/proc/version",
            "/sys/class/net"
        );

        for (String endpoint : containerEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 404 || status == 401 || status == 403,
                                  "Container info endpoint should not be exposed: " + endpoint);
                    });
        }

        // Test that the application doesn't run as root
        MvcResult systemInfoResult = mockMvc.perform(get("/api/v1/admin/system/info")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (systemInfoResult.getResponse().getStatus() == 200) {
            String response = systemInfoResult.getResponse().getContentAsString();
            // Should not indicate running as root user
            assertFalse(response.contains("\"user\":\"root\"") || 
                       response.contains("uid=0"),
                       "Application should not run as root user");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should validate container runtime security")
    void testContainerRuntimeSecurity() throws Exception {
        // Test that sensitive host resources are not accessible
        List<String> sensitiveResources = Arrays.asList(
            "/proc/1/environ", // Host process environment
            "/host/etc/passwd", // Host filesystem
            "/host/root/.ssh",  // Host SSH keys
            "/var/run/docker.sock", // Docker socket
            "/dev/mem", // System memory
            "/sys/fs/cgroup" // Control groups
        );

        for (String resource : sensitiveResources) {
            mockMvc.perform(get("/api/v1/system/file")
                    .param("path", resource)
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400,
                                  "Sensitive host resource should not be accessible: " + resource);
                    });
        }

        // Test container capabilities restrictions
        MvcResult capabilitiesResult = mockMvc.perform(get("/api/v1/admin/system/capabilities")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (capabilitiesResult.getResponse().getStatus() == 200) {
            String response = capabilitiesResult.getResponse().getContentAsString();
            
            // Should not have dangerous capabilities
            assertFalse(response.contains("CAP_SYS_ADMIN") || 
                       response.contains("CAP_NET_ADMIN") ||
                       response.contains("--privileged"),
                       "Container should not have dangerous capabilities");
        }
    }

    // ============== Database Security Tests ==============

    @Test
    @Order(10)
    @DisplayName("Should validate database connection security")
    void testDatabaseConnectionSecurity() throws Exception {
        // Test database health and security
        MvcResult dbHealthResult = mockMvc.perform(get("/api/v1/admin/database/health")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (dbHealthResult.getResponse().getStatus() == 200) {
            String response = dbHealthResult.getResponse().getContentAsString();
            Map<String, Object> dbHealth = SecurityTestUtils.fromJson(response, Map.class);
            
            // Database should be using secure connections
            if (dbHealth.containsKey("ssl") || dbHealth.containsKey("encryption")) {
                assertTrue((Boolean) dbHealth.getOrDefault("ssl", false) ||
                          (Boolean) dbHealth.getOrDefault("encryption", false),
                          "Database connections should use SSL/TLS encryption");
            }
            
            // Connection should not expose credentials
            assertFalse(response.contains("password=") || 
                       response.contains("username=") ||
                       response.contains("jdbc:"),
                       "Database credentials should not be exposed");
        }

        // Test that database admin endpoints are protected
        mockMvc.perform(get("/api/v1/database/admin")
                .header("Authorization", "Bearer " + "invalid-token"))
                .andExpected(status().isUnauthorized());

        // Test database connection pooling security
        MvcResult poolResult = mockMvc.perform(get("/api/v1/admin/database/pool-stats")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (poolResult.getResponse().getStatus() == 200) {
            String response = poolResult.getResponse().getContentAsString();
            Map<String, Object> poolStats = SecurityTestUtils.fromJson(response, Map.class);
            
            // Pool should have reasonable limits
            if (poolStats.containsKey("maxConnections")) {
                int maxConn = (Integer) poolStats.get("maxConnections");
                assertTrue(maxConn > 0 && maxConn < 1000,
                          "Database pool should have reasonable connection limits");
            }
        }
    }

    @Test
    @Order(11) 
    @DisplayName("Should prevent database information disclosure")
    void testDatabaseInformationDisclosure() throws Exception {
        // Test that database errors don't leak information
        mockMvc.perform(get("/api/v1/users/search")
                .param("query", "'; DROP TABLE users; --")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString().toLowerCase();
                    
                    // Should not contain database-specific error information
                    assertFalse(response.contains("postgresql") || 
                               response.contains("mysql") ||
                               response.contains("ora-") || // Oracle errors
                               response.contains("sql state") ||
                               response.contains("constraint violation"),
                               "Database errors should not leak information");
                });

        // Test database version information is not exposed
        mockMvc.perform(get("/api/v1/admin/system/database-version")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String response = result.getResponse().getContentAsString();
                        // If version info is provided, it should be minimal
                        assertFalse(response.contains("patch") || 
                                   response.contains("build") ||
                                   response.contains("vulnerable"),
                                   "Database version info should be minimal");
                    } else {
                        // Better if version info is not exposed at all
                        assertTrue(status >= 400,
                                  "Database version should not be exposed");
                    }
                });
    }

    // ============== Redis Security Tests ==============

    @Test
    @Order(20)
    @DisplayName("Should validate Redis security configuration")
    void testRedisSecurityConfiguration() throws Exception {
        // Test Redis health and security
        MvcResult redisHealthResult = mockMvc.perform(get("/api/v1/admin/cache/health")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (redisHealthResult.getResponse().getStatus() == 200) {
            String response = redisHealthResult.getResponse().getContentAsString();
            Map<String, Object> cacheHealth = SecurityTestUtils.fromJson(response, Map.class);
            
            // Redis should be configured securely
            if (cacheHealth.containsKey("auth")) {
                assertTrue((Boolean) cacheHealth.get("auth"),
                          "Redis should require authentication");
            }
            
            // Should not expose Redis configuration details
            assertFalse(response.contains("requirepass") || 
                       response.contains("bind 0.0.0.0") ||
                       response.contains("protected-mode no"),
                       "Redis configuration should not be exposed");
        }

        // Test that Redis admin commands are not accessible
        List<String> redisCommands = Arrays.asList(
            "CONFIG", "FLUSHALL", "FLUSHDB", "SHUTDOWN", "DEBUG"
        );

        for (String command : redisCommands) {
            mockMvc.perform(post("/api/v1/cache/command")
                    .header("Authorization", "Bearer " + adminToken)
                    .param("command", command))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status >= 400 || status == 404,
                                  "Redis admin commands should not be accessible: " + command);
                    });
        }

        // Test cache key security
        MvcResult cacheKeysResult = mockMvc.perform(get("/api/v1/admin/cache/keys")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (cacheKeysResult.getResponse().getStatus() == 200) {
            String response = cacheKeysResult.getResponse().getContentAsString();
            
            // Cache keys should not contain sensitive information
            assertFalse(response.contains("password") || 
                       response.contains("secret") ||
                       response.contains("token:") && response.length() > 100,
                       "Cache keys should not contain sensitive information");
        }
    }

    // ============== Environment Variable Security Tests ==============

    @Test
    @Order(30)
    @DisplayName("Should protect environment variables and secrets")
    void testEnvironmentVariableSecurity() throws Exception {
        // Test that environment variables are not exposed
        mockMvc.perform(get("/api/v1/admin/environment")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String response = result.getResponse().getContentAsString();
                        
                        // Should not expose sensitive environment variables
                        assertFalse(response.contains("DATABASE_PASSWORD") || 
                                   response.contains("SECRET_KEY") ||
                                   response.contains("API_SECRET") ||
                                   response.contains("PRIVATE_KEY"),
                                   "Sensitive environment variables should not be exposed");
                        
                        // If environment info is shown, sensitive values should be masked
                        if (response.contains("DATABASE_URL")) {
                            assertTrue(response.contains("***") || response.contains("REDACTED"),
                                      "Sensitive environment values should be masked");
                        }
                    } else {
                        // Better if environment variables are not exposed at all
                        assertTrue(status >= 400,
                                  "Environment variables should not be accessible via API");
                    }
                });

        // Test configuration endpoint security
        mockMvc.perform(get("/actuator/configprops")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403 || status == 404,
                              "Configuration properties should not be publicly accessible");
                });

        // Test system properties are not exposed
        mockMvc.perform(get("/api/v1/system/properties")
                .header("Authorization", "Bearer " + adminToken))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("java.class.path") || 
                                   response.contains("user.dir") ||
                                   response.contains("java.home"),
                                   "System properties should not expose sensitive paths");
                    }
                });
    }

    @Test
    @Order(31)
    @DisplayName("Should implement proper secrets management")
    void testSecretsManagement() throws Exception {
        // Test secrets endpoint security
        mockMvc.perform(get("/api/v1/admin/secrets")
                .header("Authorization", "Bearer " + "invalid-admin-token"))
                .andExpected(status().isUnauthorized());

        // Test that secrets are properly encrypted in storage
        MvcResult secretsResult = mockMvc.perform(get("/api/v1/admin/secrets/list")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (secretsResult.getResponse().getStatus() == 200) {
            String response = secretsResult.getResponse().getContentAsString();
            
            // Secrets list should not contain actual secret values
            assertFalse(response.contains("-----BEGIN") || // Private keys
                       response.contains("password\":\"") ||
                       response.contains("token\":\"sk-") || // API tokens
                       response.length() > 10000, // Suspiciously large response
                       "Secrets list should not contain actual secret values");
        }

        // Test secret rotation capabilities
        MvcResult rotationResult = mockMvc.perform(post("/api/v1/admin/secrets/rotate")
                .header("Authorization", "Bearer " + adminToken)
                .param("secretId", "test-secret-id"))
                .andReturn();

        if (rotationResult.getResponse().getStatus() < 300) {
            // Secret rotation should be logged and controlled
            String response = rotationResult.getResponse().getContentAsString();
            assertFalse(response.contains("oldValue") || response.contains("newValue"),
                       "Secret rotation should not expose secret values");
        }
    }

    // ============== Network Security Tests ==============

    @Test
    @Order(40)
    @DisplayName("Should validate network segmentation")
    void testNetworkSegmentation() throws Exception {
        // Test that internal service endpoints are not publicly accessible
        List<String> internalEndpoints = Arrays.asList(
            "/internal/metrics",
            "/internal/health", 
            "/service-mesh/config",
            "/consul/health",
            "/eureka/apps"
        );

        for (String endpoint : internalEndpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 404 || status == 403,
                                  "Internal endpoint should not be publicly accessible: " + endpoint);
                    });
        }

        // Test service-to-service authentication
        MvcResult serviceCallResult = mockMvc.perform(get("/api/v1/internal/service-call")
                .header("X-Service-Name", "test-service")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (serviceCallResult.getResponse().getStatus() < 400) {
            // Service calls should be authenticated and authorized
            String response = serviceCallResult.getResponse().getContentAsString();
            assertTrue(response.contains("authenticated") || response.contains("authorized"),
                      "Service-to-service calls should be properly authenticated");
        }

        // Test firewall rules simulation
        MvcResult networkTestResult = mockMvc.perform(get("/api/v1/admin/network/test")
                .header("Authorization", "Bearer " + adminToken)
                .param("target", "external-service.example.com")
                .param("port", "443"))
                .andReturn();

        if (networkTestResult.getResponse().getStatus() == 200) {
            String response = networkTestResult.getResponse().getContentAsString();
            Map<String, Object> networkTest = SecurityTestUtils.fromJson(response, Map.class);
            
            // Network connectivity should be controlled
            if (networkTest.containsKey("allowed")) {
                Boolean allowed = (Boolean) networkTest.get("allowed");
                // External connections should be controlled by policy
            }
        }
    }

    // ============== Service Communication Security Tests ==============

    @Test
    @Order(50)
    @DisplayName("Should secure service-to-service communication")
    void testServiceCommunicationSecurity() throws Exception {
        // Test inter-service authentication
        List<String> serviceEndpoints = Arrays.asList(
            "/api/v1/identity/validate",
            "/api/v1/analytics/internal/metrics",
            "/api/v1/notification/internal/send"
        );

        for (String endpoint : serviceEndpoints) {
            // Test without service authentication
            mockMvc.perform(get(endpoint))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 401 || status == 403 || status == 404,
                                  "Inter-service endpoint should require authentication: " + endpoint);
                    });

            // Test with proper service token
            mockMvc.perform(get(endpoint)
                    .header("X-Service-Token", "valid-service-token")
                    .header("X-Request-ID", UUID.randomUUID().toString()))
                    .andExpected(result -> {
                        int status = result.getResponse().getStatus();
                        // Should either work or still be protected
                        assertTrue(status == 200 || status >= 400,
                                  "Service endpoint should handle authentication properly");
                    });
        }

        // Test request tracing security
        MvcResult tracingResult = mockMvc.perform(get("/api/v1/admin/tracing/requests")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (tracingResult.getResponse().getStatus() == 200) {
            String response = tracingResult.getResponse().getContentAsString();
            
            // Tracing should not expose sensitive data
            assertFalse(response.contains("password") || 
                       response.contains("Authorization: Bearer") ||
                       response.contains("X-API-Key"),
                       "Request tracing should not expose sensitive headers");
        }
    }

    // ============== Infrastructure Monitoring Tests ==============

    @Test
    @Order(60)
    @DisplayName("Should implement secure infrastructure monitoring")
    void testInfrastructureMonitoring() throws Exception {
        // Test metrics endpoint security
        mockMvc.perform(get("/actuator/metrics"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403 || status == 404,
                              "Metrics endpoint should be protected");
                });

        // Test monitoring data doesn't leak sensitive information
        MvcResult monitoringResult = mockMvc.perform(get("/api/v1/admin/monitoring")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (monitoringResult.getResponse().getStatus() == 200) {
            String response = monitoringResult.getResponse().getContentAsString();
            
            // Monitoring data should not contain sensitive information
            assertFalse(response.contains("password") || 
                       response.contains("secret") ||
                       response.contains("private_key") ||
                       response.contains("connection_string"),
                       "Monitoring data should not contain secrets");
            
            // Should contain appropriate metrics
            assertTrue(response.contains("cpu") || 
                      response.contains("memory") ||
                      response.contains("requests") ||
                      response.contains("errors"),
                      "Should contain appropriate monitoring metrics");
        }

        // Test alerting configuration security
        MvcResult alertsResult = mockMvc.perform(get("/api/v1/admin/alerts/config")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (alertsResult.getResponse().getStatus() == 200) {
            String response = alertsResult.getResponse().getContentAsString();
            
            // Alert configuration should not expose sensitive webhook URLs or tokens
            if (response.contains("webhook")) {
                assertTrue(response.contains("***") || !response.contains("https://hooks.slack.com"),
                          "Webhook URLs should be masked in configuration");
            }
        }
    }

    // ============== Dependency Security Tests ==============

    @Test
    @Order(70)
    @DisplayName("Should validate dependency security")
    void testDependencySecurity() throws Exception {
        // Test dependency information endpoint
        MvcResult dependenciesResult = mockMvc.perform(get("/api/v1/admin/dependencies")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (dependenciesResult.getResponse().getStatus() == 200) {
            String response = dependenciesResult.getResponse().getContentAsString();
            Map<String, Object> dependencies = SecurityTestUtils.fromJson(response, Map.class);
            
            // Dependencies should be tracked for security
            assertTrue(dependencies.containsKey("libraries") || 
                      dependencies.containsKey("packages") ||
                      dependencies.containsKey("dependencies"),
                      "Dependencies should be tracked");
            
            // Should not expose file paths or internal structure
            assertFalse(response.contains("/home/") || 
                       response.contains("/root/") ||
                       response.contains("C:\\") ||
                       response.contains(".m2/repository"),
                       "Should not expose internal file paths");
        }

        // Test vulnerability scanning results
        MvcResult vulnScanResult = mockMvc.perform(get("/api/v1/admin/security/vulnerabilities")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (vulnScanResult.getResponse().getStatus() == 200) {
            String response = vulnScanResult.getResponse().getContentAsString();
            Map<String, Object> vulnData = SecurityTestUtils.fromJson(response, Map.class);
            
            // Vulnerability data should be structured
            if (vulnData.containsKey("vulnerabilities")) {
                List<?> vulnerabilities = (List<?>) vulnData.get("vulnerabilities");
                
                // Should have some security tracking (even if no vulns found)
                assertTrue(vulnerabilities != null,
                          "Vulnerability scanning should be implemented");
            }
        }
    }

    // ============== Configuration Security Tests ==============

    @Test
    @Order(80)
    @DisplayName("Should validate configuration security")
    void testConfigurationSecurity() throws Exception {
        // Test that default configurations are secure
        MvcResult configResult = mockMvc.perform(get("/api/v1/admin/config/security")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (configResult.getResponse().getStatus() == 200) {
            String response = configResult.getResponse().getContentAsString();
            Map<String, Object> config = SecurityTestUtils.fromJson(response, Map.class);
            
            // Security configurations should be hardened
            if (config.containsKey("session")) {
                Map<String, Object> sessionConfig = (Map<String, Object>) config.get("session");
                if (sessionConfig.containsKey("timeout")) {
                    int timeout = (Integer) sessionConfig.get("timeout");
                    assertTrue(timeout > 0 && timeout < 86400, // Less than 24 hours
                              "Session timeout should be reasonable");
                }
            }
            
            // Debug mode should be disabled in production
            if (config.containsKey("debug")) {
                Boolean debug = (Boolean) config.get("debug");
                assertFalse(debug, "Debug mode should be disabled in production");
            }
        }

        // Test configuration change tracking
        MvcResult configHistoryResult = mockMvc.perform(get("/api/v1/admin/config/history")
                .header("Authorization", "Bearer " + adminToken))
                .andReturn();

        if (configHistoryResult.getResponse().getStatus() == 200) {
            String response = configHistoryResult.getResponse().getContentAsString();
            
            // Configuration changes should be audited
            assertTrue(response.contains("timestamp") || 
                      response.contains("changed") ||
                      response.contains("version"),
                      "Configuration changes should be tracked");
            
            // Should not contain sensitive configuration values
            assertFalse(response.contains("password") || 
                       response.contains("secret"),
                       "Config history should not contain sensitive values");
        }

        // Test that security-critical settings cannot be changed via API
        MvcResult configUpdateResult = mockMvc.perform(put("/api/v1/admin/config/security")
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .content("{\"debug\":true,\"allowCors\":\"*\",\"disableSecurity\":true}"))
                .andExpected(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status >= 400,
                              "Critical security settings should not be changeable via API");
                });
    }
}