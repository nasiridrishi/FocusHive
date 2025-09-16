-- Optimized Indexes for Array Queries
-- Performance optimization for PostgreSQL array operations in buddy-service

-- ==============================================================================
-- GIN INDEXES FOR ARRAY OPERATIONS
-- ==============================================================================

-- Optimize array overlap operations for buddy_preferences focus_areas
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_focus_areas_gin
ON buddy_preferences USING GIN (focus_areas);

-- Optimize array overlap operations for buddy_preferences goals
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_goals_gin
ON buddy_preferences USING GIN (goals);

-- Optimize array overlap operations for buddy_users interests
CREATE INDEX IF NOT EXISTS idx_buddy_users_interests_gin
ON buddy_users USING GIN (interests);

-- Optimize array overlap operations for buddy_users preferred_focus_times
CREATE INDEX IF NOT EXISTS idx_buddy_users_focus_times_gin
ON buddy_users USING GIN (preferred_focus_times);

-- ==============================================================================
-- COMPOSITE INDEXES FOR MATCHING QUERIES
-- ==============================================================================

-- Optimize the findPotentialMatches query
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_matching_active
ON buddy_preferences (matching_enabled, last_active_at DESC)
WHERE matching_enabled = true;

-- Optimize timezone-based matching
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_timezone_matching
ON buddy_preferences (matching_enabled, preferred_timezone, timezone_flexibility)
WHERE matching_enabled = true;

-- Optimize user compatibility queries
CREATE INDEX IF NOT EXISTS idx_buddy_users_active_compatibility
ON buddy_users (active, timezone, experience_level, communication_style, last_seen_at DESC)
WHERE active = true;

-- ==============================================================================
-- INDEXES FOR PARTNERSHIP CAPACITY CALCULATIONS
-- ==============================================================================

-- Optimize partnership counting for capacity checks
CREATE INDEX IF NOT EXISTS idx_partnerships_active_users
ON buddy_partnerships (status, user1_id, user2_id)
WHERE status = 'ACTIVE';

-- Optimize partnership queries by individual users
CREATE INDEX IF NOT EXISTS idx_partnerships_user1_status
ON buddy_partnerships (user1_id, status);

CREATE INDEX IF NOT EXISTS idx_partnerships_user2_status
ON buddy_partnerships (user2_id, status);

-- ==============================================================================
-- EXPRESSION INDEXES FOR COMPLEX QUERIES
-- ==============================================================================

-- Optimize queries that check both users in partnerships
CREATE INDEX IF NOT EXISTS idx_partnerships_combined_users
ON buddy_partnerships ((LEAST(user1_id, user2_id)), (GREATEST(user1_id, user2_id)), status);

-- ==============================================================================
-- PARTIAL INDEXES FOR FREQUENTLY FILTERED DATA
-- ==============================================================================

-- Index only active, matching-enabled preferences
CREATE INDEX IF NOT EXISTS idx_buddy_preferences_active_recent
ON buddy_preferences (user_id, last_active_at DESC, timezone_flexibility, max_partners)
WHERE matching_enabled = true;

-- Index only active users
CREATE INDEX IF NOT EXISTS idx_buddy_users_active_recent
ON buddy_users (id, timezone, experience_level, last_seen_at DESC)
WHERE active = true;

-- ==============================================================================
-- INDEX PURPOSE DOCUMENTATION
-- ==============================================================================
-- idx_buddy_preferences_focus_areas_gin: GIN index for fast array overlap operations on focus_areas
-- idx_buddy_preferences_goals_gin: GIN index for fast array overlap operations on goals
-- idx_buddy_users_interests_gin: GIN index for fast array overlap operations on interests
-- idx_buddy_preferences_matching_active: Composite index for matching-enabled users with activity ordering
-- idx_partnerships_combined_users: Expression index for efficient partnership existence checks