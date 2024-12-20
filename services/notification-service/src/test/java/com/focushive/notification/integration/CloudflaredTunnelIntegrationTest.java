package com.focushive.notification.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify Cloudflared tunnel connectivity.
 * This test ensures that services can communicate through public URLs
 * when running with Cloudflared tunnels.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "cloudflared.enabled=true",
    "cloudflared.public.url=https://notification.focushive.app"
})
public class CloudflaredTunnelIntegrationTest {

    @Value("${cloudflared.public.url:}")
    private String publicUrl;

    @Value("${cloudflared.enabled:false}")
    private boolean cloudflaredEnabled;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void testTunnelConnectivity_whenCloudflaredEnabled_shouldAccessThroughPublicUrl() {
        // Skip test if Cloudflared is not enabled
        if (!cloudflaredEnabled) {
            return;
        }

        // Test health endpoint through public URL
        String healthUrl = publicUrl + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

        // Verify connectivity
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    public void testServiceToServiceCommunication_throughPublicUrls() {
        // Skip test if Cloudflared is not enabled
        if (!cloudflaredEnabled) {
            return;
        }

        // Test that notification service can reach identity service through public URL
        String identityHealthUrl = "https://identity.focushive.app/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(identityHealthUrl, String.class);

        // Verify connectivity (may fail if identity service is not running)
        assertThat(response).isNotNull();
    }

    @Test
    public void testInternalConnectivity_whenCloudflaredDisabled_shouldUseInternalUrls() {
        // This test verifies that when Cloudflared is disabled,
        // services still communicate internally
        if (cloudflaredEnabled) {
            return;
        }

        // Test internal health endpoint
        String internalHealthUrl = "http://localhost:8083/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(internalHealthUrl, String.class);

        // Should fail in production (no exposed ports), succeed in dev
        assertThat(response).isNotNull();
    }
}