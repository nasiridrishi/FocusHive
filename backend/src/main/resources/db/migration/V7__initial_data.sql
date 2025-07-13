-- Insert default achievement types
INSERT INTO user_achievements (user_id, achievement_type, achievement_key, title, description, points) VALUES
-- Note: These are templates, actual user achievements will be created when unlocked
(gen_random_uuid(), 'SYSTEM', 'first_session', 'First Focus', 'Complete your first focus session', 10),
(gen_random_uuid(), 'SYSTEM', 'early_bird', 'Early Bird', 'Start a session before 6 AM', 20),
(gen_random_uuid(), 'SYSTEM', 'night_owl', 'Night Owl', 'Complete a session after midnight', 20),
(gen_random_uuid(), 'SYSTEM', 'streak_3', 'Consistent', 'Maintain a 3-day streak', 30),
(gen_random_uuid(), 'SYSTEM', 'streak_7', 'Dedicated', 'Maintain a 7-day streak', 70),
(gen_random_uuid(), 'SYSTEM', 'streak_30', 'Committed', 'Maintain a 30-day streak', 300),
(gen_random_uuid(), 'SYSTEM', 'deep_focus', 'Deep Focus', 'Complete a 2-hour session without breaks', 50),
(gen_random_uuid(), 'SYSTEM', 'multitasker', 'Multitasker', 'Complete 10 sessions in one day', 100),
(gen_random_uuid(), 'SYSTEM', 'social_butterfly', 'Social Butterfly', 'Join 5 different hives', 40),
(gen_random_uuid(), 'SYSTEM', 'hive_creator', 'Hive Creator', 'Create your first hive', 50),
(gen_random_uuid(), 'SYSTEM', 'buddy_up', 'Buddy Up', 'Connect with your first buddy', 30),
(gen_random_uuid(), 'SYSTEM', 'goal_setter', 'Goal Setter', 'Set and achieve 5 goals', 60),
(gen_random_uuid(), 'SYSTEM', 'zen_master', 'Zen Master', 'Complete 50 meditation sessions', 200),
(gen_random_uuid(), 'SYSTEM', 'productivity_pro', 'Productivity Pro', 'Achieve 90% productivity score for a week', 150);

-- Create indexes after bulk insert
CREATE INDEX idx_achievement_templates ON user_achievements(achievement_type) WHERE achievement_type = 'SYSTEM';

-- Create a function to copy achievement template to user
CREATE OR REPLACE FUNCTION create_user_achievement(
    p_user_id UUID,
    p_achievement_key VARCHAR(100)
)
RETURNS UUID AS $$
DECLARE
    v_achievement_id UUID;
BEGIN
    INSERT INTO user_achievements (user_id, achievement_type, achievement_key, title, description, points, icon_url)
    SELECT 
        p_user_id,
        'USER',
        achievement_key,
        title,
        description,
        points,
        icon_url
    FROM user_achievements
    WHERE achievement_type = 'SYSTEM' AND achievement_key = p_achievement_key
    LIMIT 1
    ON CONFLICT (user_id, achievement_key) DO NOTHING
    RETURNING id INTO v_achievement_id;
    
    RETURN v_achievement_id;
END;
$$ LANGUAGE plpgsql;

-- Create default admin user (password: admin123)
-- Note: Change this password immediately in production!
INSERT INTO users (id, email, username, password, display_name, role, email_verified, enabled)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'admin@focushive.com',
    'admin',
    '$2a$10$YK1XPZ3Z8N8v7TQxLVGHAu4P9dJ9FZpGcD3QZBqMtIBR7L4QFZfHe',
    'System Administrator',
    'ADMIN',
    TRUE,
    TRUE
) ON CONFLICT (email) DO NOTHING;

-- Create admin profile
INSERT INTO user_profiles (user_id, notification_preferences, privacy_settings)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    '{"email": false, "push": false, "sms": false}'::jsonb,
    '{"profile_visible": false, "stats_visible": false, "online_status_visible": false}'::jsonb
) ON CONFLICT (user_id) DO NOTHING;