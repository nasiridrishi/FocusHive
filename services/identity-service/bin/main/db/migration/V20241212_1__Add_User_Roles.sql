-- Add role support to users table
-- Migration: V20241212_1__Add_User_Roles.sql

-- Add primary role column
ALTER TABLE users 
ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'USER';

-- Create index for role column for performance
CREATE INDEX idx_users_role ON users(role);

-- Create table for additional roles (many-to-many relationship)
CREATE TABLE user_additional_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_additional_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_additional_roles PRIMARY KEY (user_id, role)
);

-- Create index for performance on additional roles
CREATE INDEX idx_user_additional_roles_user_id ON user_additional_roles(user_id);
CREATE INDEX idx_user_additional_roles_role ON user_additional_roles(role);

-- Add check constraints to ensure valid role values
ALTER TABLE users 
ADD CONSTRAINT chk_users_role 
CHECK (role IN ('USER', 'PREMIUM_USER', 'HIVE_OWNER', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN'));

ALTER TABLE user_additional_roles 
ADD CONSTRAINT chk_user_additional_roles_role 
CHECK (role IN ('USER', 'PREMIUM_USER', 'HIVE_OWNER', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN'));

-- Update existing users to have USER role (this is the default anyway)
UPDATE users SET role = 'USER' WHERE role IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN users.role IS 'Primary role for the user (hierarchical)';
COMMENT ON TABLE user_additional_roles IS 'Additional roles that can be assigned to users';
COMMENT ON COLUMN user_additional_roles.user_id IS 'Reference to the user';
COMMENT ON COLUMN user_additional_roles.role IS 'Additional role assigned to the user';