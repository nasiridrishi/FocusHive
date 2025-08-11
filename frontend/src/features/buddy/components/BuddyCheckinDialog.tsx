import React, { useState } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  CircularProgress,
  Rating,
  Chip,
  Divider,
  Avatar,
  Card,
  CardContent,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from '@mui/material'
import {
  Send as SendIcon,
  Close as CloseIcon,
  Mood as MoodIcon,
  TrendingUp as ProgressIcon,
  EmojiEvents as WinIcon,
  Warning as ChallengeIcon,
  Flag as FocusIcon
} from '@mui/icons-material'
import { buddyApi } from '../services/buddyApi'
import { BuddyRelationship, BuddyCheckin } from '../types'

interface BuddyCheckinDialogProps {
  open: boolean
  onClose: () => void
  relationship: BuddyRelationship
  onSubmit?: () => void
}

const BuddyCheckinDialog: React.FC<BuddyCheckinDialogProps> = ({
  open,
  onClose,
  relationship,
  onSubmit
}) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  const [formData, setFormData] = useState({
    moodRating: null as number | null,
    progressRating: null as number | null,
    message: '',
    currentFocus: '',
    challenges: '',
    wins: ''
  })

  const [selectedMoodEmoji, setSelectedMoodEmoji] = useState('')

  const moodEmojis = [
    { value: 1, emoji: '😞', label: 'Very Low' },
    { value: 2, emoji: '😕', label: 'Low' },
    { value: 3, emoji: '😐', label: 'Neutral' },
    { value: 4, emoji: '😊', label: 'Good' },
    { value: 5, emoji: '😄', label: 'Excellent' }
  ]

  const focusAreas = [
    'Programming/Development',
    'Design',
    'Writing',
    'Research',
    'Learning',
    'Project Management',
    'Client Work',
    'Personal Development',
    'Health & Fitness',
    'Other'
  ]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!formData.message.trim()) {
      setError('Please write a check-in message')
      return
    }

    if (formData.moodRating === null) {
      setError('Please rate your current mood')
      return
    }

    if (formData.progressRating === null) {
      setError('Please rate your progress')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const checkin: BuddyCheckin = {
        relationshipId: relationship.id,
        moodRating: formData.moodRating,
        progressRating: formData.progressRating,
        message: formData.message.trim(),
        currentFocus: formData.currentFocus.trim() || undefined,
        challenges: formData.challenges.trim() || undefined,
        wins: formData.wins.trim() || undefined
      }

      await buddyApi.createCheckin(relationship.id, checkin)
      
      // Reset form
      setFormData({
        moodRating: null,
        progressRating: null,
        message: '',
        currentFocus: '',
        challenges: '',
        wins: ''
      })
      setSelectedMoodEmoji('')
      
      if (onSubmit) {
        onSubmit()
      }
      
      onClose()
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit check-in')
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

  const handleMoodRatingChange = (event: React.SyntheticEvent, value: number | null) => {
    setFormData(prev => ({ ...prev, moodRating: value }))
    if (value) {
      const emoji = moodEmojis.find(m => m.value === value)
      setSelectedMoodEmoji(emoji?.emoji || '')
    }
  }

  const handleProgressRatingChange = (event: React.SyntheticEvent, value: number | null) => {
    setFormData(prev => ({ ...prev, progressRating: value }))
  }

  const handleClose = () => {
    setError(null)
    onClose()
  }

  const getMoodLabel = (rating: number | null) => {
    if (!rating) return ''
    const mood = moodEmojis.find(m => m.value === rating)
    return mood ? `${mood.emoji} ${mood.label}` : ''
  }

  const getProgressLabel = (rating: number | null) => {
    if (!rating) return ''
    const labels = ['Very Behind', 'Behind', 'On Track', 'Ahead', 'Way Ahead']
    return labels[rating - 1] || ''
  }

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box>
            <Typography variant="h6">
              Buddy Check-In
            </Typography>
            <Typography variant="body2" color="textSecondary">
              with {relationship.partnerUsername}
            </Typography>
          </Box>
          <Button onClick={handleClose} color="inherit">
            <CloseIcon />
          </Button>
        </Box>
      </DialogTitle>
      
      <form onSubmit={handleSubmit}>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          <Grid container spacing={3}>
            {/* Partner Info Card */}
            <Grid item xs={12}>
              <Card variant="outlined" sx={{ mb: 2 }}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={2}>
                    <Avatar src={relationship.partnerAvatar}>
                      {relationship.partnerUsername?.[0].toUpperCase()}
                    </Avatar>
                    <Box>
                      <Typography variant="h6">
                        {relationship.partnerUsername}
                      </Typography>
                      <Typography variant="body2" color="textSecondary">
                        Partner since {new Date(relationship.startDate!).toLocaleDateString()}
                      </Typography>
                    </Box>
                    <Box ml="auto" display="flex" gap={1}>
                      <Chip size="small" label={`${relationship.completedGoals || 0} goals`} />
                      <Chip size="small" label={`${relationship.totalSessions || 0} sessions`} />
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            {/* Mood Rating */}
            <Grid item xs={12} md={6}>
              <Box>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <MoodIcon color="primary" />
                  <Typography variant="h6">How are you feeling?</Typography>
                  {selectedMoodEmoji && (
                    <Typography variant="h4">{selectedMoodEmoji}</Typography>
                  )}
                </Box>
                <Rating
                  value={formData.moodRating}
                  onChange={handleMoodRatingChange}
                  size="large"
                  max={5}
                />
                <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
                  {getMoodLabel(formData.moodRating)}
                </Typography>
              </Box>
            </Grid>

            {/* Progress Rating */}
            <Grid item xs={12} md={6}>
              <Box>
                <Box display="flex" alignItems="center" gap={1} mb={2}>
                  <ProgressIcon color="primary" />
                  <Typography variant="h6">How's your progress?</Typography>
                </Box>
                <Rating
                  value={formData.progressRating}
                  onChange={handleProgressRatingChange}
                  size="large"
                  max={5}
                />
                <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
                  {getProgressLabel(formData.progressRating)}
                </Typography>
              </Box>
            </Grid>

            {/* Main Check-in Message */}
            <Grid item xs={12}>
              <TextField
                label="Check-in Message"
                multiline
                rows={4}
                value={formData.message}
                onChange={handleInputChange('message')}
                placeholder="Share how your day/session is going, what you've been working on, or anything else you'd like your buddy to know..."
                required
                fullWidth
                helperText="Let your buddy know how you're doing and what's on your mind"
              />
            </Grid>

            {/* Current Focus */}
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Current Focus Area</InputLabel>
                <Select
                  value={formData.currentFocus}
                  onChange={(e) => setFormData(prev => ({ ...prev, currentFocus: e.target.value }))}
                  label="Current Focus Area"
                >
                  <MenuItem value="">
                    <em>Select focus area</em>
                  </MenuItem>
                  {focusAreas.map((area) => (
                    <MenuItem key={area} value={area}>
                      {area}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Wins Section */}
            <Grid item xs={12} md={6}>
              <TextField
                label="Recent Wins"
                multiline
                rows={3}
                value={formData.wins}
                onChange={handleInputChange('wins')}
                placeholder="What went well? Any accomplishments or breakthroughs?"
                fullWidth
                InputProps={{
                  startAdornment: <WinIcon color="success" sx={{ mr: 1 }} />
                }}
                helperText="Celebrate your progress, no matter how small!"
              />
            </Grid>

            {/* Challenges Section */}
            <Grid item xs={12}>
              <TextField
                label="Current Challenges"
                multiline
                rows={3}
                value={formData.challenges}
                onChange={handleInputChange('challenges')}
                placeholder="What challenges are you facing? Where could you use support or advice?"
                fullWidth
                InputProps={{
                  startAdornment: <ChallengeIcon color="warning" sx={{ mr: 1 }} />
                }}
                helperText="Share any obstacles or areas where you're struggling"
              />
            </Grid>
          </Grid>
        </DialogContent>

        <DialogActions sx={{ p: 3, gap: 1 }}>
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
            startIcon={loading ? <CircularProgress size={20} /> : <SendIcon />}
            disabled={loading || !formData.message.trim() || formData.moodRating === null || formData.progressRating === null}
          >
            {loading ? 'Submitting...' : 'Submit Check-In'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default BuddyCheckinDialog