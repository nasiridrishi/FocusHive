-- V2__additional_performance_indexes.sql  
-- Additional performance optimization indexes for Music Service
-- Enhances existing basic indexes with composite and time-based patterns

-- Playlist management composite indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_user_type_focus 
ON playlists(user_id, type, focus_mode, is_public);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_activity_tracking 
ON playlists(user_id, updated_at DESC, created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_collaborative 
ON playlists(is_public, focus_mode, created_at DESC) 
WHERE is_public = TRUE;

-- Playlist tracks performance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_ordering 
ON playlist_tracks(playlist_id, track_order ASC, created_at);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_user_lookup 
ON playlist_tracks(added_by, created_at DESC) 
WHERE added_by IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_spotify_lookup 
ON playlist_tracks(spotify_track_id, playlist_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_duration 
ON playlist_tracks(playlist_id, duration_ms) 
WHERE duration_ms IS NOT NULL;

-- Music preferences advanced indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_music_preferences_energy_tempo 
ON music_preferences(user_id, energy_level, tempo_preference);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_music_preferences_settings 
ON music_preferences(user_id, ambient_sounds_enabled, auto_start_music, default_volume);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_music_preferences_updated 
ON music_preferences(updated_at DESC, user_id) 
WHERE updated_at > created_at;

-- User preferred genres lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_preferred_genres_lookup 
ON user_preferred_genres(genre, preference_id);

-- Spotify credentials security and management indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_spotify_credentials_expiry 
ON spotify_credentials(expires_at, user_id) 
WHERE is_active = TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_spotify_credentials_refresh_needed 
ON spotify_credentials(user_id, expires_at, updated_at) 
WHERE is_active = TRUE AND expires_at < CURRENT_TIMESTAMP + INTERVAL '1 hour';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_spotify_credentials_security_audit 
ON spotify_credentials(user_id, updated_at DESC, is_active);

-- Performance indexes for playlist collaboration features
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_spotify_sync 
ON playlists(spotify_playlist_id, updated_at) 
WHERE spotify_playlist_id IS NOT NULL;

-- Indexes for music analytics and reporting
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_usage_analytics 
ON playlists(focus_mode, type, created_at, user_id) 
WHERE is_public = FALSE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_analytics 
ON playlist_tracks(created_at, playlist_id, added_by);

-- Time-based indexes for music activity tracking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_recent_activity 
ON playlists(user_id, updated_at DESC) 
WHERE updated_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_recent_additions 
ON playlist_tracks(playlist_id, created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

-- Composite indexes for common join patterns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_tracks_join 
ON playlist_tracks(playlist_id, track_order, title, artist);

-- Partial indexes for filtered queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_active_user 
ON playlists(user_id, name, focus_mode) 
WHERE is_public = FALSE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_spotify_credentials_active 
ON spotify_credentials(user_id, spotify_user_id, expires_at) 
WHERE is_active = TRUE;

-- Function-based indexes for search capabilities
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_name_search 
ON playlists USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_search 
ON playlist_tracks USING GIN(to_tsvector('english', title || ' ' || artist || ' ' || COALESCE(album, '')));

-- Cleanup and maintenance indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_spotify_credentials_cleanup 
ON spotify_credentials(expires_at, is_active) 
WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Indexes for bulk operations and data export
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_bulk_export 
ON playlists(user_id, created_at, type, focus_mode);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_music_preferences_bulk_export 
ON music_preferences(user_id, created_at, updated_at);

-- Analyze tables after creating indexes
ANALYZE playlists;
ANALYZE playlist_tracks;
ANALYZE music_preferences;
ANALYZE user_preferred_genres;
ANALYZE spotify_credentials;

-- Comments for index purposes
COMMENT ON INDEX idx_playlists_user_type_focus IS 'Optimizes playlist filtering by user, type, and focus mode';
COMMENT ON INDEX idx_playlist_tracks_ordering IS 'Accelerates playlist track ordering and playback queries';
COMMENT ON INDEX idx_spotify_credentials_expiry IS 'Supports automatic token refresh and cleanup';
COMMENT ON INDEX idx_playlists_name_search IS 'Enables full-text search across playlist names and descriptions';
COMMENT ON INDEX idx_playlist_tracks_search IS 'Enables full-text search across track metadata';
COMMENT ON INDEX idx_playlists_collaborative IS 'Optimizes discovery of public collaborative playlists';