import React, { useState, useEffect, useMemo } from 'react'
import {
  Card,
  CardContent,
  Box,
  Typography,
  IconButton,
  Chip,
  LinearProgress,
  Stack,
  Tooltip,
  Fab,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Badge,
  Alert,
  Collapse,
} from '@mui/material'
import {
  PlayArrow,
  Pause,
  Stop,
  SkipNext,
  Settings,
  Timer,
  Coffee,
  FreeBreakfast,
  Add,
  NotificationAdd,
  VolumeOff,
  VolumeUp,
  Fullscreen,
  FullscreenExit,
} from '@mui/icons-material'
import { useTheme } from '@mui/material/styles'
import { useTimer } from '../contexts/TimerContext'
import { FocusTimerProps, TimerState } from '../../../shared/types/timer'

// Circular Progress Timer Component
const CircularTimer: React.FC<{
  timeRemaining: number
  totalTime: number
  phase: TimerState['currentPhase']
  size?: number
}> = ({ timeRemaining, totalTime, phase, size = 200 }) => {
  const theme = useTheme()
  
  const progress = totalTime > 0 ? ((totalTime - timeRemaining) / totalTime) * 100 : 0
  const circumference = 2 * Math.PI * (size / 2 - 8)
  const strokeDasharray = circumference
  const strokeDashoffset = circumference - (progress / 100) * circumference
  
  const getPhaseColor = (phase: TimerState['currentPhase']) => {
    switch (phase) {
      case 'focus': return theme.palette.primary.main
      case 'short-break': return theme.palette.success.main
      case 'long-break': return theme.palette.info.main
      default: return theme.palette.grey[400]
    }
  }

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <Box 
      sx={{ 
        position: 'relative', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        width: size,
        height: size,
      }}
    >
      {/* Background Circle */}
      <svg width={size} height={size} style={{ position: 'absolute' }}>
        <circle
          cx={size / 2}
          cy={size / 2}
          r={size / 2 - 8}
          fill="none"
          stroke={theme.palette.grey[200]}
          strokeWidth="4"
        />
        {/* Progress Circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={size / 2 - 8}
          fill="none"
          stroke={getPhaseColor(phase)}
          strokeWidth="6"
          strokeLinecap="round"
          strokeDasharray={strokeDasharray}
          strokeDashoffset={strokeDashoffset}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{
            transition: 'stroke-dashoffset 1s ease',
          }}
        />
      </svg>
      
      {/* Time Display */}
      <Box sx={{ textAlign: 'center', zIndex: 1 }}>
        <Typography 
          variant="h3" 
          component="div" 
          sx={{ 
            fontFamily: 'monospace', 
            fontWeight: 'bold',
            color: getPhaseColor(phase),
            fontSize: { xs: '1.8rem', sm: '2.5rem' }
          }}
        >
          {formatTime(timeRemaining)}
        </Typography>
        <Typography 
          variant="body2" 
          color="text.secondary"
          sx={{ textTransform: 'capitalize', mt: 1 }}
        >
          {phase.replace('-', ' ')}
        </Typography>
      </Box>
    </Box>
  )
}

// Timer Settings Menu Component
const TimerSettingsMenu: React.FC<{
  anchorEl: HTMLElement | null
  open: boolean
  onClose: () => void
}> = ({ anchorEl, open, onClose }) => {
  const { timerSettings, updateSettings } = useTimer()

  const handleToggleSound = () => {
    updateSettings({ soundEnabled: !timerSettings.soundEnabled })
  }

  const handleToggleNotifications = () => {
    updateSettings({ notificationsEnabled: !timerSettings.notificationsEnabled })
  }

  return (
    <Menu anchorEl={anchorEl} open={open} onClose={onClose}>
      <MenuItem onClick={handleToggleSound}>
        <ListItemIcon>
          {timerSettings.soundEnabled ? <VolumeUp /> : <VolumeOff />}
        </ListItemIcon>
        <ListItemText>
          {timerSettings.soundEnabled ? 'Disable Sound' : 'Enable Sound'}
        </ListItemText>
      </MenuItem>
      <MenuItem onClick={handleToggleNotifications}>
        <ListItemIcon>
          <NotificationAdd />
        </ListItemIcon>
        <ListItemText>
          {timerSettings.notificationsEnabled ? 'Disable Notifications' : 'Enable Notifications'}
        </ListItemText>
      </MenuItem>
      <Divider />
      <MenuItem onClick={onClose}>
        <ListItemIcon>
          <Settings />
        </ListItemIcon>
        <ListItemText>Advanced Settings</ListItemText>
      </MenuItem>
    </Menu>
  )
}

export const FocusTimer: React.FC<FocusTimerProps> = ({
  hiveId,
  onSessionStart,
  onSessionEnd,
  showSettings = true,
  compact = false,
}) => {
  const { 
    timerState, 
    timerSettings, 
    currentSession,
    startTimer, 
    pauseTimer, 
    resumeTimer, 
    stopTimer, 
    skipPhase,
    addGoal,
    recordDistraction 
  } = useTimer()
  
  const [settingsMenuEl, setSettingsMenuEl] = useState<HTMLElement | null>(null)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [showGoals, setShowGoals] = useState(false)
  const [newGoal, setNewGoal] = useState('')
  
  const theme = useTheme()

  // Calculate total time for current phase
  const totalTime = useMemo(() => {
    const getPhaseLength = (phase: TimerState['currentPhase']): number => {
      switch (phase) {
        case 'focus': return timerSettings.focusLength * 60
        case 'short-break': return timerSettings.shortBreakLength * 60
        case 'long-break': return timerSettings.longBreakLength * 60
        default: return 0
      }
    }
    return getPhaseLength(timerState.currentPhase)
  }, [timerState.currentPhase, timerSettings])

  // Handle session callbacks
  useEffect(() => {
    if (currentSession && onSessionStart) {
      onSessionStart(currentSession)
    }
  }, [currentSession, onSessionStart])

  // Request notification permission on mount
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
  }, [])

  const handlePlayPause = () => {
    if (timerState.isRunning) {
      pauseTimer()
    } else if (timerState.isPaused) {
      resumeTimer()
    } else {
      startTimer('focus', hiveId)
    }
  }

  const handleStop = () => {
    stopTimer()
    if (currentSession && onSessionEnd) {
      onSessionEnd(currentSession)
    }
  }

  const handleSkip = () => {
    skipPhase()
  }

  const handleSettingsClick = (event: React.MouseEvent<HTMLElement>) => {
    setSettingsMenuEl(event.currentTarget)
  }

  const handleSettingsClose = () => {
    setSettingsMenuEl(null)
  }

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen)
  }

  const getMainActionIcon = () => {
    if (timerState.isRunning) return <Pause />
    if (timerState.isPaused) return <PlayArrow />
    return <PlayArrow />
  }

  const getMainActionTooltip = () => {
    if (timerState.isRunning) return 'Pause Timer'
    if (timerState.isPaused) return 'Resume Timer'
    return 'Start Focus Session'
  }

  const getPhaseIcon = (phase: TimerState['currentPhase']) => {
    switch (phase) {
      case 'focus': return <Timer />
      case 'short-break': return <Coffee />
      case 'long-break': return <FreeBreakfast />
      default: return <Timer />
    }
  }

  const getPhaseText = (phase: TimerState['currentPhase']) => {
    switch (phase) {
      case 'focus': return 'Focus Time'
      case 'short-break': return 'Short Break'
      case 'long-break': return 'Long Break'
      default: return 'Ready to Focus'
    }
  }

  if (compact) {
    return (
      <Card sx={{ minWidth: 280 }}>
        <CardContent sx={{ p: 2 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <CircularTimer
              timeRemaining={timerState.timeRemaining}
              totalTime={totalTime}
              phase={timerState.currentPhase}
              size={80}
            />
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" gutterBottom>
                {getPhaseText(timerState.currentPhase)}
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center">
                <Tooltip title={getMainActionTooltip()}>
                  <IconButton 
                    onClick={handlePlayPause}
                    color="primary"
                    size="small"
                  >
                    {getMainActionIcon()}
                  </IconButton>
                </Tooltip>
                {(timerState.isRunning || timerState.isPaused) && (
                  <>
                    <Tooltip title="Stop Timer">
                      <IconButton onClick={handleStop} size="small">
                        <Stop />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Skip Phase">
                      <IconButton onClick={handleSkip} size="small">
                        <SkipNext />
                      </IconButton>
                    </Tooltip>
                  </>
                )}
              </Stack>
            </Box>
          </Stack>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card 
      sx={{ 
        maxWidth: isFullscreen ? '100vw' : 500,
        height: isFullscreen ? '100vh' : 'auto',
        position: isFullscreen ? 'fixed' : 'relative',
        top: isFullscreen ? 0 : 'auto',
        left: isFullscreen ? 0 : 'auto',
        zIndex: isFullscreen ? theme.zIndex.modal : 'auto',
        borderRadius: isFullscreen ? 0 : undefined,
      }}
    >
      <CardContent sx={{ p: 3, textAlign: 'center' }}>
        {/* Header */}
        <Stack 
          direction="row" 
          justifyContent="space-between" 
          alignItems="center"
          sx={{ mb: 3 }}
        >
          <Chip
            icon={getPhaseIcon(timerState.currentPhase)}
            label={getPhaseText(timerState.currentPhase)}
            color={timerState.currentPhase === 'focus' ? 'primary' : 'success'}
            variant={timerState.isRunning ? 'filled' : 'outlined'}
          />
          
          <Stack direction="row" spacing={1}>
            {showSettings && (
              <Tooltip title="Settings">
                <IconButton onClick={handleSettingsClick} size="small">
                  <Settings />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}>
              <IconButton onClick={toggleFullscreen} size="small">
                {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
              </IconButton>
            </Tooltip>
          </Stack>
        </Stack>

        {/* Current Session Info */}
        {currentSession && (
          <Alert 
            severity="info" 
            sx={{ mb: 3 }}
            action={
              <Chip 
                label={`Cycle ${timerState.currentCycle}/${currentSession.targetCycles}`}
                size="small"
              />
            }
          >
            Focus session in progress
          </Alert>
        )}

        {/* Timer Display */}
        <Box sx={{ mb: 4 }}>
          <CircularTimer
            timeRemaining={timerState.timeRemaining}
            totalTime={totalTime}
            phase={timerState.currentPhase}
            size={isFullscreen ? 300 : 240}
          />
        </Box>

        {/* Progress Bar for Session */}
        {currentSession && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Session Progress
            </Typography>
            <LinearProgress 
              variant="determinate" 
              value={(timerState.currentCycle / currentSession.targetCycles) * 100}
              sx={{ height: 8, borderRadius: 4 }}
            />
          </Box>
        )}

        {/* Main Controls */}
        <Stack direction="row" spacing={2} justifyContent="center" sx={{ mb: 3 }}>
          <Tooltip title={getMainActionTooltip()}>
            <Fab 
              color="primary" 
              onClick={handlePlayPause}
              size={isFullscreen ? 'large' : 'medium'}
            >
              {getMainActionIcon()}
            </Fab>
          </Tooltip>
          
          {(timerState.isRunning || timerState.isPaused) && (
            <>
              <Tooltip title="Stop Timer">
                <IconButton 
                  onClick={handleStop} 
                  size="large"
                  sx={{ 
                    bgcolor: theme.palette.error.main,
                    color: 'white',
                    '&:hover': { bgcolor: theme.palette.error.dark }
                  }}
                >
                  <Stop />
                </IconButton>
              </Tooltip>
              <Tooltip title="Skip Phase">
                <IconButton onClick={handleSkip} size="large">
                  <SkipNext />
                </IconButton>
              </Tooltip>
            </>
          )}
        </Stack>

        {/* Quick Actions */}
        {currentSession && (
          <Stack direction="row" spacing={1} justifyContent="center" sx={{ mb: 2 }}>
            <Tooltip title="Record Distraction">
              <Chip
                label={`Distractions: ${currentSession.distractions}`}
                onClick={recordDistraction}
                clickable
                color="warning"
                variant="outlined"
                size="small"
              />
            </Tooltip>
            <Tooltip title="Session Goals">
              <Badge badgeContent={currentSession.goals.length} color="primary">
                <Chip
                  label="Goals"
                  onClick={() => setShowGoals(!showGoals)}
                  clickable
                  variant="outlined"
                  size="small"
                />
              </Badge>
            </Tooltip>
          </Stack>
        )}

        {/* Goals Section */}
        <Collapse in={showGoals && !!currentSession}>
          <Box sx={{ mt: 2, textAlign: 'left' }}>
            <Typography variant="subtitle2" gutterBottom>
              Session Goals
            </Typography>
            {currentSession?.goals.map((goal) => (
              <Chip
                key={goal.id}
                label={goal.description}
                onDelete={() => {/* Handle goal completion */}}
                color={goal.isCompleted ? 'success' : 'default'}
                variant={goal.isCompleted ? 'filled' : 'outlined'}
                size="small"
                sx={{ m: 0.5 }}
              />
            ))}
            <Box sx={{ mt: 1 }}>
              <Chip
                icon={<Add />}
                label="Add Goal"
                onClick={() => {/* Handle add goal */}}
                clickable
                variant="outlined"
                size="small"
              />
            </Box>
          </Box>
        </Collapse>

        {/* Timer Settings Menu */}
        <TimerSettingsMenu
          anchorEl={settingsMenuEl}
          open={Boolean(settingsMenuEl)}
          onClose={handleSettingsClose}
        />
      </CardContent>
    </Card>
  )
}

export default FocusTimer