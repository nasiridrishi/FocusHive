package com.focushive.music.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for Spotify API integration.
 * 
 * Configures RestTemplate with appropriate timeouts and retry settings
 * for reliable communication with Spotify's Web API.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Configuration
public class SpotifyConfig {

    @Value("${spotify.api.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${spotify.api.timeout.read:10000}")
    private int readTimeout;

    /**
     * Creates a configured RestTemplate for Spotify API calls.
     * 
     * @param builder RestTemplateBuilder
     * @return Configured RestTemplate
     */
    @Bean(name = "spotifyRestTemplate")
    public RestTemplate spotifyRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .additionalMessageConverters()
            .build();
    }
}