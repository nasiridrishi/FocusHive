import React, {useState} from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography
} from '@mui/material'
import {Close as CloseIcon, Send as SendIcon} from '@mui/icons-material'
import {DatePicker} from '@mui/x-date-pickers/DatePicker'
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider'
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns'
import {buddyApi} from '../services/buddyApi'
import {BuddyRequest} from '../types'

interface BuddyRequestDialogProps {
  open: boolean
  onClose: () => void
  onSent?: () => void
  recipientUserId?: number
  recipientUsername?: string
}

const BuddyRequestDialog: React.FC<BuddyRequestDialogProps> = ({
                                                                 open,
                                                                 onClose,
                                                                 onSent,
                                                                 recipientUserId,
                                                                 recipientUsername
                                                               }) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    toUserId: recipientUserId || 0,
    message: '',
    proposedEndDate: null as Date | null,
    goals: '',
    expectations: ''
  })

  const [selectedFocusAreas, setSelectedFocusAreas] = useState<string[]>([])

  const focusAreaOptions = [
    'Programming', 'Web Development', 'Data Science', 'Machine Learning',
    'Design', 'Writing', 'Research', 'Language Learning', 'Studying',
    'Career Development', 'Personal Projects', 'Reading', 'Fitness',
    'Productivity', 'Time Management', 'Work-Life Balance'
  ]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.toUserId) {
      setError('Please select a recipient')
      return
    }

    if (!formData.message.trim()) {
      setError('Please write a message introducing yourself')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const request: BuddyRequest = {
        toUserId: formData.toUserId,
        message: formData.message.trim(),
        proposedEndDate: formData.proposedEndDate?.toISOString().split('T')[0],
        goals: formData.goals.trim(),
        expectations: formData.expectations.trim()
      }

      await buddyApi.sendBuddyRequest(request)

      // Reset form
      setFormData({
        toUserId: 0,
        message: '',
        proposedEndDate: null,
        goals: '',
        expectations: ''
      })
      setSelectedFocusAreas([])

      if (onSent) {
        onSent()
      }

      onClose()
    } catch (err) {
      const error = err as Error & { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to send buddy request')
    } finally {
      setLoading(false)
    }
  }

  const handleInputChange = (field: keyof typeof formData) => (
      e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setFormData(prev => ({
      ...prev,
      [field]: e.target.value
    }))
  }

  const handleFocusAreaToggle = (area: string): void => {
    setSelectedFocusAreas(prev =>
        prev.includes(area)
            ? prev.filter(a => a !== area)
            : [...prev, area]
    )
  }

  const handleClose = (): void => {
    setError(null)
    onClose()
  }

  return (
      <Dialog open={open} onClose={handleClose} fullWidth>
        <DialogTitle>
          <Box display="flex" alignItems="center" justifyContent="space-between">
            <Typography variant="h6">
              Send Buddy Request
              {recipientUsername && (
                  <Typography variant="body2" color="textSecondary" component="span">
                    {' '}to {recipientUsername}
                  </Typography>
              )}
            </Typography>
            <Button onClick={handleClose} color="inherit">
              <CloseIcon/>
            </Button>
          </Box>
        </DialogTitle>

        <form onSubmit={handleSubmit}>
          <DialogContent>
            {error && (
                <Alert severity="error" sx={{mb: 2}}>
                  {error}
                </Alert>
            )}

            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3}}>
              {/* Personal Introduction */}
              <TextField
                  label="Personal Message"
                  multiline
                  rows={4}
                  value={formData.message}
                  onChange={handleInputChange('message')}
                  placeholder="Introduce yourself and explain why you'd like to be buddies..."
                  required
                  fullWidth
                  helperText="Tell them about your background, work style, and what you hope to achieve together"
              />

              {/* Focus Areas */}
              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Focus Areas (Select areas you want to work on together)
                </Typography>
                <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1}}>
                  {focusAreaOptions.map((area) => (
                      <Chip
                          key={area}
                          label={area}
                          clickable
                          color={selectedFocusAreas.includes(area) ? 'primary' : 'default'}
                          variant={selectedFocusAreas.includes(area) ? 'filled' : 'outlined'}
                          onClick={() => handleFocusAreaToggle(area)}
                          size="small"
                      />
                  ))}
                </Box>
              </Box>

              {/* Goals */}
              <TextField
                  label="Shared Goals"
                  multiline
                  rows={3}
                  value={formData.goals}
                  onChange={handleInputChange('goals')}
                  placeholder="What specific goals would you like to work towards together?"
                  fullWidth
                  helperText="Be specific about what you want to accomplish during your buddy partnership"
              />

              {/* Expectations */}
              <TextField
                  label="Expectations & Preferences"
                  multiline
                  rows={3}
                  value={formData.expectations}
                  onChange={handleInputChange('expectations')}
                  placeholder="What are your expectations for communication, check-ins, session frequency, etc.?"
                  fullWidth
                  helperText="Help them understand your preferred working style and communication frequency"
              />

              {/* Proposed End Date */}
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <DatePicker
                    label="Proposed End Date (Optional)"
                    value={formData.proposedEndDate}
                    onChange={(date) => setFormData(prev => ({...prev, proposedEndDate: date}))}
                    minDate={new Date()}
                    slotProps={{
                      textField: {
                        fullWidth: true,
                        helperText: "Suggest when you'd like to review or conclude the partnership"
                      }
                    }}
                />
              </LocalizationProvider>

              {/* User ID Input (if not pre-filled) */}
              {!recipientUserId && (
                  <TextField
                      label="Recipient User ID"
                      type="number"
                      value={formData.toUserId || ''}
                      onChange={handleInputChange('toUserId')}
                      required
                      fullWidth
                      helperText="Enter the user ID of the person you want to send a request to"
                  />
              )}
            </Box>
          </DialogContent>

          <DialogActions sx={{p: 3, gap: 1}}>
            <Button
                onClick={handleClose}
                color="inherit"
                disabled={loading}
            >
              Cancel
            </Button>
            <Button
                type="submit"
                variant="contained"
                startIcon={loading ? <CircularProgress size={20}/> : <SendIcon/>}
                disabled={loading || !formData.message.trim()}
            >
              {loading ? 'Sending...' : 'Send Request'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
  )
}

export default BuddyRequestDialog