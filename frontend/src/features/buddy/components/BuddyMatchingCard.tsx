import React, { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  CardActions,
  Typography,
  Button,
  Avatar,
  Chip,
  Grid,
  LinearProgress,
  Alert,
  Stack,
  Divider,
  IconButton,
  Tooltip
} from '@mui/material'
import {
  PersonAdd as PersonAddIcon,
  Refresh as RefreshIcon,
  Star as StarIcon,
  AccessTime as TimeIcon,
  Chat as ChatIcon,
  Psychology as PsychologyIcon
} from '@mui/icons-material'
import { buddyApi } from '../services/buddyApi'
import { BuddyMatch, BuddyRequest } from '../types'

interface BuddyMatchingCardProps {
  onMatchFound?: () => void
}

const BuddyMatchingCard: React.FC<BuddyMatchingCardProps> = ({ onMatchFound }) => {
  const [matches, setMatches] = useState<BuddyMatch[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [sendingRequest, setSendingRequest] = useState<number | null>(null)
  const [sentRequests, setSentRequests] = useState<Set<number>>(new Set())

  useEffect(() => {
    loadMatches()
  }, [])

  const loadMatches = async () => {
    try {
      setLoading(true)
      setError(null)
      const matchData = await buddyApi.findPotentialMatches()
      setMatches(matchData)
    } catch (err) {
      setError('Failed to load potential matches')
    } finally {
      setLoading(false)
    }
  }

  const handleSendRequest = async (match: BuddyMatch) => {
    try {
      setSendingRequest(match.userId)
      const request: BuddyRequest = {
        toUserId: match.userId,
        message: `Hi ${match.username}! I think we'd make great accountability buddies based on our shared interests.`
      }
      await buddyApi.sendBuddyRequest(request)
      setSentRequests(prev => new Set(prev).add(match.userId))
      if (onMatchFound) {
        onMatchFound()
      }
    } catch (err) {
      setError('Failed to send buddy request')
    } finally {
      setSendingRequest(null)
    }
  }

  const getMatchScoreColor = (score: number): "error" | "warning" | "success" => {
    if (score >= 0.8) return "success"
    if (score >= 0.6) return "warning"
    return "error"
  }

  const getMatchScoreLabel = (score: number): string => {
    if (score >= 0.8) return "Excellent Match"
    if (score >= 0.6) return "Good Match"
    if (score >= 0.4) return "Fair Match"
    return "Poor Match"
  }

  const getCommunicationIcon = (style: string) => {
    switch (style) {
      case 'FREQUENT':
        return <ChatIcon fontSize="small" />
      case 'MODERATE':
        return <TimeIcon fontSize="small" />
      case 'MINIMAL':
        return <PsychologyIcon fontSize="small" />
      default:
        return <ChatIcon fontSize="small" />
    }
  }

  if (loading) {
    return (
      <Box sx={{ p: 3 }}>
        <LinearProgress />
        <Typography variant="body2" color="textSecondary" sx={{ mt: 2, textAlign: 'center' }}>
          Finding your perfect buddy matches...
        </Typography>
      </Box>
    )
  }

  if (error) {
    return (
      <Alert 
        severity="error" 
        action={
          <Button color="inherit" size="small" onClick={loadMatches}>
            Retry
          </Button>
        }
      >
        {error}
      </Alert>
    )
  }

  if (matches.length === 0) {
    return (
      <Box textAlign="center" py={4}>
        <Typography variant="h6" gutterBottom>
          No matches found
        </Typography>
        <Typography variant="body2" color="textSecondary" paragraph>
          We couldn't find any potential buddies at this time. Please check back later or update your preferences.
        </Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={loadMatches}
        >
          Refresh Matches
        </Button>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h6">
          Potential Buddy Matches
        </Typography>
        <IconButton onClick={loadMatches} color="primary">
          <RefreshIcon />
        </IconButton>
      </Box>

      <Grid container spacing={3}>
        {matches.map((match) => (
          <Grid item xs={12} md={6} lg={4} key={match.userId}>
            <Card elevation={2}>
              <CardContent>
                {/* Header with Avatar and Score */}
                <Box display="flex" alignItems="center" mb={2}>
                  <Avatar 
                    src={match.avatar} 
                    sx={{ width: 60, height: 60, mr: 2 }}
                  >
                    {match.username[0].toUpperCase()}
                  </Avatar>
                  <Box flexGrow={1}>
                    <Typography variant="h6">
                      {match.username}
                    </Typography>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Chip
                        size="small"
                        icon={<StarIcon />}
                        label={`${Math.round(match.matchScore * 100)}%`}
                        color={getMatchScoreColor(match.matchScore)}
                      />
                      <Typography variant="caption" color="textSecondary">
                        {getMatchScoreLabel(match.matchScore)}
                      </Typography>
                    </Box>
                  </Box>
                </Box>

                {/* Bio */}
                {match.bio && (
                  <Typography variant="body2" color="textSecondary" paragraph>
                    {match.bio}
                  </Typography>
                )}

                <Divider sx={{ my: 2 }} />

                {/* Match Details */}
                <Stack spacing={1.5}>
                  {/* Common Focus Areas */}
                  {match.commonFocusAreas.length > 0 && (
                    <Box>
                      <Typography variant="caption" color="textSecondary" gutterBottom>
                        Common Interests
                      </Typography>
                      <Box display="flex" flexWrap="wrap" gap={0.5} mt={0.5}>
                        {match.commonFocusAreas.map((area) => (
                          <Chip
                            key={area}
                            label={area}
                            size="small"
                            variant="outlined"
                          />
                        ))}
                      </Box>
                    </Box>
                  )}

                  {/* Communication Style */}
                  <Box display="flex" alignItems="center" gap={1}>
                    {getCommunicationIcon(match.communicationStyle)}
                    <Typography variant="body2">
                      {match.communicationStyle.toLowerCase()} communication
                    </Typography>
                  </Box>

                  {/* Timezone Overlap */}
                  {match.timezoneOverlapHours > 0 && (
                    <Box display="flex" alignItems="center" gap={1}>
                      <TimeIcon fontSize="small" />
                      <Typography variant="body2">
                        {match.timezoneOverlapHours}h timezone overlap
                      </Typography>
                    </Box>
                  )}

                  {/* Stats */}
                  <Box display="flex" justifyContent="space-between" mt={1}>
                    <Tooltip title="Active buddy relationships">
                      <Chip
                        size="small"
                        label={`${match.activeBuddyCount} buddies`}
                        variant="outlined"
                      />
                    </Tooltip>
                    <Tooltip title="Completed goals">
                      <Chip
                        size="small"
                        label={`${match.completedGoalsCount} goals`}
                        variant="outlined"
                      />
                    </Tooltip>
                    {match.averageSessionRating && (
                      <Tooltip title="Average session rating">
                        <Chip
                          size="small"
                          icon={<StarIcon />}
                          label={match.averageSessionRating.toFixed(1)}
                          variant="outlined"
                        />
                      </Tooltip>
                    )}
                  </Box>
                </Stack>
              </CardContent>

              <CardActions>
                <Button
                  fullWidth
                  variant="contained"
                  startIcon={<PersonAddIcon />}
                  onClick={() => handleSendRequest(match)}
                  disabled={sendingRequest === match.userId || sentRequests.has(match.userId)}
                >
                  {sentRequests.has(match.userId) ? 'Request Sent' : 'Send Buddy Request'}
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}

export default BuddyMatchingCard