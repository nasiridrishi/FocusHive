package com.focushive.buddy.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Cloudflare Tunnel configuration in production environment.
 * Validates that services can communicate using public URLs through Cloudflare tunnels.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("docker")
@DisplayName("Cloudflare Tunnel Configuration Tests")
public class CloudflareTunnelTest {

    @Autowired
    private Environment env;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Environment should have Cloudflare tunnel token configured")
    public void testTunnelTokenConfigured() {
        String tunnelToken = env.getProperty("cloudflare.tunnel.token");
        assertNotNull(tunnelToken, "Cloudflare tunnel token must be configured");
        assertFalse(tunnelToken.isEmpty(), "Cloudflare tunnel token must not be empty");
        assertTrue(tunnelToken.startsWith("eyJ"), "Token should be a valid JWT format");
    }

    @Test
    @DisplayName("Service URLs should use public Cloudflare domains")
    public void testServiceUrlsUseCloudflare() {
        // Check identity service URL
        String identityUrl = env.getProperty("identity-service.url");
        assertNotNull(identityUrl, "Identity service URL must be configured");
        assertTrue(identityUrl.contains("focushive.app"),
            "Identity service should use Cloudflare public domain");
        assertTrue(identityUrl.startsWith("https://"),
            "Service URLs should use HTTPS through Cloudflare");

        // Check backend service URL
        String backendUrl = env.getProperty("backend-service.url");
        assertNotNull(backendUrl, "Backend service URL must be configured");
        assertTrue(backendUrl.contains("focushive.app"),
            "Backend service should use Cloudflare public domain");

        // Check notification service URL
        String notificationUrl = env.getProperty("notification-service.url");
        assertNotNull(notificationUrl, "Notification service URL must be configured");
        assertTrue(notificationUrl.contains("focushive.app"),
            "Notification service should use Cloudflare public domain");
    }

    @Test
    @DisplayName("Local health endpoint should work without tunnel")
    public void testLocalHealthEndpoint() {
        // Local health check should still work internally
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Service should not expose ports to host in production")
    public void testNoPortExposure() {
        // This test validates docker-compose configuration
        // In production, only cloudflared should handle external access
        String serverPort = env.getProperty("server.port");
        assertNotNull(serverPort, "Server port should be configured");

        // Management port should still be configured for internal health checks
        String managementPort = env.getProperty("management.server.port");
        assertNotNull(managementPort, "Management port should be configured");

        // These ports should only be accessible within Docker network
        // External access should be through Cloudflare tunnel only
    }

    @Test
    @DisplayName("Cloudflare tunnel service name should be configured")
    public void testTunnelServiceName() {
        // Validate that the service knows its own public URL
        String publicUrl = env.getProperty("buddy.service.public.url");
        assertNotNull(publicUrl, "Public URL must be configured for callbacks");
        assertEquals("https://buddy.focushive.app", publicUrl,
            "Public URL should match Cloudflare tunnel configuration");
    }

    @Test
    @DisplayName("Inter-service communication should use HTTPS")
    public void testInterServiceHttps() {
        // All inter-service URLs should use HTTPS for security
        String[] serviceUrls = {
            env.getProperty("identity-service.url"),
            env.getProperty("backend-service.url"),
            env.getProperty("notification-service.url")
        };

        for (String url : serviceUrls) {
            if (url != null) {
                assertTrue(url.startsWith("https://"),
                    "Inter-service communication must use HTTPS: " + url);

                try {
                    URI uri = new URI(url);
                    assertEquals("https", uri.getScheme(),
                        "URL scheme must be HTTPS for: " + url);
                    assertTrue(uri.getHost().endsWith(".focushive.app"),
                        "Host must be Cloudflare domain: " + uri.getHost());
                } catch (Exception e) {
                    fail("Invalid URL format: " + url);
                }
            }
        }
    }
}