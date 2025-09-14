package com.focushive.music;

import com.focushive.music.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class DiagnosticTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAccessHealthEndpoint() {
        String url = "http://localhost:" + port + "/api/music/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        System.out.println("Health endpoint status: " + response.getStatusCode());
        System.out.println("Health endpoint body: " + response.getBody());

        // Health endpoint should be accessible
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void shouldListActuatorEndpoints() {
        String url = "http://localhost:" + port + "/api/music/actuator";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        System.out.println("Actuator endpoint status: " + response.getStatusCode());
        System.out.println("Actuator endpoint body: " + response.getBody());
    }

    @Test
    void shouldTestPlaylistEndpointMapping() {
        String url = "http://localhost:" + port + "/api/music/playlists?userId=test-user-123";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        System.out.println("Playlist endpoint status: " + response.getStatusCode());
        System.out.println("Playlist endpoint body: " + response.getBody());

        // This should not be 404, could be 200 (empty list) or some other error
        assertThat(response.getStatusCode().value()).describedAs("Should not be 404 NOT_FOUND").isNotEqualTo(404);
    }
}