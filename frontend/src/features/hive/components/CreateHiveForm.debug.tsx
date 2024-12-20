import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Typography,
  Alert,
} from '@mui/material'
import type { Breakpoint } from '@mui/material/styles'
import { Add as AddIcon } from '@mui/icons-material'
import { CreateHiveRequest } from '@shared/types'

interface CreateHiveFormDebugProps {
  open: boolean
  onClose: () => void
  onSubmit: (hive: CreateHiveRequest) => void
  isLoading?: boolean
  error?: string | null
}

export const CreateHiveFormDebug: React.FC<CreateHiveFormDebugProps> = ({
  open,
  onClose,
  onSubmit,
  isLoading = false,
  error = null,
}) => {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const handleSubmit = () => {
    const hiveData: CreateHiveRequest = {
      name: name || 'Test Hive',
      description: description || 'This is a test hive for debugging purposes.',
      maxMembers: 10,
      isPublic: true,
      tags: ['test'],
      settings: {
        privacyLevel: 'PUBLIC',
        category: 'STUDY',
        maxParticipants: 10,
        allowChat: true,
        allowVoice: false,
        requireApproval: false,
        focusMode: 'FREEFORM',
        defaultSessionLength: 25,
        maxSessionLength: 120,
      },
    }
    onSubmit(hiveData)
  }

  const isValid = name.trim().length >= 3 && description.trim().length >= 10

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth={'sm' as Breakpoint}
      fullWidth
    >
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AddIcon />
          Create New Hive (Debug)
        </Box>
      </DialogTitle>

      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, pt: 1 }}>
          <TextField
            autoFocus
            label="Hive Name"
            placeholder="Enter hive name..."
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            required
            helperText={`${name.length}/50 characters (min 3)`}
            error={name.length > 0 && name.length < 3}
          />

          <TextField
            label="Description"
            placeholder="Enter description..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            fullWidth
            required
            multiline
            rows={4}
            helperText={`${description.length}/500 characters (min 10)`}
            error={description.length > 0 && description.length < 10}
          />

          <Typography variant="body2" color="text.secondary">
            Debug: Valid = {isValid.toString()}
          </Typography>

          {error && (
            <Alert severity="error">
              {error}
            </Alert>
          )}
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button onClick={onClose} disabled={isLoading}>
          Cancel
        </Button>
        
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isLoading || !isValid}
          startIcon={<AddIcon />}
        >
          {isLoading ? 'Creating...' : 'Create Hive'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default CreateHiveFormDebug