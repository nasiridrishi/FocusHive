package com.focushive.music.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController();
    }

    @Test
    void healthEndpointShouldReturnOk() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("music-service");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }

    @Test
    void healthEndpointShouldIndicateSpotifyApiStatus() {
        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getBody()).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, String> components = (Map<String, String>) response.getBody().get("components");

        assertThat(components).isNotNull();
        assertThat(components.get("spotify")).isEqualTo("UP");
        assertThat(components.get("database")).isEqualTo("UP");
    }
}