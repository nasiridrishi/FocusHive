-- Create music schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS music;

-- Set search path to music schema
SET search_path TO music;

-- Create user_music_preferences table
CREATE TABLE user_music_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    preferred_genres TEXT[],
    preferred_artists TEXT[],
    preferred_energy_level INTEGER CHECK (preferred_energy_level >= 1 AND preferred_energy_level <= 10),
    preferred_mood VARCHAR(50),
    spotify_connected BOOLEAN DEFAULT FALSE,
    spotify_access_token TEXT,
    spotify_refresh_token TEXT,
    spotify_expires_at TIMESTAMP,
    last_listening_activity TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_music_preferences_user_id UNIQUE (user_id)
);

-- Create playlists table
CREATE TABLE playlists (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(255) NOT NULL,
    hive_id VARCHAR(255), -- NULL for personal playlists, hive_id for hive playlists
    is_collaborative BOOLEAN DEFAULT FALSE,
    is_public BOOLEAN DEFAULT FALSE,
    spotify_playlist_id VARCHAR(255),
    total_tracks INTEGER DEFAULT 0,
    total_duration_ms BIGINT DEFAULT 0,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_playlists_created_by (created_by),
    INDEX idx_playlists_hive_id (hive_id),
    INDEX idx_playlists_collaborative (is_collaborative)
);

-- Create playlist_tracks table
CREATE TABLE playlist_tracks (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    spotify_track_id VARCHAR(255) NOT NULL,
    track_name VARCHAR(500) NOT NULL,
    artist_name VARCHAR(500) NOT NULL,
    album_name VARCHAR(500),
    duration_ms INTEGER NOT NULL,
    preview_url TEXT,
    external_url TEXT,
    image_url TEXT,
    position_in_playlist INTEGER NOT NULL,
    added_by VARCHAR(255) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    INDEX idx_playlist_tracks_playlist_id (playlist_id),
    INDEX idx_playlist_tracks_position (playlist_id, position_in_playlist)
);

-- Create music_sessions table
CREATE TABLE music_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    hive_id VARCHAR(255),
    session_type VARCHAR(50) NOT NULL, -- 'PERSONAL', 'COLLABORATIVE', 'SYNCHRONIZED'
    current_playlist_id BIGINT,
    current_track_position INTEGER DEFAULT 0,
    is_playing BOOLEAN DEFAULT FALSE,
    volume INTEGER CHECK (volume >= 0 AND volume <= 100) DEFAULT 50,
    shuffle_enabled BOOLEAN DEFAULT FALSE,
    repeat_mode VARCHAR(20) DEFAULT 'OFF', -- 'OFF', 'TRACK', 'PLAYLIST'
    session_start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_end_time TIMESTAMP,
    total_listening_time_ms BIGINT DEFAULT 0,
    
    FOREIGN KEY (current_playlist_id) REFERENCES playlists(id) ON DELETE SET NULL,
    INDEX idx_music_sessions_user_id (user_id),
    INDEX idx_music_sessions_hive_id (hive_id),
    INDEX idx_music_sessions_active (session_end_time) WHERE session_end_time IS NULL
);

-- Create streaming_credentials table
CREATE TABLE streaming_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    platform VARCHAR(50) NOT NULL, -- 'SPOTIFY', 'APPLE_MUSIC', etc.
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    scope TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_streaming_credentials_user_platform UNIQUE (user_id, platform),
    INDEX idx_streaming_credentials_user_id (user_id),
    INDEX idx_streaming_credentials_platform (platform)
);

-- Create recommendation_cache table
CREATE TABLE recommendation_cache (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    context_hash VARCHAR(255) NOT NULL, -- Hash of recommendation context (genres, mood, energy, etc.)
    recommendations JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_recommendation_cache_user_context UNIQUE (user_id, context_hash),
    INDEX idx_recommendation_cache_user_id (user_id),
    INDEX idx_recommendation_cache_expires_at (expires_at)
);

-- Create listening_history table
CREATE TABLE listening_history (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    spotify_track_id VARCHAR(255) NOT NULL,
    track_name VARCHAR(500) NOT NULL,
    artist_name VARCHAR(500) NOT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    play_duration_ms INTEGER,
    skip_reason VARCHAR(50), -- 'COMPLETED', 'SKIPPED', 'NEXT_TRACK', etc.
    context_type VARCHAR(50), -- 'PLAYLIST', 'RECOMMENDATION', 'SEARCH', etc.
    context_id VARCHAR(255), -- playlist_id, recommendation_id, etc.
    hive_id VARCHAR(255), -- For hive listening context
    
    INDEX idx_listening_history_user_id (user_id),
    INDEX idx_listening_history_played_at (played_at),
    INDEX idx_listening_history_track_id (spotify_track_id),
    INDEX idx_listening_history_hive_id (hive_id)
);

-- Create function to update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_user_music_preferences_updated_at 
    BEFORE UPDATE ON user_music_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_playlists_updated_at 
    BEFORE UPDATE ON playlists
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_streaming_credentials_updated_at 
    BEFORE UPDATE ON streaming_credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();