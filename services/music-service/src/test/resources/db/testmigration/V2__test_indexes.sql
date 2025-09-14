-- V2__test_indexes.sql
-- Test-safe performance indexes (without CONCURRENTLY)

-- Essential composite indexes for tests
CREATE INDEX idx_playlists_user_type_focus ON playlists(user_id, type, focus_mode);
CREATE INDEX idx_playlists_activity_tracking ON playlists(user_id, updated_at DESC);
CREATE INDEX idx_playlist_tracks_ordering ON playlist_tracks(playlist_id, track_order ASC);
CREATE INDEX idx_playlist_tracks_spotify_lookup ON playlist_tracks(spotify_track_id, playlist_id);
CREATE INDEX idx_music_preferences_energy_tempo ON music_preferences(user_id, energy_level, tempo_preference);
CREATE INDEX idx_spotify_credentials_expiry ON spotify_credentials(expires_at, user_id) WHERE is_active = TRUE;