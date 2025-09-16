-- V3: Allow NULL partnership_id for individual goals
-- The shared_goals table is used for both shared and individual goals
-- Individual goals should have partnership_id as NULL

-- Drop the NOT NULL constraint on partnership_id
ALTER TABLE shared_goals
ALTER COLUMN partnership_id DROP NOT NULL;

-- Add a comment to clarify the column usage
COMMENT ON COLUMN shared_goals.partnership_id IS 'Reference to buddy_partnerships. NULL for individual goals, required for shared goals';

-- Add an index for individual goals (where partnership_id is NULL)
CREATE INDEX idx_shared_goals_individual
ON shared_goals(created_by, created_at DESC)
WHERE partnership_id IS NULL;

-- Add an index for shared goals (where partnership_id is NOT NULL)
CREATE INDEX idx_shared_goals_shared
ON shared_goals(partnership_id, created_at DESC)
WHERE partnership_id IS NOT NULL;