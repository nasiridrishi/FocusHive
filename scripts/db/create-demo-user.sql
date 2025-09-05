-- Create demo user for testing
-- Password: Demo123! (BCrypt hashed)
-- BCrypt hash for "Demo123!" = $2a$10$N9qo8uLOickgx2ZMRZoJGea3FjVPDQoNH7M0sMfAGTZ0e3YsPZ/9K

INSERT INTO users (
    id,
    username,
    email,
    password,
    display_name,
    avatar_url,
    bio,
    timezone,
    locale,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    email_verified,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'demo_user',
    'demo@focushive.com',
    '$2a$10$bGx1Y7LbI7oZg7qhj8VZF.JVyHrX1St7bV0eQ3D5X.mZrDqNwJmNa', -- BCrypt hash of "Demo123!"
    'Demo User',
    'https://ui-avatars.com/api/?name=Demo+User&background=6366f1&color=fff',
    'This is a demo account for testing FocusHive features',
    'UTC',
    'en-US',
    'USER',
    true,
    true,
    true,
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

-- Also create with alternate email in case the first one exists
INSERT INTO users (
    id,
    username,
    email,
    password,
    display_name,
    avatar_url,
    bio,
    timezone,
    locale,
    role,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    email_verified,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'test_user',
    'test@focushive.com',
    '$2a$10$bGx1Y7LbI7oZg7qhj8VZF.JVyHrX1St7bV0eQ3D5X.mZrDqNwJmNa', -- BCrypt hash of "Demo123!"
    'Test User',
    'https://ui-avatars.com/api/?name=Test+User&background=10b981&color=fff',
    'This is a test account for testing FocusHive features',
    'UTC',
    'en-US',
    'USER',
    true,
    true,
    true,
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;