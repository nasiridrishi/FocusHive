package com.focushive.music.integration;

import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import com.focushive.music.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Integration tests for collaborative playlist real-time updates via WebSockets.
 * Tests WebSocket connections, real-time track operations, participant notifications,
 * and conflict resolution for concurrent edits.
 */
class CollaborativePlaylistIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private Playlist testPlaylist;
    private String websocketUrl;

    @BeforeEach
    void setupWebSocket() throws Exception {
        playlistTrackRepository.deleteAll();
        playlistRepository.deleteAll();
        
        // Setup WebSocket client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        websocketUrl = "ws://localhost:" + port + "/ws";
        
        // Create test collaborative playlist
        testPlaylist = TestFixtures.collaborativePlaylistBuilder()
            .userId(createTestUserId())
            .build();
        playlistRepository.save(testPlaylist);
        
        // Connect to WebSocket
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        stompSession = stompClient.connect(websocketUrl, sessionHandler).get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void teardownWebSocket() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    @Test
    @DisplayName("Should establish WebSocket connection for playlist")
    void shouldEstablishWebSocketConnectionForPlaylist() {
        // Given - WebSocket connection established in setup
        
        // Then - Connection should be active
        assertThat(stompSession.isConnected()).isTrue();
        
        // Should be able to subscribe to playlist topic
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        StompSession.Subscription subscription = stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );
        
        assertThat(subscription).isNotNull();
        assertThat(subscription.getSubscriptionId()).isNotNull();
    }

    @Test
    @DisplayName("Should broadcast track addition to all subscribers")
    void shouldBroadcastTrackAdditionToAllSubscribers() throws Exception {
        // Given - Two clients subscribed to playlist
        BlockingQueue<String> client1Messages = new LinkedBlockingQueue<>();
        BlockingQueue<String> client2Messages = new LinkedBlockingQueue<>();
        
        StompSession.Subscription sub1 = stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(client1Messages)
        );
        
        // Create second client
        StompSession client2Session = stompClient.connect(websocketUrl, new TestStompSessionHandler())
            .get(5, TimeUnit.SECONDS);
        StompSession.Subscription sub2 = client2Session.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(client2Messages)
        );

        // When - Add track via WebSocket
        String addTrackMessage = String.format("""
            {
                "action": "ADD_TRACK",
                "playlistId": "%s",
                "spotifyTrackId": "%s",
                "userId": "%s"
            }
            """, testPlaylist.getId(), TestFixtures.REAL_SPOTIFY_TRACK_IDS[0], testPlaylist.getUserId());

        stompSession.send("/app/playlist/track/add", addTrackMessage);

        // Then - Both clients should receive the update
        String message1 = client1Messages.poll(5, TimeUnit.SECONDS);
        String message2 = client2Messages.poll(5, TimeUnit.SECONDS);
        
        assertThat(message1).isNotNull();
        assertThat(message2).isNotNull();
        
        assertThat(message1)
            .contains("TRACK_ADDED")
            .contains(TestFixtures.REAL_SPOTIFY_TRACK_IDS[0])
            .contains(testPlaylist.getId().toString());
        
        assertThat(message2).isEqualTo(message1);

        // Verify track was persisted to database
        await().atMost(java.time.Duration.ofSeconds(3))
            .until(() -> {
                List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistId(testPlaylist.getId());
                return tracks.size() == 1;
            });
        
        client2Session.disconnect();
    }

    @Test
    @DisplayName("Should broadcast track removal to all subscribers")
    void shouldBroadcastTrackRemovalToAllSubscribers() throws Exception {
        // Given - Playlist with existing track
        PlaylistTrack existingTrack = TestFixtures.playlistTrackBuilder()
            .playlist(testPlaylist)
            .spotifyTrackId(TestFixtures.REAL_SPOTIFY_TRACK_IDS[0])
            .build();
        playlistTrackRepository.save(existingTrack);

        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        // When - Remove track via WebSocket
        String removeTrackMessage = String.format("""
            {
                "action": "REMOVE_TRACK",
                "playlistId": "%s",
                "trackId": "%s",
                "userId": "%s"
            }
            """, testPlaylist.getId(), existingTrack.getId(), testPlaylist.getUserId());

        stompSession.send("/app/playlist/track/remove", removeTrackMessage);

        // Then - Should receive removal notification
        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message)
            .contains("TRACK_REMOVED")
            .contains(existingTrack.getId().toString())
            .contains(testPlaylist.getId().toString());

        // Verify track was removed from database
        await().atMost(java.time.Duration.ofSeconds(3))
            .until(() -> {
                List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistId(testPlaylist.getId());
                return tracks.isEmpty();
            });
    }

    @Test
    @DisplayName("Should handle participant join notifications")
    void shouldHandleParticipantJoinNotifications() throws Exception {
        // Given - Subscribed to playlist
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        // When - New participant joins
        String joinMessage = String.format("""
            {
                "action": "JOIN_PLAYLIST",
                "playlistId": "%s",
                "userId": "%s",
                "userName": "Test User"
            }
            """, testPlaylist.getId(), createTestUserId());

        stompSession.send("/app/playlist/join", joinMessage);

        // Then - Should receive join notification
        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message)
            .contains("USER_JOINED")
            .contains("Test User")
            .contains(testPlaylist.getId().toString());
    }

    @Test
    @DisplayName("Should handle participant leave notifications")
    void shouldHandleParticipantLeaveNotifications() throws Exception {
        // Given - Subscribed to playlist with existing participant
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        String participantId = createTestUserId();

        // When - Participant leaves
        String leaveMessage = String.format("""
            {
                "action": "LEAVE_PLAYLIST",
                "playlistId": "%s",
                "userId": "%s",
                "userName": "Test User"
            }
            """, testPlaylist.getId(), participantId);

        stompSession.send("/app/playlist/leave", leaveMessage);

        // Then - Should receive leave notification
        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message)
            .contains("USER_LEFT")
            .contains("Test User")
            .contains(testPlaylist.getId().toString());
    }

    @Test
    @DisplayName("Should handle concurrent track additions without conflicts")
    void shouldHandleConcurrentTrackAdditionsWithoutConflicts() throws Exception {
        // Given - Multiple clients
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        StompSession client2 = stompClient.connect(websocketUrl, new TestStompSessionHandler())
            .get(5, TimeUnit.SECONDS);

        // When - Both clients add tracks simultaneously
        String addTrack1Message = String.format("""
            {
                "action": "ADD_TRACK",
                "playlistId": "%s",
                "spotifyTrackId": "%s",
                "userId": "%s"
            }
            """, testPlaylist.getId(), TestFixtures.REAL_SPOTIFY_TRACK_IDS[0], testPlaylist.getUserId());

        String addTrack2Message = String.format("""
            {
                "action": "ADD_TRACK",
                "playlistId": "%s",
                "spotifyTrackId": "%s",
                "userId": "%s"
            }
            """, testPlaylist.getId(), TestFixtures.REAL_SPOTIFY_TRACK_IDS[1], createTestUserId());

        // Send both messages rapidly
        stompSession.send("/app/playlist/track/add", addTrack1Message);
        client2.send("/app/playlist/track/add", addTrack2Message);

        // Then - Should receive two separate addition notifications
        String message1 = messages.poll(5, TimeUnit.SECONDS);
        String message2 = messages.poll(5, TimeUnit.SECONDS);
        
        assertThat(message1).isNotNull();
        assertThat(message2).isNotNull();

        // Verify both tracks were added to database
        await().atMost(java.time.Duration.ofSeconds(3))
            .until(() -> {
                List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistId(testPlaylist.getId());
                return tracks.size() == 2;
            });

        List<PlaylistTrack> finalTracks = playlistTrackRepository.findByPlaylistId(testPlaylist.getId());
        assertThat(finalTracks)
            .extracting(PlaylistTrack::getSpotifyTrackId)
            .containsExactlyInAnyOrder(
                TestFixtures.REAL_SPOTIFY_TRACK_IDS[0], 
                TestFixtures.REAL_SPOTIFY_TRACK_IDS[1]
            );

        client2.disconnect();
    }

    @Test
    @DisplayName("Should handle track reordering via WebSocket")
    void shouldHandleTrackReorderingViaWebSocket() throws Exception {
        // Given - Playlist with multiple tracks
        PlaylistTrack track1 = TestFixtures.playlistTrackBuilder()
            .playlist(testPlaylist)
            .spotifyTrackId(TestFixtures.REAL_SPOTIFY_TRACK_IDS[0])
            .position(1)
            .build();
        PlaylistTrack track2 = TestFixtures.playlistTrackBuilder()
            .playlist(testPlaylist)
            .spotifyTrackId(TestFixtures.REAL_SPOTIFY_TRACK_IDS[1])
            .position(2)
            .build();
        playlistTrackRepository.saveAll(List.of(track1, track2));

        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        // When - Reorder tracks (move track1 to position 2)
        String reorderMessage = String.format("""
            {
                "action": "REORDER_TRACK",
                "playlistId": "%s",
                "trackId": "%s",
                "newPosition": 2,
                "userId": "%s"
            }
            """, testPlaylist.getId(), track1.getId(), testPlaylist.getUserId());

        stompSession.send("/app/playlist/track/reorder", reorderMessage);

        // Then - Should receive reorder notification
        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message)
            .contains("TRACK_REORDERED")
            .contains(track1.getId().toString())
            .contains("\"newPosition\":2");

        // Verify positions were updated in database
        await().atMost(java.time.Duration.ofSeconds(3))
            .until(() -> {
                PlaylistTrack updatedTrack1 = playlistTrackRepository.findById(track1.getId()).orElse(null);
                PlaylistTrack updatedTrack2 = playlistTrackRepository.findById(track2.getId()).orElse(null);
                return updatedTrack1 != null && updatedTrack2 != null && 
                       updatedTrack1.getPosition() == 2 && updatedTrack2.getPosition() == 1;
            });
    }

    @Test
    @DisplayName("Should validate permissions for non-collaborative playlists")
    void shouldValidatePermissionsForNonCollaborativePlaylists() throws Exception {
        // Given - Non-collaborative playlist
        Playlist privatePlaylist = TestFixtures.playlistBuilder()
            .userId(createTestUserId())
            .type(Playlist.PlaylistType.CUSTOM)
            .isPublic(false)
            .build();
        playlistRepository.save(privatePlaylist);

        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + privatePlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        // When - Unauthorized user tries to add track
        String unauthorizedUser = createTestUserId();
        String addTrackMessage = String.format("""
            {
                "action": "ADD_TRACK",
                "playlistId": "%s",
                "spotifyTrackId": "%s",
                "userId": "%s"
            }
            """, privatePlaylist.getId(), TestFixtures.REAL_SPOTIFY_TRACK_IDS[0], unauthorizedUser);

        stompSession.send("/app/playlist/track/add", addTrackMessage);

        // Then - Should receive permission denied notification
        String message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message)
            .contains("PERMISSION_DENIED")
            .contains(unauthorizedUser)
            .contains(privatePlaylist.getId().toString());

        // Verify track was not added to database
        List<PlaylistTrack> tracks = playlistTrackRepository.findByPlaylistId(privatePlaylist.getId());
        assertThat(tracks).isEmpty();
    }

    @Test
    @DisplayName("Should handle WebSocket disconnection gracefully")
    void shouldHandleWebSocketDisconnectionGracefully() throws Exception {
        // Given - Connected WebSocket client
        assertThat(stompSession.isConnected()).isTrue();

        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        stompSession.subscribe(
            "/topic/playlist/" + testPlaylist.getId(), 
            new TestStompFrameHandler(messages)
        );

        // When - Client disconnects abruptly
        stompSession.disconnect();

        // Then - Should handle disconnection without errors
        assertThat(stompSession.isConnected()).isFalse();

        // Server should clean up any session-related resources
        // This would be verified through server-side monitoring or logs
    }

    // Helper classes for WebSocket testing
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // Connection established
        }

        @Override
        public void handleException(StompSession session, StompCommand command, 
                                  StompHeaders headers, byte[] payload, Throwable exception) {
            throw new RuntimeException("WebSocket error", exception);
        }
    }

    private static class TestStompFrameHandler implements StompFrameHandler {
        private final BlockingQueue<String> messages;

        public TestStompFrameHandler(BlockingQueue<String> messages) {
            this.messages = messages;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (payload instanceof String) {
                messages.offer((String) payload);
            }
        }
    }
}