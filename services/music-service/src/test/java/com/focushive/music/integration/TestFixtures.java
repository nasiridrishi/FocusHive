package com.focushive.music.integration;

import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.entity.SpotifyCredentials;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test fixtures providing realistic test data for music service integration tests.
 * Contains mock Spotify API responses and entity builders for TDD approach.
 */
public class TestFixtures {

    // Spotify API Mock Responses
    public static final String SPOTIFY_AUTH_RESPONSE = """
        {
            "access_token": "BQDyP4h_mock_access_token_here",
            "token_type": "Bearer",
            "scope": "user-read-private user-read-email playlist-read-private playlist-modify-public playlist-modify-private",
            "expires_in": 3600,
            "refresh_token": "AQBmock_refresh_token_here"
        }
        """;

    public static final String SPOTIFY_USER_PROFILE_RESPONSE = """
        {
            "id": "testuser123",
            "display_name": "Test User",
            "email": "test@example.com",
            "country": "US",
            "product": "premium",
            "images": [
                {
                    "url": "https://i.scdn.co/image/ab67757000003b82mock_image",
                    "height": 300,
                    "width": 300
                }
            ]
        }
        """;

    public static final String SPOTIFY_SEARCH_TRACKS_RESPONSE = """
        {
            "tracks": {
                "items": [
                    {
                        "id": "4iV5W9uYEdYUVa79Axb7Rh",
                        "name": "Never Gonna Give You Up",
                        "artists": [
                            {
                                "id": "0gxyHStUsqpMadRV0Di1Qt",
                                "name": "Rick Astley"
                            }
                        ],
                        "album": {
                            "id": "6XzKSFffJLcnllIkUSuf45",
                            "name": "Whenever You Need Somebody",
                            "images": [
                                {
                                    "url": "https://i.scdn.co/image/ab67616d00001e02mock_album",
                                    "height": 300,
                                    "width": 300
                                }
                            ]
                        },
                        "duration_ms": 213573,
                        "preview_url": "https://p.scdn.co/mp3-preview/mock_preview",
                        "external_urls": {
                            "spotify": "https://open.spotify.com/track/4iV5W9uYEdYUVa79Axb7Rh"
                        }
                    },
                    {
                        "id": "5FVd6KXrgO9B3JPmC8OPst",
                        "name": "Bohemian Rhapsody",
                        "artists": [
                            {
                                "id": "1dfeR4HaWDbWqFHLkxsg1d",
                                "name": "Queen"
                            }
                        ],
                        "album": {
                            "id": "6i6folBtxKV28WX3ZgUIB8",
                            "name": "A Night at the Opera",
                            "images": [
                                {
                                    "url": "https://i.scdn.co/image/ab67616d00001e02mock_queen",
                                    "height": 300,
                                    "width": 300
                                }
                            ]
                        },
                        "duration_ms": 355000,
                        "preview_url": "https://p.scdn.co/mp3-preview/mock_queen_preview",
                        "external_urls": {
                            "spotify": "https://open.spotify.com/track/5FVd6KXrgO9B3JPmC8OPst"
                        }
                    }
                ]
            }
        }
        """;

    public static final String SPOTIFY_CREATE_PLAYLIST_RESPONSE = """
        {
            "id": "5ieJqeLJjjI8iJWaxeBLuK",
            "name": "Focus Deep Work",
            "description": "Music for deep work sessions",
            "collaborative": false,
            "public": false,
            "snapshot_id": "MTLmock_snapshot_id",
            "owner": {
                "id": "testuser123",
                "display_name": "Test User"
            },
            "tracks": {
                "href": "https://api.spotify.com/v1/playlists/5ieJqeLJjjI8iJWaxeBLuK/tracks",
                "total": 0
            },
            "external_urls": {
                "spotify": "https://open.spotify.com/playlist/5ieJqeLJjjI8iJWaxeBLuK"
            }
        }
        """;

    public static final String SPOTIFY_RECOMMENDATIONS_RESPONSE = """
        {
            "tracks": [
                {
                    "id": "0c6xIDDpzE81m2q797ordA",
                    "name": "Focus Flow",
                    "artists": [
                        {
                            "id": "mock_artist_id",
                            "name": "Study Music Academy"
                        }
                    ],
                    "album": {
                        "id": "mock_album_id",
                        "name": "Deep Work Instrumentals"
                    },
                    "duration_ms": 240000,
                    "preview_url": "https://p.scdn.co/mp3-preview/mock_focus_preview"
                },
                {
                    "id": "1a2b3c4d5e6f7g8h9i0j",
                    "name": "Productive Vibes",
                    "artists": [
                        {
                            "id": "mock_artist_id_2",
                            "name": "Concentration Collective"
                        }
                    ],
                    "album": {
                        "id": "mock_album_id_2",
                        "name": "Study Session Sounds"
                    },
                    "duration_ms": 180000,
                    "preview_url": "https://p.scdn.co/mp3-preview/mock_productive_preview"
                }
            ]
        }
        """;

    public static final String SPOTIFY_ERROR_RATE_LIMITED_RESPONSE = """
        {
            "error": {
                "status": 429,
                "message": "API rate limit exceeded"
            }
        }
        """;

    public static final String SPOTIFY_ERROR_UNAUTHORIZED_RESPONSE = """
        {
            "error": {
                "status": 401,
                "message": "The access token expired"
            }
        }
        """;

    // Entity Builders
    public static SpotifyCredentials.SpotifyCredentialsBuilder spotifyCredentialsBuilder() {
        return SpotifyCredentials.builder()
                .id(UUID.randomUUID())
                .userId("test-user-123")
                .accessToken("BQDyP4h_mock_access_token_here")
                .refreshToken("AQBmock_refresh_token_here")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .scope("user-read-private user-read-email playlist-read-private playlist-modify-public playlist-modify-private")
                .spotifyUserId("testuser123")
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now());
    }

    public static Playlist.PlaylistBuilder playlistBuilder() {
        return Playlist.builder()
                .id(UUID.randomUUID())
                .name("Test Playlist")
                .description("A test playlist for integration testing")
                .userId("test-user-123")
                .type(Playlist.PlaylistType.CUSTOM)
                .focusMode(Playlist.FocusMode.DEEP_WORK)
                .isPublic(false)
                .spotifyPlaylistId("5ieJqeLJjjI8iJWaxeBLuK")
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now());
    }

    public static Playlist.PlaylistBuilder collaborativePlaylistBuilder() {
        return playlistBuilder()
                .name("Collaborative Study Session")
                .description("Shared playlist for our study group")
                .type(Playlist.PlaylistType.COLLABORATIVE)
                .isPublic(true);
    }

    public static PlaylistTrack.PlaylistTrackBuilder playlistTrackBuilder() {
        return PlaylistTrack.builder()
                .id(UUID.randomUUID())
                .spotifyTrackId("4iV5W9uYEdYUVa79Axb7Rh")
                .title("Never Gonna Give You Up")
                .artist("Rick Astley")
                .album("Whenever You Need Somebody")
                .durationMs(213573)
                .order(1)
                .addedBy("test-user")
                .createdAt(LocalDateTime.now());
    }

    // OAuth Constants
    public static final String OAUTH_AUTHORIZATION_CODE = "AQAmock_authorization_code_here";
    public static final String OAUTH_STATE = "test-state-12345";
    public static final String OAUTH_REDIRECT_URI = "http://localhost:3000/auth/spotify/callback";

    // Test User Data
    public static final String TEST_USER_ID = "test-user-123";
    public static final String TEST_SPOTIFY_USER_ID = "testuser123";
    public static final String TEST_HIVE_ID = "test-hive-456";

    // Spotify Track IDs (real ones for realistic testing)
    public static final String[] REAL_SPOTIFY_TRACK_IDS = {
        "4iV5W9uYEdYUVa79Axb7Rh", // Rick Astley - Never Gonna Give You Up
        "5FVd6KXrgO9B3JPmC8OPst", // Queen - Bohemian Rhapsody
        "6lPb7Eoon6QPbscWbMsk6a", // Ludovico Einaudi - Nuvole Bianche
        "0c6xIDDpzE81m2q797ordA", // Max Richter - On The Nature Of Daylight
    };

    // WebSocket Test Messages
    public static final String WEBSOCKET_TRACK_ADDED_MESSAGE = """
        {
            "type": "TRACK_ADDED",
            "playlistId": "test-playlist-id",
            "track": {
                "spotifyTrackId": "4iV5W9uYEdYUVa79Axb7Rh",
                "trackName": "Never Gonna Give You Up",
                "artistName": "Rick Astley",
                "albumName": "Whenever You Need Somebody",
                "durationMs": 213573,
                "position": 1
            },
            "addedBy": "test-user-123",
            "timestamp": "2024-09-13T10:30:00Z"
        }
        """;

    public static final String WEBSOCKET_TRACK_REMOVED_MESSAGE = """
        {
            "type": "TRACK_REMOVED",
            "playlistId": "test-playlist-id",
            "trackId": "track-uuid-123",
            "removedBy": "test-user-456",
            "timestamp": "2024-09-13T10:35:00Z"
        }
        """;
}