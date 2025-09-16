-- Update hive_members table to add missing fields
ALTER TABLE hive_members
ADD COLUMN IF NOT EXISTS invited_by_user_id UUID REFERENCES users(id),
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INVITED', 'BANNED'));

-- Update existing records to have ACTIVE status
UPDATE hive_members SET status = 'ACTIVE' WHERE status IS NULL;

-- Make status NOT NULL after updating existing records
ALTER TABLE hive_members ALTER COLUMN status SET NOT NULL;

-- Drop existing hive_invitations table since it has different structure
DROP TABLE IF EXISTS hive_invitations CASCADE;

-- Create new hive_invitations table with proper structure
CREATE TABLE hive_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,
    invited_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitation_code VARCHAR(100) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'REVOKED')),
    responded_at TIMESTAMP WITH TIME ZONE,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0,
    UNIQUE(hive_id, invited_user_id, status)
);

-- Indexes for hive_invitations
CREATE INDEX idx_hive_invitations_code ON hive_invitations(invitation_code);
CREATE INDEX idx_hive_invitations_hive ON hive_invitations(hive_id);
CREATE INDEX idx_hive_invitations_invited_user ON hive_invitations(invited_user_id);
CREATE INDEX idx_hive_invitations_invited_by ON hive_invitations(invited_by_user_id);
CREATE INDEX idx_hive_invitations_status ON hive_invitations(status);
CREATE INDEX idx_hive_invitations_expires_at ON hive_invitations(expires_at);
CREATE INDEX idx_hive_invitations_pending ON hive_invitations(status, expires_at) WHERE status = 'PENDING';

-- Add trigger for updated_at
CREATE TRIGGER update_hive_invitations_updated_at BEFORE UPDATE ON hive_invitations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for new hive_members columns
CREATE INDEX idx_hive_members_invited_by ON hive_members(invited_by_user_id);
CREATE INDEX idx_hive_members_status ON hive_members(status);

-- Add comments for clarity
COMMENT ON TABLE hive_invitations IS 'Stores invitations sent to users to join hives';
COMMENT ON COLUMN hive_invitations.invitation_code IS 'Unique code used to accept the invitation';
COMMENT ON COLUMN hive_invitations.expires_at IS 'When this invitation expires';
COMMENT ON COLUMN hive_invitations.status IS 'Current status of the invitation';
COMMENT ON COLUMN hive_invitations.message IS 'Optional message from the inviter';

COMMENT ON COLUMN hive_members.invited_by_user_id IS 'User who invited this member (if applicable)';
COMMENT ON COLUMN hive_members.status IS 'Current status of the membership';