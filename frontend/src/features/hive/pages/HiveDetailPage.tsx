import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Button,
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  TextField,
  IconButton,
  CircularProgress,
  Alert,
  Badge,
  Divider,
  useTheme,
  useMediaQuery,
  Breakpoint,
} from '@mui/material'
import Grid from '../../../components/ui/Grid'
import {
  Send as SendIcon,
  Settings as SettingsIcon,
  PlayArrow as PlayArrowIcon,
  Pause as PauseIcon,
  Refresh as RefreshIcon,
  People as PeopleIcon,
  Timer as TimerIcon,
  Chat as ChatIcon,
} from '@mui/icons-material'
import { useParams, useNavigate } from 'react-router-dom'
import { useHiveDetails } from '../../hive/hooks/useHiveDetails'
import { useHivePresence } from '../../presence/hooks/useHivePresence'
import { useTimer } from '../../timer/hooks/useTimer'
import { useHiveChat } from '../../chat/hooks/useHiveChat'

const HiveDetailPage: React.FC = () => {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md' as Breakpoint))
  const { hiveId } = useParams<{ hiveId: string }>()
  const navigate = useNavigate()

  const { hive, loading: hiveLoading, error: hiveError } = useHiveDetails()
  const { members, loading: membersLoading } = useHivePresence(hiveId!)
  const { time, isRunning, start, pause, reset } = useTimer()
  const { messages, sendMessage, error: chatError } = useHiveChat(hiveId!)
  const isConnected = !chatError // Consider connected if no error

  const [messageInput, setMessageInput] = useState('')

  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  const handleSendMessage = () => {
    if (messageInput.trim()) {
      sendMessage(messageInput.trim())
      setMessageInput('')
    }
  }

  const handleJoinHive = () => {
    // Join hive logic
    console.log('Joining hive:', hiveId)
  }

  const handleLeaveHive = () => {
    // Leave hive logic
    console.log('Leaving hive:', hiveId)
  }

  const handleSettings = () => {
    navigate(`/hive/${hiveId}/settings`)
  }

  const getPresenceColor = (status: string) => {
    switch (status) {
      case 'ONLINE':
        return 'success'
      case 'IN_FOCUS':
        return 'secondary'
      case 'AWAY':
        return 'warning'
      default:
        return 'default'
    }
  }

  if (hiveLoading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="400px"
      >
        <CircularProgress role="progressbar" />
      </Box>
    )
  }

  if (hiveError) {
    return (
      <Box p={3}>
        <Alert severity="error">
          {hiveError.message || 'Failed to load hive'}
          <Box mt={2}>
            <Button variant="outlined" onClick={() => window.location.reload()}>
              Try Again
            </Button>
          </Box>
        </Alert>
      </Box>
    )
  }

  if (!hive) {
    return null
  }

  const isFull = hive.currentMembers >= hive.maxMembers

  return (
    <Box
      data-testid="hive-detail-page"
      sx={{
        display: 'flex',
        flexDirection: isMobile ? 'column' : 'row',
        gap: 3,
        p: 3,
      }}
    >
      <Grid container spacing={3} sx={{ flex: 1 }}>
        {/* Hive Info Header */}
        <Grid xs={12}>
          <Paper data-testid="hive-info-header" sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="start">
              <Box>
                <Typography variant="h1" sx={{ fontSize: '2rem', mb: 1 }}>
                  {hive.name}
                </Typography>
                <Typography variant="body1" color="text.secondary" paragraph>
                  {hive.description}
                </Typography>
                <Box display="flex" gap={1} flexWrap="wrap" mb={2}>
                  <Chip label={hive.settings.focusMode} size="small" />
                  <Chip label={hive.settings.category} size="small" />
                  <Chip
                    icon={<PeopleIcon />}
                    label={`${hive.currentMembers} / ${hive.maxMembers} members`}
                    size="small"
                  />
                </Box>
                {/* Tags feature - not implemented yet
                <Box display="flex" gap={1} flexWrap="wrap">
                  {hive.tags?.map((tag) => (
                    <Chip key={tag} label={tag} size="small" variant="outlined" />
                  ))}
                </Box>
                */}
              </Box>
              <Box display="flex" gap={1}>
                {/* Owner settings - not implemented yet
                {hive.isOwner && (
                  <Button
                    startIcon={<SettingsIcon />}
                    variant="outlined"
                    onClick={handleSettings}
                  >
                    Settings
                  </Button>
                )}
                */}
                {false ? ( // Member check - not implemented
                  <Button variant="contained" color="error" onClick={handleLeaveHive}>
                    Leave Hive
                  </Button>
                ) : ( // Default to join button
                  <Button
                    variant="contained"
                    onClick={handleJoinHive}
                    disabled={isFull}
                  >
                    {isFull ? 'Hive Full' : 'Join Hive'}
                  </Button>
                )}
              </Box>
            </Box>
          </Paper>
        </Grid>

        {/* Main Content Area */}
        <Grid xs={12} md={8}>
          {/* Timer Section */}
          <Paper data-testid="timer-section" sx={{ p: 3, mb: 3 }}>
            <Typography variant="h2" sx={{ fontSize: '1.5rem', mb: 2 }}>
              Focus Timer
            </Typography>
            <Box
              data-testid="focus-timer"
              aria-label="Focus timer"
              display="flex"
              alignItems="center"
              gap={2}
            >
              <Typography variant="h3" sx={{ fontSize: '3rem', fontFamily: 'monospace' }}>
                {formatTime(time)}
              </Typography>
              <Box display="flex" gap={1}>
                {isRunning ? (
                  <Button
                    variant="contained"
                    startIcon={<PauseIcon />}
                    onClick={pause}
                  >
                    Pause
                  </Button>
                ) : (
                  <Button
                    variant="contained"
                    startIcon={<PlayArrowIcon />}
                    onClick={start}
                  >
                    Start
                  </Button>
                )}
                <Button
                  variant="outlined"
                  startIcon={<RefreshIcon />}
                  onClick={reset}
                >
                  Reset
                </Button>
              </Box>
            </Box>
          </Paper>

          {/* Chat Section */}
          <Paper data-testid="chat-section" sx={{ p: 3 }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
              <Typography variant="h2" sx={{ fontSize: '1.5rem' }}>
                Chat
              </Typography>
              <Chip
                data-testid="chat-connection-status"
                label={isConnected ? 'Connected' : 'Disconnected'}
                color={isConnected ? 'success' : 'error'}
                size="small"
              />
            </Box>
            <Box
              aria-label="Chat messages"
              sx={{
                height: 300,
                overflowY: 'auto',
                mb: 2,
                p: 2,
                bgcolor: 'background.default',
                borderRadius: 1,
              }}
            >
              {messages.map((msg) => (
                <Box key={msg.id} mb={2}>
                  <Typography variant="subtitle2" fontWeight="bold">
                    {msg.username}
                  </Typography>
                  <Typography variant="body2">{msg.content}</Typography>
                </Box>
              ))}
            </Box>
            <Box display="flex" gap={1}>
              <TextField
                fullWidth
                size="small"
                placeholder="Type a message..."
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyPress={(e) => {
                  if (e.key === 'Enter') {
                    handleSendMessage()
                  }
                }}
              />
              <Button
                variant="contained"
                endIcon={<SendIcon />}
                onClick={handleSendMessage}
                disabled={!messageInput.trim()}
              >
                Send
              </Button>
            </Box>
          </Paper>
        </Grid>

        {/* Sidebar - Member List */}
        <Grid xs={12} md={4}>
          <Paper
            data-testid="member-list"
            aria-label="Member list"
            sx={{ p: 3 }}
          >
            <Typography variant="h2" sx={{ fontSize: '1.5rem', mb: 2 }}>
              Members ({members.length})
            </Typography>
            <List>
              {members.map((member) => (
                <ListItem key={member.userId} alignItems="flex-start">
                  <ListItemAvatar>
                    <Badge
                      data-testid={`presence-${member.status}`}
                      overlap="circular"
                      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                      badgeContent={
                        <Box
                          sx={{
                            width: 12,
                            height: 12,
                            borderRadius: '50%',
                            bgcolor: `${getPresenceColor(member.status)}.main`,
                          }}
                        />
                      }
                    >
                      <Avatar
                        src={member.avatar}
                        alt={member.username}
                      >
                        {!member.avatar && member.username[0]}
                      </Avatar>
                    </Badge>
                  </ListItemAvatar>
                  <ListItemText
                    primary={member.username}
                    secondary={
                      <>
                        <Chip
                          label={member.status.replace('_', ' ')}
                          size="small"
                          sx={{ height: 20 }}
                        />
                        {/* focusTime feature not available yet
                        member.focusTime && (
                          <Typography variant="caption" component="span" sx={{ ml: 1 }}>
                            {member.focusTime} min
                          </Typography>
                        )*/}
                      </>
                    }
                  />
                </ListItem>
              ))}
            </List>
          </Paper>
        </Grid>
      </Grid>

      {/* Status Updates - Live Region */}
      <Box
        role="status"
        aria-live="polite"
        aria-atomic="true"
        sx={{ position: 'absolute', left: '-9999px' }}
      >
        {/* This will announce member status changes to screen readers */}
      </Box>
    </Box>
  )
}

export default HiveDetailPage