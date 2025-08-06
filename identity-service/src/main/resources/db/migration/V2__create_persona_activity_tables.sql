-- Create persona activities table
CREATE TABLE persona_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id UUID NOT NULL REFERENCES personas(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    activity_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    session_id UUID,
    duration_minutes INTEGER,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    productivity_score DOUBLE PRECISION,
    focus_score DOUBLE PRECISION,
    interruption_count INTEGER DEFAULT 0,
    context_location VARCHAR(100),
    device_type VARCHAR(50),
    ip_address VARCHAR(45),
    activity_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create persona activity tags table
CREATE TABLE persona_activity_tags (
    activity_id UUID NOT NULL REFERENCES persona_activities(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (activity_id, tag)
);

-- Create indexes for persona activities
CREATE INDEX idx_persona_activity_persona ON persona_activities(persona_id);
CREATE INDEX idx_persona_activity_type ON persona_activities(activity_type);
CREATE INDEX idx_persona_activity_timestamp ON persona_activities(activity_timestamp);
CREATE INDEX idx_persona_activity_session ON persona_activities(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_persona_activity_status ON persona_activities(status);
CREATE INDEX idx_persona_activity_composite ON persona_activities(persona_id, activity_type, activity_timestamp);

-- Create index for activity tags
CREATE INDEX idx_persona_activity_tags_tag ON persona_activity_tags(tag);

-- Add constraints
ALTER TABLE persona_activities 
ADD CONSTRAINT chk_persona_activity_status 
CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED'));

ALTER TABLE persona_activities 
ADD CONSTRAINT chk_persona_activity_duration_positive 
CHECK (duration_minutes IS NULL OR duration_minutes >= 0);

ALTER TABLE persona_activities 
ADD CONSTRAINT chk_persona_activity_scores 
CHECK (
    (productivity_score IS NULL OR (productivity_score >= 0 AND productivity_score <= 100)) AND
    (focus_score IS NULL OR (focus_score >= 0 AND focus_score <= 100))
);

-- Add trigger for updated_at if it doesn't exist for persona_activities
-- (We'll add an updated_at column if needed in future migrations)