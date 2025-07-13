-- Create hives table
CREATE TABLE hives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) DEFAULT 'GENERAL' CHECK (type IN ('STUDY', 'WORK', 'CREATIVE', 'MEDITATION', 'GENERAL')),
    max_members INTEGER DEFAULT 10 CHECK (max_members > 0 AND max_members <= 100),
    is_public BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    background_image VARCHAR(500),
    theme_color VARCHAR(7) CHECK (theme_color ~ '^#[0-9A-Fa-f]{6}$'),
    rules TEXT,
    tags TEXT[],
    settings JSONB DEFAULT '{}'::jsonb,
    member_count INTEGER DEFAULT 0,
    total_focus_minutes BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0
);

-- Indexes for hives
CREATE INDEX idx_hives_owner ON hives(owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_public ON hives(is_public, is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_slug ON hives(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_type ON hives(type) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_tags ON hives USING GIN(tags) WHERE deleted_at IS NULL;
CREATE INDEX idx_hives_deleted_at ON hives(deleted_at);

CREATE TRIGGER update_hives_updated_at BEFORE UPDATE ON hives
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create hive_members table
CREATE TABLE hive_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'MODERATOR', 'MEMBER')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP WITH TIME ZONE,
    total_minutes INTEGER DEFAULT 0,
    consecutive_days INTEGER DEFAULT 0,
    is_muted BOOLEAN DEFAULT FALSE,
    notification_settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(hive_id, user_id)
);

-- Indexes for hive_members
CREATE INDEX idx_hive_members_user ON hive_members(user_id);
CREATE INDEX idx_hive_members_hive ON hive_members(hive_id);
CREATE INDEX idx_hive_members_active ON hive_members(hive_id, last_active_at);
CREATE INDEX idx_hive_members_role ON hive_members(hive_id, role);

CREATE TRIGGER update_hive_members_updated_at BEFORE UPDATE ON hive_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create hive_invitations table
CREATE TABLE hive_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    created_by_id UUID NOT NULL REFERENCES users(id),
    code VARCHAR(32) UNIQUE NOT NULL,
    max_uses INTEGER,
    uses_count INTEGER DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hive_invitations_code ON hive_invitations(code) 
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;
CREATE INDEX idx_hive_invitations_hive ON hive_invitations(hive_id);

-- Create hive_settings table for extended configuration
CREATE TABLE hive_settings (
    hive_id UUID PRIMARY KEY REFERENCES hives(id) ON DELETE CASCADE,
    allow_guests BOOLEAN DEFAULT FALSE,
    require_approval BOOLEAN DEFAULT FALSE,
    min_focus_duration INTEGER DEFAULT 25,
    max_break_duration INTEGER DEFAULT 15,
    daily_goal_minutes INTEGER DEFAULT 240,
    week_starts_on INTEGER DEFAULT 1 CHECK (week_starts_on >= 0 AND week_starts_on <= 6),
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    features JSONB DEFAULT '{"chat": true, "voice": false, "leaderboard": true, "achievements": true}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_hive_settings_updated_at BEFORE UPDATE ON hive_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to update member count
CREATE OR REPLACE FUNCTION update_hive_member_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE hives SET member_count = member_count + 1 WHERE id = NEW.hive_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE hives SET member_count = member_count - 1 WHERE id = OLD.hive_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_hive_member_count_trigger
AFTER INSERT OR DELETE ON hive_members
FOR EACH ROW EXECUTE FUNCTION update_hive_member_count();