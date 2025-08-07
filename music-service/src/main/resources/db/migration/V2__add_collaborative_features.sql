-- Set search path to music schema
SET search_path TO music;

-- Create playlist_queue table for collaborative playlists
CREATE TABLE playlist_queue (
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
    requested_by VARCHAR(255) NOT NULL,
    queue_position INTEGER NOT NULL,
    votes_count INTEGER DEFAULT 0,
    is_processed BOOLEAN DEFAULT FALSE,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    INDEX idx_playlist_queue_playlist_id (playlist_id),
    INDEX idx_playlist_queue_position (playlist_id, queue_position),
    INDEX idx_playlist_queue_unprocessed (playlist_id, is_processed) WHERE is_processed = FALSE
);

-- Create playlist_votes table for voting on queued tracks
CREATE TABLE playlist_votes (
    id BIGSERIAL PRIMARY KEY,
    queue_item_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    vote_type VARCHAR(10) NOT NULL CHECK (vote_type IN ('UP', 'DOWN')),
    voted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (queue_item_id) REFERENCES playlist_queue(id) ON DELETE CASCADE,
    CONSTRAINT uk_playlist_votes_queue_user UNIQUE (queue_item_id, user_id),
    INDEX idx_playlist_votes_queue_item (queue_item_id),
    INDEX idx_playlist_votes_user_id (user_id)
);

-- Create hive_music_settings table
CREATE TABLE hive_music_settings (
    id BIGSERIAL PRIMARY KEY,
    hive_id VARCHAR(255) NOT NULL,
    allow_music_sharing BOOLEAN DEFAULT TRUE,
    allow_collaborative_playlists BOOLEAN DEFAULT TRUE,
    music_voting_enabled BOOLEAN DEFAULT TRUE,
    auto_add_threshold INTEGER DEFAULT 3, -- Minimum votes to auto-add to playlist
    queue_max_size INTEGER DEFAULT 50,
    daily_music_time_limit_minutes INTEGER, -- NULL for unlimited
    allowed_genres TEXT[], -- NULL for all genres allowed
    blocked_explicit_content BOOLEAN DEFAULT FALSE,
    music_session_sync_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_hive_music_settings_hive_id UNIQUE (hive_id),
    INDEX idx_hive_music_settings_hive_id (hive_id)
);

-- Create playlist_collaborators table
CREATE TABLE playlist_collaborators (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    permission_level VARCHAR(20) NOT NULL DEFAULT 'CONTRIBUTOR', -- 'OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'
    can_add_tracks BOOLEAN DEFAULT TRUE,
    can_remove_tracks BOOLEAN DEFAULT FALSE,
    can_reorder_tracks BOOLEAN DEFAULT FALSE,
    can_edit_playlist BOOLEAN DEFAULT FALSE,
    can_invite_others BOOLEAN DEFAULT FALSE,
    added_by VARCHAR(255) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    CONSTRAINT uk_playlist_collaborators_playlist_user UNIQUE (playlist_id, user_id),
    INDEX idx_playlist_collaborators_playlist_id (playlist_id),
    INDEX idx_playlist_collaborators_user_id (user_id)
);

-- Create music_activity_log table for tracking music-related activities
CREATE TABLE music_activity_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    hive_id VARCHAR(255),
    activity_type VARCHAR(50) NOT NULL, -- 'TRACK_ADDED', 'TRACK_REMOVED', 'PLAYLIST_CREATED', 'VOTE_CAST', etc.
    activity_description TEXT NOT NULL,
    entity_type VARCHAR(50), -- 'PLAYLIST', 'TRACK', 'VOTE', etc.
    entity_id VARCHAR(255),
    metadata JSONB, -- Additional context data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_music_activity_log_user_id (user_id),
    INDEX idx_music_activity_log_hive_id (hive_id),
    INDEX idx_music_activity_log_activity_type (activity_type),
    INDEX idx_music_activity_log_created_at (created_at)
);

-- Create synchronized_sessions table for real-time music synchronization
CREATE TABLE synchronized_sessions (
    id BIGSERIAL PRIMARY KEY,
    hive_id VARCHAR(255) NOT NULL,
    session_name VARCHAR(255),
    host_user_id VARCHAR(255) NOT NULL,
    current_playlist_id BIGINT,
    current_track_spotify_id VARCHAR(255),
    current_track_position_ms BIGINT DEFAULT 0,
    is_playing BOOLEAN DEFAULT FALSE,
    playback_rate DECIMAL(3,2) DEFAULT 1.00,
    synchronized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    session_ended_at TIMESTAMP,
    max_participants INTEGER DEFAULT 50,
    participant_count INTEGER DEFAULT 0,
    
    FOREIGN KEY (current_playlist_id) REFERENCES playlists(id) ON DELETE SET NULL,
    INDEX idx_synchronized_sessions_hive_id (hive_id),
    INDEX idx_synchronized_sessions_host (host_user_id),
    INDEX idx_synchronized_sessions_active (session_ended_at) WHERE session_ended_at IS NULL
);

-- Create synchronized_session_participants table
CREATE TABLE synchronized_session_participants (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    is_synchronized BOOLEAN DEFAULT TRUE,
    last_sync_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    connection_status VARCHAR(20) DEFAULT 'CONNECTED', -- 'CONNECTED', 'DISCONNECTED', 'SYNCING'
    
    FOREIGN KEY (session_id) REFERENCES synchronized_sessions(id) ON DELETE CASCADE,
    CONSTRAINT uk_sync_session_participants_session_user UNIQUE (session_id, user_id),
    INDEX idx_sync_session_participants_session_id (session_id),
    INDEX idx_sync_session_participants_user_id (user_id),
    INDEX idx_sync_session_participants_active (session_id, left_at) WHERE left_at IS NULL
);

-- Add trigger for hive_music_settings updated_at
CREATE TRIGGER update_hive_music_settings_updated_at 
    BEFORE UPDATE ON hive_music_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to update vote counts
CREATE OR REPLACE FUNCTION update_playlist_queue_votes()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE playlist_queue 
    SET votes_count = (
        SELECT COUNT(*) FILTER (WHERE vote_type = 'UP') - COUNT(*) FILTER (WHERE vote_type = 'DOWN')
        FROM playlist_votes 
        WHERE queue_item_id = NEW.queue_item_id
    )
    WHERE id = NEW.queue_item_id;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update vote counts
CREATE TRIGGER update_playlist_queue_votes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON playlist_votes
    FOR EACH ROW EXECUTE FUNCTION update_playlist_queue_votes();

-- Create function to update synchronized session participant count
CREATE OR REPLACE FUNCTION update_session_participant_count()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE synchronized_sessions 
    SET participant_count = (
        SELECT COUNT(*) 
        FROM synchronized_session_participants 
        WHERE session_id = COALESCE(NEW.session_id, OLD.session_id) AND left_at IS NULL
    )
    WHERE id = COALESCE(NEW.session_id, OLD.session_id);
    
    RETURN COALESCE(NEW, OLD);
END;
$$ language 'plpgsql';

-- Create trigger to automatically update participant count
CREATE TRIGGER update_session_participant_count_trigger
    AFTER INSERT OR UPDATE OR DELETE ON synchronized_session_participants
    FOR EACH ROW EXECUTE FUNCTION update_session_participant_count();