import React, { useState } from 'react'
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  Box,
  Alert,
} from '@mui/material'
import {
  PersonAdd as PersonAddIcon,
  Lock as LockIcon,
  Public as PublicIcon,
} from '@mui/icons-material'
import { Hive } from '@shared/types'
import { LoadingButton } from '@shared/components/loading'

interface JoinHiveButtonProps {
  hive: Hive
  onJoin?: (hiveId: string, message?: string) => void
  isLoading?: boolean
  variant?: 'text' | 'outlined' | 'contained'
  size?: 'small' | 'medium' | 'large'
  fullWidth?: boolean
}

export const JoinHiveButton: React.FC<JoinHiveButtonProps> = ({
  hive,
  onJoin,
  isLoading = false,
  variant = 'contained',
  size = 'medium',
  fullWidth = false,
}) => {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [joining, setJoining] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const requiresApproval = hive.settings.requireApproval
  const isFull = hive.currentMembers >= hive.maxMembers

  const handleJoinClick = () => {
    if (requiresApproval) {
      setDialogOpen(true)
    } else {
      handleJoin()
    }
  }

  const handleJoin = async (joinMessage?: string) => {
    setJoining(true)
    setError(null)
    
    try {
      await onJoin?.(hive.id, joinMessage)
      setDialogOpen(false)
      setMessage('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to join hive')
    } finally {
      setJoining(false)
    }
  }

  const handleDialogClose = () => {
    if (!joining) {
      setDialogOpen(false)
      setMessage('')
      setError(null)
    }
  }

  const handleDialogSubmit = () => {
    handleJoin(message.trim() || undefined)
  }

  const getButtonText = () => {
    if (isFull) return 'Full'
    if (requiresApproval) return 'Request to Join'
    return 'Join Hive'
  }

  const getButtonIcon = () => {
    if (hive.isPublic) {
      return <PublicIcon />
    }
    return requiresApproval ? <LockIcon /> : <PersonAddIcon />
  }

  return (
    <>
      <LoadingButton
        variant={variant}
        size={size}
        fullWidth={fullWidth}
        loading={joining || isLoading}
        startIcon={getButtonIcon()}
        onClick={handleJoinClick}
        disabled={isFull}
        color={requiresApproval ? 'warning' : 'primary'}
        sx={{
          minWidth: fullWidth ? undefined : 120,
          opacity: isFull ? 0.5 : 1,
        }}
      >
        {getButtonText()}
      </LoadingButton>

      {/* Join Request Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={handleDialogClose}
        maxWidth="tablet"
        fullWidth
        PaperProps={{
          sx: { borderRadius: 2 }
        }}
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <LockIcon color="warning" />
            Request to Join "{hive.name}"
          </Box>
        </DialogTitle>
        
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            This hive requires approval to join. Please introduce yourself and explain 
            why you'd like to join this community.
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <TextField
            autoFocus
            fullWidth
            multiline
            rows={4}
            label="Introduction message (optional)"
            placeholder="Hi! I'm interested in joining this hive because..."
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            disabled={joining}
            inputProps={{
              maxLength: 500,
            }}
            helperText={`${message.length}/500 characters`}
          />

          <Box sx={{ mt: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
            <Typography variant="caption" color="text.secondary">
              <strong>Note:</strong> Your request will be sent to the hive owner and moderators. 
              You'll be notified when your request is reviewed.
            </Typography>
          </Box>
        </DialogContent>
        
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button 
            onClick={handleDialogClose} 
            disabled={joining}
            color="inherit"
          >
            Cancel
          </Button>
          <LoadingButton
            onClick={handleDialogSubmit}
            variant="contained"
            loading={joining}
            loadingText="Sending Request..."
            startIcon={<PersonAddIcon />}
          >
            Send Request
          </LoadingButton>
        </DialogActions>
      </Dialog>
    </>
  )
}

export default JoinHiveButton