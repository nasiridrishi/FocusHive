package com.focushive.music.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.music.dto.SpotifyDTO.*;
import com.focushive.music.model.StreamingCredentials;
import com.focushive.music.repository.StreamingCredentialsRepository;
import com.focushive.music.service.SpotifyIntegrationService;
import com.focushive.music.service.TokenEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Spotify OAuth2 flow.
 * Tests the complete authentication flow, token management, and API endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Spotify OAuth Integration Tests")
class SpotifyOAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StreamingCredentialsRepository credentialsRepository;

    @MockBean
    private TokenEncryptionService tokenEncryptionService;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Mock token encryption/decryption
        given(tokenEncryptionService.encrypt(anyString())).willReturn("encrypted_token");
        given(tokenEncryptionService.decrypt(anyString())).willReturn("decrypted_token");
        given(tokenEncryptionService.maskToken(anyString())).willReturn("****masked****");
    }

    @Nested
    @DisplayName("OAuth Authorization Flow")
    class OAuthAuthorizationFlow {

        @Test
        @DisplayName("Should redirect to Spotify authorization URL")
        @WithMockUser(roles = "USER")
        void shouldRedirectToSpotifyAuth() throws Exception {
            mockMvc.perform(get("/api/music/spotify/auth")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("https://accounts.spotify.com/authorize*"))
                .andExpect(redirectedUrlPattern("*client_id=*"))
                .andExpect(redirectedUrlPattern("*redirect_uri=*"))
                .andExpect(redirectedUrlPattern("*scope=*"));
        }

        @Test
        @DisplayName("Should handle OAuth callback successfully")
        void shouldHandleCallbackSuccessfully() throws Exception {
            // Given
            String code = "test_authorization_code";
            String state = testUserId + ":test_state";

            // When & Then
            mockMvc.perform(get("/api/music/spotify/callback")
                    .param("code", code)
                    .param("state", state))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.serviceName").value("spotify"))
                .andExpect(jsonPath("$.message").value("Successfully connected to Spotify"));
        }

        @Test
        @DisplayName("Should handle callback with missing parameters")
        void shouldHandleCallbackWithMissingParams() throws Exception {
            mockMvc.perform(get("/api/music/spotify/callback"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Missing required parameters"));
        }

        @Test
        @DisplayName("Should handle callback with error parameter")
        void shouldHandleCallbackWithError() throws Exception {
            mockMvc.perform(get("/api/music/spotify/callback")
                    .param("error", "access_denied")
                    .param("state", testUserId + ":test_state"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authorization failed: access_denied"));
        }
    }

    @Nested
    @DisplayName("Connection Status Management")
    class ConnectionStatusManagement {

        @Test
        @DisplayName("Should return not connected status when no credentials")
        @WithMockUser(roles = "USER")
        void shouldReturnNotConnectedWhenNoCredentials() throws Exception {
            mockMvc.perform(get("/api/music/spotify/status")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.tokenExpired").value(false));
        }

        @Test
        @DisplayName("Should return connected status with valid credentials")
        @WithMockUser(roles = "USER")
        void shouldReturnConnectedWithValidCredentials() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(get("/api/music/spotify/status")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.tokenExpired").value(false));
        }

        @Test
        @DisplayName("Should return expired status with expired credentials")
        @WithMockUser(roles = "USER")
        void shouldReturnExpiredWithExpiredCredentials() throws Exception {
            // Given
            createExpiredSpotifyCredentials();

            // When & Then
            mockMvc.perform(get("/api/music/spotify/status")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.tokenExpired").value(true));
        }
    }

    @Nested
    @DisplayName("Token Management")
    class TokenManagement {

        @Test
        @DisplayName("Should refresh token successfully")
        @WithMockUser(roles = "USER")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(post("/api/music/spotify/refresh")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
        }

        @Test
        @DisplayName("Should fail refresh when not connected")
        @WithMockUser(roles = "USER")
        void shouldFailRefreshWhenNotConnected() throws Exception {
            mockMvc.perform(post("/api/music/spotify/refresh")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Service Disconnection")
    class ServiceDisconnection {

        @Test
        @DisplayName("Should disconnect Spotify successfully")
        @WithMockUser(roles = "USER")
        void shouldDisconnectSpotifySuccessfully() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(delete("/api/music/spotify/disconnect")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.serviceName").value("spotify"))
                .andExpect(jsonPath("$.message").value("Successfully disconnected from Spotify"));

            // Verify credentials are deleted
            Optional<StreamingCredentials> credentials = credentialsRepository
                .findByUserIdAndPlatform(testUserId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY);
            assertThat(credentials).isEmpty();
        }

        @Test
        @DisplayName("Should handle disconnect when not connected")
        @WithMockUser(roles = "USER")
        void shouldHandleDisconnectWhenNotConnected() throws Exception {
            mockMvc.perform(delete("/api/music/spotify/disconnect")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Spotify not connected or disconnect failed"));
        }
    }

    @Nested
    @DisplayName("API Integration")
    class ApiIntegration {

        @Test
        @DisplayName("Should search tracks successfully")
        @WithMockUser(roles = "USER")
        void shouldSearchTracksSuccessfully() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(get("/api/music/spotify/search")
                    .param("query", "Bohemian Rhapsody")
                    .param("limit", "10")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should get user playlists successfully")
        @WithMockUser(roles = "USER")
        void shouldGetUserPlaylistsSuccessfully() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(get("/api/music/spotify/playlists")
                    .param("limit", "20")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should get audio features successfully")
        @WithMockUser(roles = "USER")
        void shouldGetAudioFeaturesSuccessfully() throws Exception {
            // Given
            createValidSpotifyCredentials();

            // When & Then
            mockMvc.perform(get("/api/music/spotify/audio-features/4iV5W9uYEdYUVa79Axb7Rh")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should require authentication for protected endpoints")
        void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
            mockMvc.perform(get("/api/music/spotify/status"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/music/spotify/search")
                    .param("query", "test"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/music/spotify/disconnect"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @DisplayName("Should validate search query parameter")
        @WithMockUser(roles = "USER")
        void shouldValidateSearchQueryParameter() throws Exception {
            createValidSpotifyCredentials();

            mockMvc.perform(get("/api/music/spotify/search")
                    .param("query", "") // Empty query
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate limit parameters")
        @WithMockUser(roles = "USER")
        void shouldValidateLimitParameters() throws Exception {
            createValidSpotifyCredentials();

            // Test limit too high
            mockMvc.perform(get("/api/music/spotify/search")
                    .param("query", "test")
                    .param("limit", "100") // Too high
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isBadRequest());

            // Test limit too low
            mockMvc.perform(get("/api/music/spotify/playlists")
                    .param("limit", "0") // Too low
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate track ID parameter")
        @WithMockUser(roles = "USER")
        void shouldValidateTrackIdParameter() throws Exception {
            createValidSpotifyCredentials();

            mockMvc.perform(get("/api/music/spotify/audio-features/")
                    .with(jwt().jwt(builder -> builder.subject(testUserId.toString()))))
                .andDo(print())
                .andExpect(status().isNotFound()); // Empty track ID results in 404
        }
    }

    // Helper methods

    private void createValidSpotifyCredentials() {
        StreamingCredentials credentials = StreamingCredentials.builder()
            .userId(testUserId.toString())
            .platform(StreamingCredentials.StreamingPlatform.SPOTIFY)
            .accessToken("encrypted_access_token")
            .refreshToken("encrypted_refresh_token")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .scope("user-read-private user-read-email")
            .isActive(true)
            .build();
        
        credentials.setCreatedAt(LocalDateTime.now().minusDays(1));
        credentials.setUpdatedAt(LocalDateTime.now());
        
        credentialsRepository.save(credentials);
    }

    private void createExpiredSpotifyCredentials() {
        StreamingCredentials credentials = StreamingCredentials.builder()
            .userId(testUserId.toString())
            .platform(StreamingCredentials.StreamingPlatform.SPOTIFY)
            .accessToken("encrypted_access_token")
            .refreshToken("encrypted_refresh_token")
            .expiresAt(LocalDateTime.now().minusMinutes(10)) // Expired
            .scope("user-read-private user-read-email")
            .isActive(true)
            .build();
        
        credentials.setCreatedAt(LocalDateTime.now().minusDays(1));
        credentials.setUpdatedAt(LocalDateTime.now());
        
        credentialsRepository.save(credentials);
    }
}