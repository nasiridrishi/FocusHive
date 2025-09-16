import React, {useEffect, useState} from 'react'
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
  Grid,
  IconButton,
  LinearProgress,
  Paper,
  TextField,
  Typography
} from '@mui/material'

// Grid component type workaround
import {
  Add as AddIcon,
  Cancel as CancelIcon,
  CheckCircle as CheckCircleIcon,
  Close as CloseIcon,
  Edit as EditIcon,
  Flag as FlagIcon
} from '@mui/icons-material'
import {DatePicker} from '@mui/x-date-pickers/DatePicker'
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider'
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns'
import {buddyApi} from '../services/buddyApi'
import {BuddyGoal} from '../types'

interface BuddyGoalsListProps {
  relationshipId: number
  onUpdate?: () => void
}

interface GoalFormData {
  title: string
  description: string
  dueDate: Date | null
  metrics: string
}

const BuddyGoalsList: React.FC<BuddyGoalsListProps> = ({
                                                         relationshipId,
                                                         onUpdate
                                                       }) => {
  const [goals, setGoals] = useState<BuddyGoal[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingGoal, setEditingGoal] = useState<BuddyGoal | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Form state
  const [formData, setFormData] = useState<GoalFormData>({
    title: '',
    description: '',
    dueDate: null,
    metrics: ''
  })

  useEffect(() => {
    const fetchGoals = async () => {
      try {
        setLoading(true)
        const goalsData = await buddyApi.getRelationshipGoals(relationshipId)
        setGoals(goalsData)
      } catch {
        // console.error('Error:', err);
        setError('Failed to load goals')
      } finally {
        setLoading(false)
      }
    }
    fetchGoals()
  }, [relationshipId])

  const loadGoals = async () => {
    try {
      setLoading(true)
      const goalsData = await buddyApi.getRelationshipGoals(relationshipId)
      setGoals(goalsData)
    } catch {
      // console.error('Error:', err);
      setError('Failed to load goals')
    } finally {
      setLoading(false)
    }
  }

  const handleOpenDialog = (goal?: BuddyGoal): void => {
    if (goal) {
      setEditingGoal(goal)
      setFormData({
        title: goal.title || '',
        description: goal.description || '',
        dueDate: goal.dueDate ? new Date(goal.dueDate) : null,
        metrics: goal.metrics ? JSON.stringify(goal.metrics) : ''
      })
    } else {
      setEditingGoal(null)
      setFormData({
        title: '',
        description: '',
        dueDate: null,
        metrics: ''
      })
    }
    setDialogOpen(true)
  }

  const handleCloseDialog = (): void => {
    setDialogOpen(false)
    setEditingGoal(null)
    setError(null)
  }

  const handleInputChange = (field: keyof GoalFormData) => (
      e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setFormData(prev => ({
      ...prev,
      [field]: e.target.value
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.title.trim()) {
      setError('Goal title is required')
      return
    }

    setSubmitting(true)
    setError(null)

    try {
      const goalData: BuddyGoal = {
        relationshipId,
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        dueDate: formData.dueDate?.toISOString().split('T')[0],
        metrics: formData.metrics ? JSON.parse(formData.metrics) : undefined,
        status: 'IN_PROGRESS'
      }

      if (editingGoal && editingGoal.id) {
        await buddyApi.updateGoal(editingGoal.id, goalData)
      } else {
        await buddyApi.createGoal(relationshipId, goalData)
      }

      await loadGoals()
      if (onUpdate) onUpdate()
      handleCloseDialog()
    } catch (err) {
      const error = err as Error & { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to save goal')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCompleteGoal = async (goalId: number) => {
    try {
      await buddyApi.completeGoal(goalId)
      await loadGoals()
      if (onUpdate) onUpdate()
    } catch {
      // console.error('Error:', err);
      setError('Failed to complete goal')
    }
  }

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'COMPLETED':
        return 'success'
      case 'CANCELLED':
        return 'error'
      case 'IN_PROGRESS':
      default:
        return 'primary'
    }
  }

  const getStatusIcon = (status: string): React.ReactElement => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircleIcon/>
      case 'CANCELLED':
        return <CancelIcon/>
      case 'IN_PROGRESS':
      default:
        return <FlagIcon/>
    }
  }

  const calculateProgress = (goal: BuddyGoal): number => {
    if (goal.status === 'COMPLETED') return 100
    if (goal.status === 'CANCELLED') return 0
    return goal.progressPercentage || 0
  }

  const formatDate = (dateString?: string): string => {
    if (!dateString) return 'No due date'
    return new Date(dateString).toLocaleDateString()
  }

  const isOverdue = (goal: BuddyGoal): boolean => {
    if (!goal.dueDate || goal.status === 'COMPLETED') return false
    return new Date(goal.dueDate) < new Date()
  }

  if (loading) {
    return (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress/>
        </Box>
    )
  }

  return (
      <Box>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h6">Shared Goals</Typography>
          <Button
              variant="contained"
              startIcon={<AddIcon/>}
              onClick={() => handleOpenDialog()}
          >
            Add Goal
          </Button>
        </Box>

        {error && (
            <Alert severity="error" sx={{mb: 2}} onClose={() => setError(null)}>
              {error}
            </Alert>
        )}

        {goals.length === 0 ? (
            <Paper sx={{p: 4, textAlign: 'center'}}>
              <Typography color="textSecondary" gutterBottom>
                No goals created yet
              </Typography>
              <Button
                  variant="outlined"
                  startIcon={<AddIcon/>}
                  onClick={() => handleOpenDialog()}
              >
                Create Your First Goal
              </Button>
            </Paper>
        ) : (
            <Grid container spacing={2}>
              {goals.map((goal) => (
                  <Grid item key={goal.id}>
                    <Card
                        sx={{
                          border: isOverdue(goal) ? '1px solid' : 'none',
                          borderColor: isOverdue(goal) ? 'error.main' : 'transparent'
                        }}
                    >
                      <CardContent>
                        <Box display="flex" justifyContent="space-between" alignItems="flex-start"
                             mb={2}>
                          <Typography variant="h6" sx={{flexGrow: 1}}>
                            {goal.title}
                          </Typography>
                          <Chip
                              icon={getStatusIcon(goal.status)}
                              label={goal.status.replace('_', ' ')}
                              color={getStatusColor(goal.status) as 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'}
                              size="small"
                              sx={{ml: 1}}
                          />
                        </Box>

                        {goal.description && (
                            <Typography variant="body2" color="textSecondary" paragraph>
                              {goal.description}
                            </Typography>
                        )}

                        <Box mb={2}>
                          <Typography variant="caption" color="textSecondary">
                            Progress: {calculateProgress(goal)}%
                          </Typography>
                          <LinearProgress
                              variant="determinate"
                              value={calculateProgress(goal)}
                              sx={{mt: 0.5}}
                              color={goal.status === 'COMPLETED' ? 'success' : 'primary'}
                          />
                        </Box>

                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Typography variant="caption"
                                      color={isOverdue(goal) ? 'error' : 'textSecondary'}>
                            Due: {formatDate(goal.dueDate)}
                            {isOverdue(goal) && ' (Overdue)'}
                          </Typography>

                          {goal.completedByUsername && (
                              <Typography variant="caption" color="success.main">
                                Completed by {goal.completedByUsername}
                              </Typography>
                          )}
                        </Box>
                      </CardContent>

                      <CardActions>
                        <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(goal)}
                            disabled={goal.status === 'COMPLETED'}
                        >
                          <EditIcon/>
                        </IconButton>

                        {goal.status === 'IN_PROGRESS' && (
                            <Button
                                size="small"
                                color="success"
                                startIcon={<CheckCircleIcon/>}
                                onClick={() => goal.id && handleCompleteGoal(goal.id)}
                            >
                              Complete
                            </Button>
                        )}
                      </CardActions>
                    </Card>
                  </Grid>
              ))}
            </Grid>
        )}

        {/* Goal Form Dialog */}
        <Dialog open={dialogOpen} onClose={handleCloseDialog} fullWidth>
          <DialogTitle>
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">
                {editingGoal ? 'Edit Goal' : 'Create New Goal'}
              </Typography>
              <IconButton onClick={handleCloseDialog}>
                <CloseIcon/>
              </IconButton>
            </Box>
          </DialogTitle>

          <form onSubmit={handleSubmit}>
            <DialogContent>
              <Box sx={{display: 'flex', flexDirection: 'column', gap: 3, mt: 1}}>
                <TextField
                    label="Goal Title"
                    value={formData.title}
                    onChange={handleInputChange('title')}
                    required
                    fullWidth
                    placeholder="e.g., Complete JavaScript course, Build portfolio website"
                />

                <TextField
                    label="Description"
                    multiline
                    rows={3}
                    value={formData.description}
                    onChange={handleInputChange('description')}
                    fullWidth
                    placeholder="Describe the goal in more detail..."
                />

                <LocalizationProvider dateAdapter={AdapterDateFns}>
                  <DatePicker
                      label="Due Date"
                      value={formData.dueDate}
                      onChange={(date) => setFormData(prev => ({...prev, dueDate: date}))}
                      minDate={new Date()}
                      slotProps={{
                        textField: {
                          fullWidth: true,
                          helperText: "Optional: Set a target completion date"
                        }
                      }}
                  />
                </LocalizationProvider>

                <TextField
                    label="Success Metrics (JSON format)"
                    multiline
                    rows={2}
                    value={formData.metrics}
                    onChange={handleInputChange('metrics')}
                    fullWidth
                    placeholder='e.g., {"hours": 20, "modules": 10}'
                    helperText="Optional: Define measurable criteria for success"
                />
              </Box>
            </DialogContent>

            <DialogActions sx={{p: 3, gap: 1}}>
              <Button onClick={handleCloseDialog} disabled={submitting}>
                Cancel
              </Button>
              <Button
                  type="submit"
                  variant="contained"
                  disabled={submitting || !formData.title.trim()}
                  startIcon={submitting ? <CircularProgress size={16}/> : undefined}
              >
                {submitting ? 'Saving...' : editingGoal ? 'Update Goal' : 'Create Goal'}
              </Button>
            </DialogActions>
          </form>
        </Dialog>
      </Box>
  )
}

export default BuddyGoalsList