-- Add firstName and lastName columns to users table for privacy-focused model
-- Username serves as public display name, firstName/lastName are private

-- First, add the new columns if they don't exist
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(50),
ADD COLUMN IF NOT EXISTS last_name VARCHAR(50);

-- Migrate existing displayName data if it exists (split on space)
-- This is a safe migration that won't fail if displayName doesn't exist
DO $$
BEGIN
    -- Check if display_name column exists
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'display_name'
    ) THEN
        -- Update firstName and lastName from displayName
        UPDATE users
        SET 
            first_name = COALESCE(first_name, SPLIT_PART(display_name, ' ', 1)),
            last_name = COALESCE(last_name, 
                CASE 
                    WHEN POSITION(' ' IN display_name) > 0 
                    THEN SUBSTRING(display_name FROM POSITION(' ' IN display_name) + 1)
                    ELSE display_name -- Use display_name as last_name if no space
                END
            )
        WHERE display_name IS NOT NULL 
        AND display_name != '';
        
        -- Drop the display_name column as it's no longer needed
        ALTER TABLE users DROP COLUMN IF EXISTS display_name;
    END IF;
    
    -- Set default values for any remaining NULL values
    UPDATE users 
    SET first_name = COALESCE(first_name, username)
    WHERE first_name IS NULL;
    
    UPDATE users 
    SET last_name = COALESCE(last_name, 'User')
    WHERE last_name IS NULL;
END $$;

-- Add indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_first_name ON users(first_name);
CREATE INDEX IF NOT EXISTS idx_users_last_name ON users(last_name);

-- Update personas table to use username as displayName
-- The displayName in personas should reference the user's username
ALTER TABLE personas 
ALTER COLUMN display_name TYPE VARCHAR(100);

-- Update any existing personas to use username as displayName
UPDATE personas p
SET display_name = u.username
FROM users u
WHERE p.user_id = u.id
AND (p.display_name IS NULL OR p.display_name = '');