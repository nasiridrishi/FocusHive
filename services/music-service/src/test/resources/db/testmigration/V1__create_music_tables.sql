-- Music Service Database Schema

-- Playlists table
CREATE TABLE playlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'CUSTOM',
    focus_mode VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_public BOOLEAN DEFAULT FALSE,
    spotify_playlist_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Playlist tracks table
CREATE TABLE playlist_tracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id UUID NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    spotify_track_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255),
    duration_ms INTEGER,
    track_order INTEGER NOT NULL,
    added_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Music preferences table
CREATE TABLE music_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) UNIQUE NOT NULL,
    energy_level DECIMAL(3,2) DEFAULT 0.6,
    tempo_preference DECIMAL(6,2) DEFAULT 120.0,
    ambient_sounds_enabled BOOLEAN DEFAULT TRUE,
    auto_start_music BOOLEAN DEFAULT TRUE,
    default_volume DECIMAL(3,2) DEFAULT 0.5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User preferred genres table
CREATE TABLE user_preferred_genres (
    preference_id UUID NOT NULL REFERENCES music_preferences(id) ON DELETE CASCADE,
    genre VARCHAR(100) NOT NULL,
    PRIMARY KEY (preference_id, genre)
);

-- Spotify credentials table
CREATE TABLE spotify_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) UNIQUE NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    scope TEXT,
    spotify_user_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for better performance
CREATE INDEX idx_playlists_user_id ON playlists(user_id);
CREATE INDEX idx_playlists_focus_mode ON playlists(focus_mode);
CREATE INDEX idx_playlists_type ON playlists(type);
CREATE INDEX idx_playlists_public ON playlists(is_public);

CREATE INDEX idx_playlist_tracks_playlist_id ON playlist_tracks(playlist_id);
CREATE INDEX idx_playlist_tracks_order ON playlist_tracks(track_order);

CREATE INDEX idx_music_preferences_user_id ON music_preferences(user_id);

CREATE INDEX idx_spotify_credentials_user_id ON spotify_credentials(user_id);
CREATE INDEX idx_spotify_credentials_spotify_user_id ON spotify_credentials(spotify_user_id);
CREATE INDEX idx_spotify_credentials_active ON spotify_credentials(is_active);

-- Comments
COMMENT ON TABLE playlists IS 'User-created and system-generated playlists for different focus modes';
COMMENT ON TABLE playlist_tracks IS 'Individual tracks within playlists';
COMMENT ON TABLE music_preferences IS 'User music preferences and settings';
COMMENT ON TABLE spotify_credentials IS 'Encrypted Spotify OAuth credentials for users';

COMMENT ON COLUMN playlists.type IS 'Playlist type: CUSTOM, RECOMMENDED, AMBIENT, COLLABORATIVE';
COMMENT ON COLUMN playlists.focus_mode IS 'Focus mode: DEEP_WORK, CREATIVE, RELAXATION, STUDY, GENERAL';
COMMENT ON COLUMN music_preferences.energy_level IS 'Preferred energy level from 0.0 to 1.0';
COMMENT ON COLUMN music_preferences.tempo_preference IS 'Preferred tempo in BPM';
COMMENT ON COLUMN spotify_credentials.access_token IS 'Encrypted Spotify access token';
COMMENT ON COLUMN spotify_credentials.refresh_token IS 'Encrypted Spotify refresh token';