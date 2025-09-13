-- ===================================================================
-- OAUTH2 CLIENT AND IDENTITY TEST DATA
-- ===================================================================

\c identity_test;

-- Create OAuth2 clients for testing
CREATE TABLE IF NOT EXISTS oauth2_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(255) UNIQUE NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    redirect_uris TEXT[] NOT NULL,
    scopes TEXT[] NOT NULL,
    grant_types TEXT[] NOT NULL,
    client_type VARCHAR(50) DEFAULT 'confidential',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed OAuth2 test clients
INSERT INTO oauth2_clients (client_id, client_secret, client_name, redirect_uris, scopes, grant_types, client_type) VALUES
-- Main FocusHive frontend client
('focushive-frontend', 'frontend_secret_for_testing', 'FocusHive Frontend App', 
 ARRAY['http://localhost:3000/auth/callback', 'http://localhost:5173/auth/callback'], 
 ARRAY['read', 'write', 'profile', 'hives'], 
 ARRAY['authorization_code', 'refresh_token'], 'public'),

-- Backend service client
('focushive-backend', 'backend_secret_for_testing', 'FocusHive Backend Service',
 ARRAY['http://localhost:8080/auth/callback'],
 ARRAY['read', 'write', 'admin', 'service'],
 ARRAY['client_credentials', 'authorization_code'], 'confidential'),

-- Music service client  
('music-service', 'music_secret_for_testing', 'Music Service',
 ARRAY['http://localhost:8082/auth/callback'],
 ARRAY['read', 'write', 'music'],
 ARRAY['client_credentials', 'authorization_code'], 'confidential'),

-- Test mobile app client
('focushive-mobile', 'mobile_secret_for_testing', 'FocusHive Mobile App',
 ARRAY['focushive://auth/callback'],
 ARRAY['read', 'write', 'profile', 'offline_access'],
 ARRAY['authorization_code', 'refresh_token'], 'public'),

-- Third-party integration test client
('test-integration', 'integration_secret_for_testing', 'Test Integration Client',
 ARRAY['http://localhost:9000/callback'],
 ARRAY['read', 'profile'],
 ARRAY['authorization_code'], 'confidential')
ON CONFLICT (client_id) DO UPDATE SET
    client_secret = EXCLUDED.client_secret,
    redirect_uris = EXCLUDED.redirect_uris,
    scopes = EXCLUDED.scopes,
    grant_types = EXCLUDED.grant_types;

-- Create user personas table
CREATE TABLE IF NOT EXISTS user_personas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    persona_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    is_default BOOLEAN DEFAULT false,
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, persona_name)
);

-- Seed user personas for testing different contexts
INSERT INTO user_personas (user_id, persona_name, display_name, description, settings, is_default) VALUES
-- Alice's personas
('11111111-1111-1111-1111-111111111111', 'student', 'Alice the Student', 'Computer Science studies', 
 '{"study_goals": ["algorithms", "databases"], "preferred_timer": "pomodoro", "focus_music": true}', true),
('11111111-1111-1111-1111-111111111111', 'researcher', 'Alice the Researcher', 'Academic research work',
 '{"research_areas": ["AI", "HCI"], "preferred_timer": "deep_work", "focus_music": false}', false),

-- Bob's personas
('22222222-2222-2222-2222-222222222222', 'developer', 'Bob the Developer', 'Software development work',
 '{"languages": ["Java", "TypeScript"], "preferred_timer": "flow_state", "ide_theme": "dark"}', true),
('22222222-2222-2222-2222-222222222222', 'mentor', 'Bob the Mentor', 'Mentoring junior developers',
 '{"mentoring_style": "collaborative", "preferred_timer": "flexible", "availability": "evenings"}', false),

-- Charlie's personas  
('33333333-3333-3333-3333-333333333333', 'writer', 'Charlie the Writer', 'Creative writing',
 '{"genres": ["sci-fi", "fantasy"], "word_goals": 500, "preferred_timer": "writing_sprint"}', true),
('33333333-3333-3333-3333-333333333333', 'editor', 'Charlie the Editor', 'Editing and proofreading',
 '{"editing_style": "detailed", "preferred_timer": "focused_review", "grammar_tools": true}', false),

-- Diana's personas
('44444444-4444-4444-4444-444444444444', 'manager', 'Diana the Manager', 'Project management',
 '{"management_style": "agile", "meeting_preferences": "short", "preferred_timer": "time_boxing"}', true),
('44444444-4444-4444-4444-444444444444', 'analyst', 'Diana the Analyst', 'Data analysis work',
 '{"tools": ["Python", "R", "Excel"], "preferred_timer": "deep_analysis", "visualization": true}', false),

-- Eve's personas
('55555555-5555-5555-5555-555555555555', 'designer', 'Eve the Designer', 'UI/UX design work',
 '{"design_tools": ["Figma", "Sketch"], "preferred_timer": "creative_flow", "inspiration_boards": true}', true),
('55555555-5555-5555-5555-555555555555', 'freelancer', 'Eve the Freelancer', 'Client project work',
 '{"client_types": ["startups", "agencies"], "preferred_timer": "billable_hours", "time_tracking": true}', false)
ON CONFLICT (user_id, persona_name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    settings = EXCLUDED.settings;

-- Create OAuth2 authorization codes for testing
CREATE TABLE IF NOT EXISTS oauth2_authorization_codes (
    code VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    redirect_uri VARCHAR(500) NOT NULL,
    scopes TEXT[] NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create some test authorization codes (these will expire quickly in real tests)
INSERT INTO oauth2_authorization_codes (code, client_id, user_id, redirect_uri, scopes, expires_at) VALUES
('test_auth_code_alice', 'focushive-frontend', '11111111-1111-1111-1111-111111111111', 
 'http://localhost:3000/auth/callback', ARRAY['read', 'write', 'profile'], 
 CURRENT_TIMESTAMP + INTERVAL '10 minutes'),
('test_auth_code_bob', 'focushive-frontend', '22222222-2222-2222-2222-222222222222',
 'http://localhost:3000/auth/callback', ARRAY['read', 'write', 'profile'],
 CURRENT_TIMESTAMP + INTERVAL '10 minutes')
ON CONFLICT (code) DO UPDATE SET expires_at = EXCLUDED.expires_at;

-- Create refresh tokens table
CREATE TABLE IF NOT EXISTS oauth2_refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    scopes TEXT[] NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create privacy settings for testing privacy controls
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    user_id UUID PRIMARY KEY,
    profile_visibility VARCHAR(50) DEFAULT 'public',
    activity_visibility VARCHAR(50) DEFAULT 'hive_members',
    show_online_status BOOLEAN DEFAULT true,
    show_focus_time BOOLEAN DEFAULT true,
    allow_buddy_requests BOOLEAN DEFAULT true,
    data_sharing_consent BOOLEAN DEFAULT false,
    marketing_emails BOOLEAN DEFAULT false,
    analytics_consent BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed privacy settings for test users
INSERT INTO user_privacy_settings (user_id, profile_visibility, activity_visibility, show_online_status, show_focus_time, allow_buddy_requests, data_sharing_consent) VALUES
('11111111-1111-1111-1111-111111111111', 'public', 'public', true, true, true, true),
('22222222-2222-2222-2222-222222222222', 'hive_members', 'hive_members', true, false, true, false),
('33333333-3333-3333-3333-333333333333', 'public', 'hive_members', true, true, false, true),
('44444444-4444-4444-4444-444444444444', 'private', 'private', false, false, false, false),
('55555555-5555-5555-5555-555555555555', 'public', 'public', true, true, true, true)
ON CONFLICT (user_id) DO UPDATE SET
    profile_visibility = EXCLUDED.profile_visibility,
    activity_visibility = EXCLUDED.activity_visibility,
    show_online_status = EXCLUDED.show_online_status,
    show_focus_time = EXCLUDED.show_focus_time,
    allow_buddy_requests = EXCLUDED.allow_buddy_requests,
    data_sharing_consent = EXCLUDED.data_sharing_consent,
    updated_at = CURRENT_TIMESTAMP;

-- Verify identity service data
SELECT 'OAuth2 clients: ' || COUNT(*) FROM oauth2_clients;
SELECT 'User personas: ' || COUNT(*) FROM user_personas;  
SELECT 'Authorization codes: ' || COUNT(*) FROM oauth2_authorization_codes;
SELECT 'Privacy settings: ' || COUNT(*) FROM user_privacy_settings;