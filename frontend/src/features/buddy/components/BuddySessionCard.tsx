import React, {useState} from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Menu,
  MenuItem,
  Rating,
  TextField,
  Typography
} from '@mui/material'
import {
  AccessTime as TimeIcon,
  Cancel as CancelIcon,
  Edit as EditIcon,
  Event as EventIcon,
  MoreVert as MoreVertIcon,
  PlayArrow as PlayIcon,
  Star as StarIcon,
  Stop as StopIcon
} from '@mui/icons-material'
import {DateTimePicker} from '@mui/x-date-pickers/DateTimePicker'
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider'
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns'
import {buddyApi} from '../services/buddyApi'
import {BuddySession} from '../types'

interface BuddySessionCardProps {
  session: BuddySession
  onUpdate?: () => void
}

const BuddySessionCard: React.FC<BuddySessionCardProps> = ({
                                                             session,
                                                             onUpdate
                                                           }) => {
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)
  const [ratingDialogOpen, setRatingDialogOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Edit form state
  const [editForm, setEditForm] = useState({
    sessionDate: new Date(session.sessionDate),
    plannedDurationMinutes: session.plannedDurationMinutes,
    agenda: session.agenda || ''
  })

  // Cancel form state
  const [cancellationReason, setCancellationReason] = useState('')

  // Rating form state
  const [rating, setRating] = useState<number | null>(null)
  const [feedback, setFeedback] = useState('')

  const handleMenuClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
    setMenuAnchor(event.currentTarget)
  }

  const handleMenuClose = (): void => {
    setMenuAnchor(null)
  }

  const handleStartSession = async () => {
    if (!session.id) {
      setError('Session ID not available')
      return
    }
    setLoading(true)
    try {
      await buddyApi.startSession(session.id)
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error starting session');
      setError('Failed to start session')
    } finally {
      setLoading(false)
    }
  }

  const handleEndSession = async () => {
    if (!session.id) {
      setError('Session ID not available')
      return
    }
    setLoading(true)
    try {
      await buddyApi.endSession(session.id)
      setRatingDialogOpen(true)
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error ending session');
      setError('Failed to end session')
    } finally {
      setLoading(false)
    }
  }

  const handleEditSession = async () => {
    if (!session.id) {
      setError('Session ID not available')
      return
    }
    setLoading(true)
    try {
      const updatedSession: BuddySession = {
        ...session,
        sessionDate: editForm.sessionDate.toISOString(),
        plannedDurationMinutes: editForm.plannedDurationMinutes,
        agenda: editForm.agenda || undefined
      }
      await buddyApi.updateSession(session.id, updatedSession)
      setEditDialogOpen(false)
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error updating session');
      setError('Failed to update session')
    } finally {
      setLoading(false)
    }
  }

  const handleCancelSession = async () => {
    if (!session.id) {
      setError('Session ID not available')
      return
    }
    setLoading(true)
    try {
      await buddyApi.cancelSession(session.id, cancellationReason)
      setCancelDialogOpen(false)
      setCancellationReason('')
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error cancelling session');
      setError('Failed to cancel session')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmitRating = async () => {
    if (rating === null) {
      setError('Please provide a rating')
      return
    }
    if (!session.id) {
      setError('Session ID not available')
      return
    }

    setLoading(true)
    try {
      await buddyApi.rateSession(session.id, rating, feedback)
      setRatingDialogOpen(false)
      setRating(null)
      setFeedback('')
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error submitting rating');
      setError('Failed to submit rating')
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'SCHEDULED':
        return 'info'
      case 'IN_PROGRESS':
        return 'warning'
      case 'COMPLETED':
        return 'success'
      case 'CANCELLED':
      case 'NO_SHOW':
        return 'error'
      default:
        return 'default'
    }
  }

  const formatDateTime = (dateString: string): { date: string; time: string } => {
    const date = new Date(dateString)
    return {
      date: date.toLocaleDateString(),
      time: date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
    }
  }

  const formatDuration = (minutes: number): string => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    if (hours > 0) {
      return `${hours}h ${mins}m`
    }
    return `${mins}m`
  }

  const canStart = session.status === 'SCHEDULED' && new Date(session.sessionDate) <= new Date()
  const canEnd = session.status === 'IN_PROGRESS'
  const canEdit = ['SCHEDULED'].includes(session.status)
  const canCancel = ['SCHEDULED', 'IN_PROGRESS'].includes(session.status)
  const canRate = session.status === 'COMPLETED' && !session.user1Rating && !session.user2Rating

  const {date, time} = formatDateTime(session.sessionDate)

  return (
      <Card>
        <CardContent>
          {error && (
              <Alert severity="error" sx={{mb: 2}} onClose={() => setError(null)}>
                {error}
              </Alert>
          )}

          <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Buddy Session
              </Typography>
              <Box display="flex" alignItems="center" gap={1} mb={1}>
                <EventIcon color="action" fontSize="small"/>
                <Typography variant="body2" color="textSecondary">
                  {date} at {time}
                </Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <TimeIcon color="action" fontSize="small"/>
                <Typography variant="body2" color="textSecondary">
                  {formatDuration(session.plannedDurationMinutes)}
                  {session.actualDurationMinutes && (
                      < span > (actual: {formatDuration(session.actualDurationMinutes)})</span>
                    )}
                </Typography>
              </Box>
            </Box>

            <Box display="flex" alignItems="center" gap={1}>
              <Chip
                  label={session.status.replace('_', ' ')}
                  color={getStatusColor(session.status) as 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'}
                  size="small"
              />
              <IconButton size="small" onClick={handleMenuClick}>
                <MoreVertIcon/>
              </IconButton>
            </Box>
          </Box>

          {session.agenda && (
              <Box mb={2}>
                <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                  Agenda:
                </Typography>
                <Typography variant="body2">
                  {session.agenda}
                </Typography>
              </Box>
          )}

          {session.notes && (
              <Box mb={2}>
                <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                  Notes:
                </Typography>
                <Typography variant="body2">
                  {session.notes}
                </Typography>
              </Box>
          )}

          {/* Session Rating */}
          {session.averageRating && (
              <Box display="flex" alignItems="center" gap={1} mt={2}>
                <StarIcon color="primary" fontSize="small"/>
                <Typography variant="body2">
                  Rating: {session.averageRating.toFixed(1)}/5
                </Typography>
              </Box>
          )}

          {/* Join Status */}
          {(session.user1Joined || session.user2Joined) && (
              <Box mt={2}>
                <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                  Participation:
                </Typography>
                <Box display="flex" gap={1}>
                  {session.user1Joined && (
                      <Chip
                          size="small"
                          label="User 1 joined"
                          color="success"
                          variant="outlined"
                      />
                  )}
                  {session.user2Joined && (
                      <Chip
                          size="small"
                          label="User 2 joined"
                          color="success"
                          variant="outlined"
                      />
                  )}
                </Box>
              </Box>
          )}
        </CardContent>

        <CardActions>
          {canStart && (
              <Button
                  size="small"
                  variant="contained"
                  color="primary"
                  startIcon={loading ? <CircularProgress size={16}/> : <PlayIcon/>}
                  onClick={handleStartSession}
                  disabled={loading}
              >
                Start Session
              </Button>
          )}

          {canEnd && (
              <Button
                  size="small"
                  variant="contained"
                  color="warning"
                  startIcon={loading ? <CircularProgress size={16}/> : <StopIcon/>}
                  onClick={handleEndSession}
                  disabled={loading}
              >
                End Session
              </Button>
          )}

          {canRate && (
              <Button
                  size="small"
                  variant="outlined"
                  startIcon={<StarIcon/>}
                  onClick={() => setRatingDialogOpen(true)}
              >
                Rate Session
              </Button>
          )}
        </CardActions>

        {/* Menu */}
        <Menu
            anchorEl={menuAnchor}
            open={Boolean(menuAnchor)}
            onClose={handleMenuClose}
        >
          {canEdit && (
              <MenuItem onClick={() => {
                setEditDialogOpen(true);
                handleMenuClose()
              }}>
                <EditIcon sx={{mr: 1}} fontSize="small"/>
                Edit Session
              </MenuItem>
          )}
          {canCancel && (
              <MenuItem onClick={() => {
                setCancelDialogOpen(true);
                handleMenuClose()
              }}>
                <CancelIcon sx={{mr: 1}} fontSize="small"/>
                Cancel Session
              </MenuItem>
          )}
        </Menu>

        {/* Edit Dialog */}
        <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} fullWidth>
          <DialogTitle>Edit Session</DialogTitle>
          <DialogContent>
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3, mt: 2}}>
              <LocalizationProvider dateAdapter={AdapterDateFns}>
                <DateTimePicker
                    label="Session Date & Time"
                    value={editForm.sessionDate}
                    onChange={(date) => date && setEditForm(prev => ({...prev, sessionDate: date}))}
                    minDateTime={new Date()}
                    slotProps={{
                      textField: {fullWidth: true}
                    }}
                />
              </LocalizationProvider>

              <TextField
                  label="Duration (minutes)"
                  type="number"
                  value={editForm.plannedDurationMinutes}
                  onChange={(e) => setEditForm(prev => ({
                    ...prev,
                    plannedDurationMinutes: parseInt(e.target.value) || 0
                  }))}
                  inputProps={{min: 15, max: 480}}
                  fullWidth
              />

              <TextField
                  label="Agenda"
                  multiline
                  rows={3}
                  value={editForm.agenda}
                  onChange={(e) => setEditForm(prev => ({...prev, agenda: e.target.value}))}
                  placeholder="What do you want to accomplish in this session?"
                  fullWidth
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleEditSession} variant="contained" disabled={loading}>
              {loading ? 'Saving...' : 'Save Changes'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Cancel Dialog */}
        <Dialog open={cancelDialogOpen} onClose={() => setCancelDialogOpen(false)} fullWidth>
          <DialogTitle>Cancel Session</DialogTitle>
          <DialogContent>
            <TextField
                label="Reason for cancellation"
                multiline
                rows={3}
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                placeholder="Please provide a reason for canceling this session..."
                fullWidth
                sx={{mt: 2}}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCancelDialogOpen(false)}>Keep Session</Button>
            <Button
                onClick={handleCancelSession}
                color="error"
                variant="contained"
                disabled={loading}
            >
              {loading ? 'Cancelling...' : 'Cancel Session'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Rating Dialog */}
        <Dialog open={ratingDialogOpen} onClose={() => setRatingDialogOpen(false)} fullWidth>
          <DialogTitle>Rate This Session</DialogTitle>
          <DialogContent>
            <Box sx={{display: 'flex', flexDirection: 'column', gap: 3, mt: 2}}>
              <Box>
                <Typography component="legend" gutterBottom>
                  How was this session?
                </Typography>
                <Rating
                    value={rating}
                    onChange={(event, newValue) => setRating(newValue)}
                    size="large"
                />
              </Box>

              <TextField
                  label="Feedback (optional)"
                  multiline
                  rows={3}
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                  placeholder="Share your thoughts about this session..."
                  fullWidth
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setRatingDialogOpen(false)}>Skip</Button>
            <Button
                onClick={handleSubmitRating}
                variant="contained"
                disabled={rating === null || loading}
            >
              {loading ? 'Submitting...' : 'Submit Rating'}
            </Button>
          </DialogActions>
        </Dialog>
      </Card>
  )
}

export default BuddySessionCard