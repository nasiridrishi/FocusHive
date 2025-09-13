package com.focushive.music.integration;

import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.entity.SpotifyCredentials;
import com.focushive.music.repository.PlaylistRepository;
import com.focushive.music.repository.PlaylistTrackRepository;
import com.focushive.music.repository.SpotifyCredentialsRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Playlist CRUD operations.
 * Tests playlist creation, reading, updating, deletion with database persistence.
 * Includes collaborative playlist permissions testing.
 */
class PlaylistIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;

    @Autowired
    private SpotifyCredentialsRepository credentialsRepository;

    private static WireMockServer spotifyMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spotify.api.base-url", () -> "http://localhost:" + spotifyMockServer.port());
    }

    @BeforeAll
    static void startWireMock() {
        spotifyMockServer = new WireMockServer(0);
        spotifyMockServer.start();
        WireMock.configureFor(spotifyMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (spotifyMockServer != null) {
            spotifyMockServer.stop();
        }
    }

    @BeforeEach
    void setupData() {
        spotifyMockServer.resetAll();
        playlistTrackRepository.deleteAll();
        playlistRepository.deleteAll();
        credentialsRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create playlist with database persistence")
    void shouldCreatePlaylistWithDatabasePersistence() {
        // Given - Valid user with Spotify credentials
        String userId = createTestUserId();
        SpotifyCredentials credentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(userId)
            .build();
        credentialsRepository.save(credentials);

        // Mock Spotify playlist creation
        stubFor(post(urlEqualTo("/v1/users/" + credentials.getSpotifyUserId() + "/playlists"))
            .withHeader("Authorization", equalTo("Bearer " + credentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_CREATE_PLAYLIST_RESPONSE)));

        // When - Create playlist
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/playlists",
            createPlaylistRequest("Focus Deep Work", "Music for deep work sessions", 
                Playlist.FocusMode.DEEP_WORK, false, userId),
            String.class
        );

        // Then - Should create and persist playlist
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        List<Playlist> playlists = playlistRepository.findByUserId(userId);
        assertThat(playlists).hasSize(1);
        
        Playlist savedPlaylist = playlists.get(0);
        assertThat(savedPlaylist.getName()).isEqualTo("Focus Deep Work");
        assertThat(savedPlaylist.getDescription()).isEqualTo("Music for deep work sessions");
        assertThat(savedPlaylist.getFocusMode()).isEqualTo(Playlist.FocusMode.DEEP_WORK);
        assertThat(savedPlaylist.getIsPublic()).isFalse();
        assertThat(savedPlaylist.getSpotifyPlaylistId()).isEqualTo("5ieJqeLJjjI8iJWaxeBLuK");
        assertThat(savedPlaylist.getCreatedAt()).isNotNull();
        assertThat(savedPlaylist.getUpdatedAt()).isNotNull();

        // Verify Spotify API call was made
        verify(postRequestedFor(urlEqualTo("/v1/users/" + credentials.getSpotifyUserId() + "/playlists")));
    }

    @Test
    @DisplayName("Should read playlist with tracks")
    void shouldReadPlaylistWithTracks() {
        // Given - Existing playlist with tracks
        String userId = createTestUserId();
        Playlist playlist = TestFixtures.playlistBuilder()
            .userId(userId)
            .build();
        playlistRepository.save(playlist);

        PlaylistTrack track1 = TestFixtures.playlistTrackBuilder()
            .playlist(playlist)
            .position(1)
            .build();
        PlaylistTrack track2 = TestFixtures.playlistTrackBuilder()
            .playlist(playlist)
            .spotifyTrackId("5FVd6KXrgO9B3JPmC8OPst")
            .trackName("Bohemian Rhapsody")
            .artistName("Queen")
            .position(2)
            .build();
        playlistTrackRepository.saveAll(List.of(track1, track2));

        // When - Get playlist by ID
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/playlists/" + playlist.getId() + "?userId=" + userId,
            String.class
        );

        // Then - Should return playlist with tracks
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        
        assertThat(responseBody)
            .contains(playlist.getName())
            .contains(playlist.getDescription())
            .contains("Never Gonna Give You Up")
            .contains("Bohemian Rhapsody")
            .contains("Rick Astley")
            .contains("Queen");
    }

    @Test
    @DisplayName("Should update playlist metadata")
    void shouldUpdatePlaylistMetadata() {
        // Given - Existing playlist
        String userId = createTestUserId();
        Playlist playlist = TestFixtures.playlistBuilder()
            .userId(userId)
            .name("Original Name")
            .description("Original Description")
            .focusMode(Playlist.FocusMode.GENERAL)
            .build();
        playlistRepository.save(playlist);

        // When - Update playlist metadata
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/music/playlists/" + playlist.getId(),
            HttpMethod.PUT,
            createPlaylistUpdateRequest("Updated Name", "Updated Description", 
                Playlist.FocusMode.CREATIVE, userId),
            String.class
        );

        // Then - Should update and persist changes
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Optional<Playlist> updatedPlaylist = playlistRepository.findById(playlist.getId());
        assertThat(updatedPlaylist).isPresent();
        
        Playlist updated = updatedPlaylist.get();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        assertThat(updated.getFocusMode()).isEqualTo(Playlist.FocusMode.CREATIVE);
        assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
    }

    @Test
    @DisplayName("Should soft delete playlist")
    void shouldSoftDeletePlaylist() {
        // Given - Existing playlist with tracks
        String userId = createTestUserId();
        Playlist playlist = TestFixtures.playlistBuilder()
            .userId(userId)
            .build();
        playlistRepository.save(playlist);

        PlaylistTrack track = TestFixtures.playlistTrackBuilder()
            .playlist(playlist)
            .build();
        playlistTrackRepository.save(track);

        // When - Delete playlist
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/music/playlists/" + playlist.getId() + "?userId=" + userId,
            HttpMethod.DELETE,
            null,
            String.class
        );

        // Then - Should soft delete (or hard delete for tests)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        // Verify playlist is deleted
        Optional<Playlist> deletedPlaylist = playlistRepository.findById(playlist.getId());
        assertThat(deletedPlaylist).isEmpty();
        
        // Verify associated tracks are also deleted
        List<PlaylistTrack> remainingTracks = playlistTrackRepository.findByPlaylistId(playlist.getId());
        assertThat(remainingTracks).isEmpty();
    }

    @Test
    @DisplayName("Should handle playlist access permissions for collaborative playlists")
    void shouldHandlePlaylistAccessPermissionsForCollaborativePlaylists() {
        // Given - Collaborative playlist owned by user1
        String ownerId = createTestUserId();
        String collaboratorId = createTestUserId();
        
        Playlist collaborativePlaylist = TestFixtures.collaborativePlaylistBuilder()
            .userId(ownerId)
            .build();
        playlistRepository.save(collaborativePlaylist);

        // When - Collaborator tries to read public collaborative playlist
        ResponseEntity<String> readResponse = restTemplate.getForEntity(
            "/api/music/playlists/" + collaborativePlaylist.getId() + "?userId=" + collaboratorId,
            String.class
        );

        // Then - Should allow read access
        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // When - Collaborator tries to modify playlist (should be allowed for collaborative)
        ResponseEntity<String> updateResponse = restTemplate.exchange(
            "/api/music/playlists/" + collaborativePlaylist.getId() + "/tracks",
            HttpMethod.POST,
            createAddTrackRequest(TestFixtures.REAL_SPOTIFY_TRACK_IDS[0], collaboratorId),
            String.class
        );

        // Then - Should allow track addition for collaborative playlists
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Should prevent unauthorized access to private playlists")
    void shouldPreventUnauthorizedAccessToPrivatePlaylists() {
        // Given - Private playlist owned by user1
        String ownerId = createTestUserId();
        String otherUserId = createTestUserId();
        
        Playlist privatePlaylist = TestFixtures.playlistBuilder()
            .userId(ownerId)
            .isPublic(false)
            .type(Playlist.PlaylistType.CUSTOM)
            .build();
        playlistRepository.save(privatePlaylist);

        // When - Other user tries to access private playlist
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/playlists/" + privatePlaylist.getId() + "?userId=" + otherUserId,
            String.class
        );

        // Then - Should deny access
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should list user's playlists with pagination")
    void shouldListUserPlaylistsWithPagination() {
        // Given - Multiple playlists for a user
        String userId = createTestUserId();
        for (int i = 0; i < 15; i++) {
            Playlist playlist = TestFixtures.playlistBuilder()
                .name("Playlist " + i)
                .userId(userId)
                .build();
            playlistRepository.save(playlist);
        }

        // When - Get first page of playlists
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/playlists?userId=" + userId + "&page=0&size=10",
            String.class
        );

        // Then - Should return paginated results
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        
        // Should contain pagination metadata
        assertThat(responseBody)
            .contains("\"totalElements\":15")
            .contains("\"totalPages\":2")
            .contains("\"first\":true")
            .contains("\"last\":false");
    }

    @Test
    @DisplayName("Should handle playlist creation without Spotify credentials")
    void shouldHandlePlaylistCreationWithoutSpotifyCredentials() {
        // Given - User without Spotify credentials
        String userId = createTestUserId();

        // When - Try to create playlist
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/playlists",
            createPlaylistRequest("Local Playlist", "Local only playlist", 
                Playlist.FocusMode.STUDY, false, userId),
            String.class
        );

        // Then - Should create local playlist without Spotify integration
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        List<Playlist> playlists = playlistRepository.findByUserId(userId);
        assertThat(playlists).hasSize(1);
        
        Playlist savedPlaylist = playlists.get(0);
        assertThat(savedPlaylist.getName()).isEqualTo("Local Playlist");
        assertThat(savedPlaylist.getSpotifyPlaylistId()).isNull();
    }

    // Helper methods for creating test requests
    private HttpEntity<String> createPlaylistRequest(String name, String description, 
            Playlist.FocusMode focusMode, boolean isPublic, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "name": "%s",
                "description": "%s",
                "focusMode": "%s",
                "isPublic": %s,
                "userId": "%s"
            }
            """, name, description, focusMode, isPublic, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }

    private HttpEntity<String> createPlaylistUpdateRequest(String name, String description, 
            Playlist.FocusMode focusMode, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "name": "%s",
                "description": "%s",
                "focusMode": "%s",
                "userId": "%s"
            }
            """, name, description, focusMode, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }

    private HttpEntity<String> createAddTrackRequest(String spotifyTrackId, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "spotifyTrackId": "%s",
                "userId": "%s"
            }
            """, spotifyTrackId, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }
}