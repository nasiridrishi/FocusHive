import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  FormControl,
  FormLabel,
  FormGroup,
  FormControlLabel,
  Switch,
  Select,
  MenuItem,
  InputLabel,
  Chip,
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Alert,
  CircularProgress,
  Autocomplete,
  Paper,
  Divider,
} from '@mui/material'
import {
  Add as AddIcon,
  Public as PublicIcon,
  Lock as LockIcon,
  Group as GroupIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material'
import { CreateHiveRequest, HiveSettings } from '@shared/types'

interface CreateHiveFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (hive: CreateHiveRequest) => void
  isLoading?: boolean
  error?: string | null
}

const steps = ['Basic Info', 'Settings', 'Review']

const focusModeOptions = [
  { value: 'pomodoro', label: 'Pomodoro (25/5 min cycles)', description: 'Structured work sessions with breaks' },
  { value: 'continuous', label: 'Continuous', description: 'Uninterrupted focus sessions' },
  { value: 'flexible', label: 'Flexible', description: 'Members choose their own timing' },
]

const commonTags = [
  'Study', 'Work', 'Coding', 'Writing', 'Reading', 'Research',
  'Art', 'Design', 'Language Learning', 'Fitness', 'Music',
  'Science', 'Math', 'Programming', 'Business', 'Creative'
]

export const CreateHiveForm: React.FC<CreateHiveFormProps> = ({
  open,
  onClose,
  onSubmit,
  isLoading = false,
  error = null,
}) => {
  const [activeStep, setActiveStep] = useState(0)
  const [formData, setFormData] = useState<CreateHiveRequest>({
    name: '',
    description: '',
    maxMembers: 10,
    isPublic: true,
    tags: [],
    settings: {
      allowChat: true,
      allowVoice: false,
      requireApproval: false,
      focusMode: 'flexible',
      defaultSessionLength: 25,
      maxSessionLength: 120,
    },
  })

  // const [newTag, setNewTag] = useState('')

  const handleNext = () => {
    setActiveStep((prevActiveStep) => prevActiveStep + 1)
  }

  const handleBack = () => {
    setActiveStep((prevActiveStep) => prevActiveStep - 1)
  }

  const handleInputChange = (field: keyof CreateHiveRequest) => 
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
      setFormData(prev => ({
        ...prev,
        [field]: value,
      }))
    }

  const handleSettingsChange = (field: keyof HiveSettings) => 
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
      setFormData(prev => ({
        ...prev,
        settings: {
          ...prev.settings,
          [field]: value,
        },
      }))
    }

  const handleSelectChange = (field: keyof CreateHiveRequest | keyof HiveSettings) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const value = event.target.value
      
      if (field in formData.settings) {
        setFormData(prev => ({
          ...prev,
          settings: {
            ...prev.settings,
            [field]: value,
          },
        }))
      } else {
        setFormData(prev => ({
          ...prev,
          [field]: value,
        }))
      }
    }

  const handleTagsChange = (_event: React.SyntheticEvent, newValue: string[]) => {
    setFormData(prev => ({
      ...prev,
      tags: newValue,
    }))
  }


  const handleSubmit = () => {
    onSubmit(formData)
  }

  const handleClose = () => {
    if (!isLoading) {
      onClose()
      // Reset form after dialog closes
      setTimeout(() => {
        setActiveStep(0)
        setFormData({
          name: '',
          description: '',
          maxMembers: 10,
          isPublic: true,
          tags: [],
          settings: {
            allowChat: true,
            allowVoice: false,
            requireApproval: false,
            focusMode: 'flexible',
            defaultSessionLength: 25,
            maxSessionLength: 120,
          },
        })
        // setNewTag('')
      }, 300)
    }
  }

  const isStepValid = (step: number) => {
    switch (step) {
      case 0:
        return formData.name.trim().length >= 3 && formData.description.trim().length >= 10
      case 1:
        return true
      case 2:
        return true
      default:
        return false
    }
  }

  const renderStepContent = (step: number) => {
    switch (step) {
      case 0:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <TextField
              autoFocus
              label="Hive Name"
              placeholder="e.g., Study Group, Writing Circle, Code Jam"
              value={formData.name}
              onChange={handleInputChange('name')}
              fullWidth
              required
              helperText={`${formData.name.length}/50 characters`}
              inputProps={{ maxLength: 50 }}
              error={formData.name.length > 0 && formData.name.length < 3}
            />

            <TextField
              label="Description"
              placeholder="Describe the purpose and goals of your hive..."
              value={formData.description}
              onChange={handleInputChange('description')}
              fullWidth
              required
              multiline
              rows={4}
              helperText={`${formData.description.length}/500 characters`}
              inputProps={{ maxLength: 500 }}
              error={formData.description.length > 0 && formData.description.length < 10}
            />

            <FormControl fullWidth>
              <InputLabel>Maximum Members</InputLabel>
              <Select
                value={formData.maxMembers}
                label="Maximum Members"
                onChange={handleSelectChange('maxMembers')}
              >
                {[5, 10, 15, 20, 25, 30, 50].map(num => (
                  <MenuItem key={num} value={num}>{num} members</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControlLabel
              control={
                <Switch
                  checked={formData.isPublic}
                  onChange={handleInputChange('isPublic')}
                  color="primary"
                />
              }
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {formData.isPublic ? <PublicIcon /> : <LockIcon />}
                  {formData.isPublic ? 'Public Hive' : 'Private Hive'}
                </Box>
              }
            />

            <Typography variant="body2" color="text.secondary">
              {formData.isPublic 
                ? 'Anyone can discover and join this hive'
                : 'Only invited members can join this hive'
              }
            </Typography>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Tags (help others discover your hive)
              </Typography>
              <Autocomplete
                multiple
                freeSolo
                options={commonTags}
                value={formData.tags}
                onChange={handleTagsChange}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip variant="outlined" label={option} {...getTagProps({ index })} />
                  ))
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Add tags..."
                    helperText="Press Enter to add custom tags"
                  />
                )}
              />
            </Box>
          </Box>
        )

      case 1:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <SettingsIcon />
              Hive Settings
            </Typography>

            <FormControl component="fieldset">
              <FormLabel component="legend">Communication</FormLabel>
              <FormGroup>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.settings.allowChat}
                      onChange={handleSettingsChange('allowChat')}
                    />
                  }
                  label="Allow chat messages"
                />
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.settings.allowVoice}
                      onChange={handleSettingsChange('allowVoice')}
                    />
                  }
                  label="Allow voice calls (coming soon)"
                  disabled
                />
              </FormGroup>
            </FormControl>

            <FormControlLabel
              control={
                <Switch
                  checked={formData.settings.requireApproval}
                  onChange={handleSettingsChange('requireApproval')}
                />
              }
              label="Require approval to join"
            />

            <FormControl fullWidth>
              <InputLabel>Focus Mode</InputLabel>
              <Select
                value={formData.settings.focusMode}
                label="Focus Mode"
                onChange={handleSelectChange('focusMode')}
              >
                {focusModeOptions.map(option => (
                  <MenuItem key={option.value} value={option.value}>
                    <Box>
                      <Typography variant="body1">{option.label}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {option.description}
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                label="Default Session (minutes)"
                type="number"
                value={formData.settings.defaultSessionLength}
                onChange={handleSettingsChange('defaultSessionLength')}
                inputProps={{ min: 5, max: 120, step: 5 }}
                sx={{ flex: 1 }}
              />
              <TextField
                label="Max Session (minutes)"
                type="number"
                value={formData.settings.maxSessionLength}
                onChange={handleSettingsChange('maxSessionLength')}
                inputProps={{ min: 30, max: 480, step: 15 }}
                sx={{ flex: 1 }}
              />
            </Box>
          </Box>
        )

      case 2:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Typography variant="h6">Review Your Hive</Typography>

            <Paper variant="outlined" sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                <GroupIcon color="primary" />
                <Typography variant="h6">{formData.name}</Typography>
                <Chip
                  size="small"
                  icon={formData.isPublic ? <PublicIcon /> : <LockIcon />}
                  label={formData.isPublic ? 'Public' : 'Private'}
                  color={formData.isPublic ? 'success' : 'warning'}
                />
              </Box>

              <Typography variant="body1" color="text.secondary" paragraph>
                {formData.description}
              </Typography>

              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                {formData.tags.map(tag => (
                  <Chip key={tag} size="small" label={tag} variant="outlined" />
                ))}
              </Box>

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle2" gutterBottom>Settings</Typography>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                <Typography variant="body2">Max Members: {formData.maxMembers}</Typography>
                <Typography variant="body2">Focus Mode: {formData.settings.focusMode}</Typography>
                <Typography variant="body2">
                  Chat: {formData.settings.allowChat ? 'Enabled' : 'Disabled'}
                </Typography>
                <Typography variant="body2">
                  Approval: {formData.settings.requireApproval ? 'Required' : 'Not Required'}
                </Typography>
                <Typography variant="body2">
                  Default Session: {formData.settings.defaultSessionLength}m
                </Typography>
                <Typography variant="body2">
                  Max Session: {formData.settings.maxSessionLength}m
                </Typography>
              </Box>
            </Paper>

            {error && (
              <Alert severity="error">
                {error}
              </Alert>
            )}
          </Box>
        )

      default:
        return null
    }
  }

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: { borderRadius: 2, minHeight: 600 }
      }}
    >
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <AddIcon />
          Create New Hive
        </Box>
      </DialogTitle>

      <DialogContent>
        <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {renderStepContent(activeStep)}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button onClick={handleClose} disabled={isLoading}>
          Cancel
        </Button>
        
        <Box sx={{ display: 'flex', gap: 1 }}>
          {activeStep > 0 && (
            <Button onClick={handleBack} disabled={isLoading}>
              Back
            </Button>
          )}
          
          {activeStep < steps.length - 1 ? (
            <Button
              variant="contained"
              onClick={handleNext}
              disabled={!isStepValid(activeStep)}
            >
              Next
            </Button>
          ) : (
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={isLoading || !isStepValid(activeStep)}
              startIcon={isLoading ? <CircularProgress size={16} /> : <AddIcon />}
            >
              {isLoading ? 'Creating...' : 'Create Hive'}
            </Button>
          )}
        </Box>
      </DialogActions>
    </Dialog>
  )
}

export default CreateHiveForm

