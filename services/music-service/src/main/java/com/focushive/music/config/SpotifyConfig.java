package com.focushive.music.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spotify API configuration properties.
 */
@Configuration
@ConfigurationProperties(prefix = "spotify")
@Data
public class SpotifyConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes = "user-read-private,user-read-email,user-library-read,user-top-read,playlist-read-private,playlist-read-collaborative,user-read-playback-state,user-modify-playback-state,streaming";
    
    public String[] getScopesArray() {
        return scopes.split(",");
    }
}