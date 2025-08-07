-- V3__create_recommendation_tables.sql
-- Creates tables for the enhanced music recommendation system

-- Recommendation history table
CREATE TABLE music.recommendation_history (
    recommendation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    session_id UUID,
    hive_id UUID,
    task_type VARCHAR(50),
    mood VARCHAR(50),
    algorithm_version VARCHAR(20) DEFAULT 'v2.1.0',
    total_tracks INTEGER,
    accepted_tracks INTEGER DEFAULT 0,
    rejected_tracks INTEGER DEFAULT 0,
    acceptance_rate DECIMAL(5,3),
    average_rating DECIMAL(3,2),
    diversity_score DECIMAL(3,2),
    novelty_score DECIMAL(3,2),
    serendipity_score DECIMAL(3,2),
    productivity_score DECIMAL(3,2),
    focus_score DECIMAL(3,2),
    generation_time_ms BIGINT,
    served_from_cache BOOLEAN DEFAULT false,
    cache_key VARCHAR(200),
    feedback_count INTEGER DEFAULT 0,
    avg_completion_rate DECIMAL(3,2),
    avg_skip_time_seconds INTEGER,
    above_average_performance BOOLEAN,
    ab_test_variant VARCHAR(50),
    
    -- Context information
    context_time_of_day TIMESTAMP,
    context_expected_duration INTEGER,
    context_environment VARCHAR(50),
    context_device_type VARCHAR(50),
    context_collaborative BOOLEAN DEFAULT false,
    context_hive_size INTEGER,
    context_user_energy_level INTEGER,
    
    -- Performance metrics
    metrics_confidence_score DECIMAL(3,2),
    metrics_predicted_satisfaction DECIMAL(3,2),
    metrics_actual_satisfaction DECIMAL(3,2),
    metrics_engagement_score DECIMAL(3,2),
    metrics_retention_rate DECIMAL(3,2),
    metrics_surprise_factor DECIMAL(3,2),
    metrics_familiarity_score DECIMAL(3,2),
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT valid_acceptance_rate CHECK (acceptance_rate >= 0 AND acceptance_rate <= 1),
    CONSTRAINT valid_rating CHECK (average_rating >= 1 AND average_rating <= 5),
    CONSTRAINT valid_scores CHECK (
        diversity_score >= 0 AND diversity_score <= 1 AND
        novelty_score >= 0 AND novelty_score <= 1 AND
        serendipity_score >= 0 AND serendipity_score <= 1 AND
        productivity_score >= 0 AND productivity_score <= 1
    )
);

-- Recommendation track IDs (many-to-many relationship)
CREATE TABLE music.recommendation_track_ids (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    track_id VARCHAR(100) NOT NULL,
    position INTEGER,
    PRIMARY KEY (recommendation_id, track_id)
);

-- Recommendation genre distribution
CREATE TABLE music.recommendation_genre_distribution (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    genre VARCHAR(50) NOT NULL,
    count INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (recommendation_id, genre)
);

-- Recommendation data sources
CREATE TABLE music.recommendation_data_sources (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    data_source VARCHAR(50) NOT NULL,
    PRIMARY KEY (recommendation_id, data_source)
);

-- Recommendation seed artists
CREATE TABLE music.recommendation_seed_artists (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    artist_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (recommendation_id, artist_id)
);

-- Recommendation seed tracks
CREATE TABLE music.recommendation_seed_tracks (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    track_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (recommendation_id, track_id)
);

-- Recommendation seed genres
CREATE TABLE music.recommendation_seed_genres (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    genre VARCHAR(50) NOT NULL,
    PRIMARY KEY (recommendation_id, genre)
);

-- Recommendation metadata
CREATE TABLE music.recommendation_metadata (
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (recommendation_id, metadata_key)
);

-- Recommendation feedback table
CREATE TABLE music.recommendation_feedback (
    feedback_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    recommendation_id UUID NOT NULL REFERENCES music.recommendation_history(recommendation_id),
    recommendation_track_id UUID,
    track_id VARCHAR(100) NOT NULL,
    session_id UUID,
    feedback_type VARCHAR(50) NOT NULL,
    overall_rating INTEGER CHECK (overall_rating >= 1 AND overall_rating <= 5),
    liked BOOLEAN,
    interaction_type VARCHAR(50),
    productivity_impact INTEGER CHECK (productivity_impact >= 1 AND productivity_impact <= 10),
    focus_enhancement INTEGER CHECK (focus_enhancement >= 1 AND focus_enhancement <= 10),
    mood_appropriateness INTEGER CHECK (mood_appropriateness >= 1 AND mood_appropriateness <= 10),
    task_suitability INTEGER CHECK (task_suitability >= 1 AND task_suitability <= 10),
    negative_reason VARCHAR(100),
    skip_reason VARCHAR(100),
    feedback_text TEXT,
    feedback_at TIMESTAMP NOT NULL DEFAULT NOW(),
    influence_future BOOLEAN DEFAULT true,
    confidence_level INTEGER CHECK (confidence_level >= 1 AND confidence_level <= 5),
    
    -- Context information
    context_task_type VARCHAR(50),
    context_mood VARCHAR(50),
    context_time TIMESTAMP,
    context_session_duration INTEGER,
    context_environment VARCHAR(50),
    context_device VARCHAR(50),
    context_volume INTEGER CHECK (context_volume >= 0 AND context_volume <= 100),
    context_solo_listening BOOLEAN,
    context_collaborator_count INTEGER,
    
    -- Listening behavior
    listen_duration_seconds INTEGER,
    completion_percentage DECIMAL(3,2) CHECK (completion_percentage >= 0 AND completion_percentage <= 1),
    repeat_count INTEGER DEFAULT 0,
    time_to_skip_seconds INTEGER,
    volume_adjusted BOOLEAN DEFAULT false,
    paused_during_play BOOLEAN DEFAULT false,
    pause_count INTEGER DEFAULT 0,
    total_pause_duration_seconds INTEGER DEFAULT 0,
    user_seeked BOOLEAN DEFAULT false,
    seek_count INTEGER DEFAULT 0,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Feedback tags (many-to-many)
CREATE TABLE music.feedback_tags (
    feedback_id UUID NOT NULL REFERENCES music.recommendation_feedback(feedback_id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (feedback_id, tag)
);

-- Feedback metadata
CREATE TABLE music.feedback_metadata (
    feedback_id UUID NOT NULL REFERENCES music.recommendation_feedback(feedback_id) ON DELETE CASCADE,
    metadata_key VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500),
    PRIMARY KEY (feedback_id, metadata_key)
);

-- Volume change events
CREATE TABLE music.feedback_volume_changes (
    feedback_id UUID NOT NULL REFERENCES music.recommendation_feedback(feedback_id) ON DELETE CASCADE,
    time_in_track_seconds INTEGER NOT NULL,
    from_volume INTEGER CHECK (from_volume >= 0 AND from_volume <= 100),
    to_volume INTEGER CHECK (to_volume >= 0 AND to_volume <= 100),
    change_id SERIAL,
    PRIMARY KEY (feedback_id, change_id)
);

-- Seek events
CREATE TABLE music.feedback_seek_events (
    feedback_id UUID NOT NULL REFERENCES music.recommendation_feedback(feedback_id) ON DELETE CASCADE,
    from_time_seconds INTEGER NOT NULL,
    to_time_seconds INTEGER NOT NULL,
    seek_direction VARCHAR(10) CHECK (seek_direction IN ('FORWARD', 'BACKWARD')),
    event_id SERIAL,
    PRIMARY KEY (feedback_id, event_id)
);

-- Update music_sessions table to work with new system
ALTER TABLE music.music_sessions 
ADD COLUMN IF NOT EXISTS user_id_uuid UUID,
ADD COLUMN IF NOT EXISTS recommendation_context JSONB,
ADD COLUMN IF NOT EXISTS current_recommendation_id UUID REFERENCES music.recommendation_history(recommendation_id);

-- Create indexes for optimal query performance

-- Recommendation history indexes
CREATE INDEX idx_rec_history_user_id ON music.recommendation_history(user_id);
CREATE INDEX idx_rec_history_session_id ON music.recommendation_history(session_id);
CREATE INDEX idx_rec_history_hive_id ON music.recommendation_history(hive_id);
CREATE INDEX idx_rec_history_created_at ON music.recommendation_history(created_at);
CREATE INDEX idx_rec_history_task_type ON music.recommendation_history(task_type);
CREATE INDEX idx_rec_history_mood ON music.recommendation_history(mood);
CREATE INDEX idx_rec_history_rating ON music.recommendation_history(average_rating);
CREATE INDEX idx_rec_history_acceptance ON music.recommendation_history(acceptance_rate);
CREATE INDEX idx_rec_history_productivity ON music.recommendation_history(productivity_score);
CREATE INDEX idx_rec_history_performance ON music.recommendation_history(above_average_performance);

-- Feedback indexes
CREATE INDEX idx_rec_feedback_user_id ON music.recommendation_feedback(user_id);
CREATE INDEX idx_rec_feedback_track_id ON music.recommendation_feedback(track_id);
CREATE INDEX idx_rec_feedback_recommendation_id ON music.recommendation_feedback(recommendation_id);
CREATE INDEX idx_rec_feedback_created_at ON music.recommendation_feedback(feedback_at);
CREATE INDEX idx_rec_feedback_rating ON music.recommendation_feedback(overall_rating);
CREATE INDEX idx_rec_feedback_type ON music.recommendation_feedback(feedback_type);
CREATE INDEX idx_rec_feedback_interaction ON music.recommendation_feedback(interaction_type);
CREATE INDEX idx_rec_feedback_liked ON music.recommendation_feedback(liked);
CREATE INDEX idx_rec_feedback_task_mood ON music.recommendation_feedback(context_task_type, context_mood);

-- Track relationship indexes
CREATE INDEX idx_rec_track_ids_track ON music.recommendation_track_ids(track_id);
CREATE INDEX idx_rec_track_ids_position ON music.recommendation_track_ids(recommendation_id, position);

-- Genre and metadata indexes
CREATE INDEX idx_rec_genre_genre ON music.recommendation_genre_distribution(genre);
CREATE INDEX idx_rec_metadata_key ON music.recommendation_metadata(metadata_key);
CREATE INDEX idx_feedback_metadata_key ON music.feedback_metadata(metadata_key);

-- Composite indexes for common queries
CREATE INDEX idx_rec_history_user_task_created ON music.recommendation_history(user_id, task_type, created_at DESC);
CREATE INDEX idx_rec_history_user_mood_created ON music.recommendation_history(user_id, mood, created_at DESC);
CREATE INDEX idx_rec_history_performance_metrics ON music.recommendation_history(user_id, average_rating, acceptance_rate);
CREATE INDEX idx_feedback_user_track_time ON music.recommendation_feedback(user_id, track_id, feedback_at DESC);
CREATE INDEX idx_feedback_behavior_patterns ON music.recommendation_feedback(user_id, context_task_type, completion_percentage);

-- Partial indexes for specific conditions
CREATE INDEX idx_rec_history_successful ON music.recommendation_history(user_id, created_at DESC) 
    WHERE average_rating >= 4.0 OR acceptance_rate >= 0.7 OR productivity_score >= 0.8;
CREATE INDEX idx_feedback_positive ON music.recommendation_feedback(user_id, track_id) 
    WHERE liked = true OR overall_rating >= 4;
CREATE INDEX idx_feedback_negative ON music.recommendation_feedback(user_id, track_id) 
    WHERE liked = false OR overall_rating <= 2;

-- Function to update recommendation history acceptance rate
CREATE OR REPLACE FUNCTION music.update_recommendation_acceptance_rate()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE music.recommendation_history 
    SET 
        feedback_count = (
            SELECT COUNT(*) 
            FROM music.recommendation_feedback 
            WHERE recommendation_id = NEW.recommendation_id
        ),
        accepted_tracks = (
            SELECT COUNT(*) 
            FROM music.recommendation_feedback 
            WHERE recommendation_id = NEW.recommendation_id 
            AND (liked = true OR overall_rating >= 4)
        ),
        rejected_tracks = (
            SELECT COUNT(*) 
            FROM music.recommendation_feedback 
            WHERE recommendation_id = NEW.recommendation_id 
            AND (liked = false OR overall_rating <= 2)
        ),
        updated_at = NOW()
    WHERE recommendation_id = NEW.recommendation_id;
    
    -- Update acceptance rate
    UPDATE music.recommendation_history 
    SET acceptance_rate = CASE 
        WHEN total_tracks > 0 THEN CAST(accepted_tracks AS DECIMAL) / total_tracks
        ELSE NULL
    END
    WHERE recommendation_id = NEW.recommendation_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update acceptance rates
CREATE TRIGGER trigger_update_acceptance_rate
    AFTER INSERT OR UPDATE OR DELETE ON music.recommendation_feedback
    FOR EACH ROW 
    EXECUTE FUNCTION music.update_recommendation_acceptance_rate();

-- Function to calculate recommendation performance metrics
CREATE OR REPLACE FUNCTION music.calculate_recommendation_metrics(rec_id UUID)
RETURNS VOID AS $$
DECLARE
    avg_rating DECIMAL(3,2);
    avg_productivity DECIMAL(3,2);
    avg_focus DECIMAL(3,2);
    completion_rate DECIMAL(3,2);
BEGIN
    -- Calculate average ratings and scores from feedback
    SELECT 
        AVG(overall_rating),
        AVG(productivity_impact),
        AVG(focus_enhancement),
        AVG(completion_percentage)
    INTO avg_rating, avg_productivity, avg_focus, completion_rate
    FROM music.recommendation_feedback
    WHERE recommendation_id = rec_id;
    
    -- Update recommendation history with calculated metrics
    UPDATE music.recommendation_history
    SET 
        average_rating = avg_rating,
        productivity_score = CASE WHEN avg_productivity IS NOT NULL THEN avg_productivity / 10.0 ELSE NULL END,
        focus_score = CASE WHEN avg_focus IS NOT NULL THEN avg_focus / 10.0 ELSE NULL END,
        avg_completion_rate = completion_rate,
        above_average_performance = (
            avg_rating >= 4.0 OR 
            (avg_productivity / 10.0) >= 0.7 OR 
            completion_rate >= 0.8
        ),
        updated_at = NOW()
    WHERE recommendation_id = rec_id;
END;
$$ LANGUAGE plpgsql;

-- Views for common analytics queries

-- User recommendation performance summary
CREATE VIEW music.user_recommendation_summary AS
SELECT 
    user_id,
    COUNT(*) as total_recommendations,
    AVG(average_rating) as avg_rating,
    AVG(acceptance_rate) as avg_acceptance_rate,
    AVG(productivity_score) as avg_productivity_score,
    AVG(diversity_score) as avg_diversity_score,
    COUNT(*) FILTER (WHERE above_average_performance = true) as successful_recommendations,
    ROUND(COUNT(*) FILTER (WHERE above_average_performance = true) * 100.0 / COUNT(*), 2) as success_rate
FROM music.recommendation_history
GROUP BY user_id;

-- Track recommendation performance
CREATE VIEW music.track_recommendation_performance AS
SELECT 
    rt.track_id,
    COUNT(*) as recommendation_count,
    AVG(CASE WHEN rf.overall_rating IS NOT NULL THEN rf.overall_rating ELSE NULL END) as avg_user_rating,
    COUNT(*) FILTER (WHERE rf.liked = true) as like_count,
    COUNT(*) FILTER (WHERE rf.liked = false) as dislike_count,
    AVG(rf.completion_percentage) as avg_completion_rate,
    AVG(rf.productivity_impact) as avg_productivity_impact
FROM music.recommendation_track_ids rt
LEFT JOIN music.recommendation_feedback rf ON rt.track_id = rf.track_id
GROUP BY rt.track_id;

-- Task type effectiveness
CREATE VIEW music.task_type_effectiveness AS
SELECT 
    task_type,
    mood,
    COUNT(*) as recommendation_count,
    AVG(average_rating) as avg_rating,
    AVG(acceptance_rate) as avg_acceptance_rate,
    AVG(productivity_score) as avg_productivity_score,
    COUNT(*) FILTER (WHERE above_average_performance = true) as successful_count
FROM music.recommendation_history
WHERE task_type IS NOT NULL
GROUP BY task_type, mood
ORDER BY avg_productivity_score DESC;

-- Comment on tables and important columns
COMMENT ON TABLE music.recommendation_history IS 'Stores comprehensive history of music recommendations with performance metrics';
COMMENT ON TABLE music.recommendation_feedback IS 'Captures user feedback on recommendations for machine learning and improvement';
COMMENT ON COLUMN music.recommendation_history.algorithm_version IS 'Version of recommendation algorithm used for A/B testing and performance tracking';
COMMENT ON COLUMN music.recommendation_history.acceptance_rate IS 'Ratio of positively received recommendations to total recommendations';
COMMENT ON COLUMN music.recommendation_feedback.completion_percentage IS 'Percentage of track completed by user (0.0 to 1.0)';
COMMENT ON COLUMN music.recommendation_feedback.influence_future IS 'Whether this feedback should influence future recommendations';