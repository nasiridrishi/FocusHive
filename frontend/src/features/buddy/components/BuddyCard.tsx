import React from 'react'
import {
  Avatar,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  LinearProgress,
  Stack,
  Typography,
  IconButton,
  Tooltip,
  Skeleton
} from '@mui/material'
import {
  AccessTime as TimeIcon,
  Chat as ChatIcon,
  EmojiEvents as TrophyIcon,
  FiberManualRecord as StatusIcon,
  Groups as GroupsIcon,
  Message as MessageIcon,
  Person as PersonIcon,
  Psychology as PsychologyIcon,
  Schedule as ScheduleIcon,
  Star as StarIcon,
  Visibility as VisibilityIcon,
  WhatshotRounded as FireIcon
} from '@mui/icons-material'
import { formatDistanceToNow } from 'date-fns'
import { BuddyMatch, BuddyRelationship } from './types'

export interface BuddyCardProps {
  buddy?: BuddyMatch | BuddyRelationship
  variant?: 'compact' | 'default' | 'full'
  currentUserId: number
  isOnline?: boolean
  status?: 'online' | 'offline' | 'away'
  lastActiveTime?: Date
  streakDays?: number
  achievements?: string[]
  isPending?: boolean
  isConnected?: boolean
  onConnect?: (userId: number) => void
  onMessage?: (userId: number) => void
  onViewProfile?: (userId: number) => void
  'data-testid'?: string
}

/**
 * BuddyCard component for displaying buddy information and actions
 *
 * Features:
 * - Display buddy's name, avatar, and bio
 * - Show online/offline/away status
 * - Display compatibility score/match percentage
 * - Show shared interests as chips
 * - Display achievements and streak information
 * - Action buttons for Connect, Message, View Profile
 * - Support for different card sizes (compact/default/full)
 * - Proper accessibility with ARIA labels and keyboard navigation
 */
export const BuddyCard: React.FC<BuddyCardProps> = ({
  buddy,
  variant = 'default',
  currentUserId,
  isOnline,
  status,
  lastActiveTime,
  streakDays,
  achievements = [],
  isPending = false,
  isConnected = false,
  onConnect,
  onMessage,
  onViewProfile,
  'data-testid': testId = 'buddy-card',
  ...props
}) => {
  // Handle undefined buddy gracefully
  if (!buddy) {
    return null
  }

  // Determine if buddy is a match or relationship
  const isBuddyMatch = 'matchScore' in buddy
  const isBuddyRelationship = 'status' in buddy && 'partnerId' in buddy

  // Extract common properties
  const userId = isBuddyMatch ? buddy.userId : (buddy as BuddyRelationship).partnerId!
  const username = isBuddyMatch ? buddy.username : (buddy as BuddyRelationship).partnerUsername!
  const avatar = isBuddyMatch ? buddy.avatar : (buddy as BuddyRelationship).partnerAvatar
  const bio = isBuddyMatch ? buddy.bio : undefined

  // Determine status
  const getStatusText = () => {
    if (status) return status === 'online' ? 'Online' : status === 'away' ? 'Away' : 'Offline'
    if (isOnline !== undefined) return isOnline ? 'Online' : 'Offline'
    if (isBuddyRelationship && (buddy as BuddyRelationship).status === 'ACTIVE') return 'Online'
    return 'Offline'
  }

  const getStatusColor = () => {
    const statusText = getStatusText().toLowerCase()
    if (statusText === 'online') return 'success'
    if (statusText === 'away') return 'warning'
    return 'default'
  }

  // Format match score
  const getMatchScore = () => {
    if (!isBuddyMatch) return null
    const score = Math.min(Math.max((buddy as BuddyMatch).matchScore, 0), 1) // Clamp between 0 and 1
    return Math.round(score * 100)
  }

  const getMatchLabel = (score: number) => {
    if (score >= 80) return 'Excellent Match'
    if (score >= 70) return 'Good Match'
    if (score >= 50) return 'Fair Match'
    return 'Poor Match'
  }

  const getMatchColor = (score: number) => {
    if (score >= 80) return 'success'
    if (score >= 70) return 'primary'
    if (score >= 50) return 'warning'
    return 'error'
  }

  // Get communication style
  const getCommunicationStyle = () => {
    if (isBuddyMatch) {
      const style = (buddy as BuddyMatch).communicationStyle?.toLowerCase()
      return style ? `${style.charAt(0).toUpperCase() + style.slice(1)} communication` : null
    }
    return null
  }

  // Get shared interests
  const getSharedInterests = () => {
    if (isBuddyMatch) {
      return (buddy as BuddyMatch).commonFocusAreas || []
    }
    return []
  }

  // Get achievement badges (limit to 3)
  const getAchievementBadges = () => {
    return achievements.slice(0, 3)
  }

  // Get last active time
  const getLastActiveText = () => {
    if (lastActiveTime) {
      return formatDistanceToNow(lastActiveTime, { addSuffix: true })
    }
    if (isBuddyRelationship) {
      const updatedAt = (buddy as BuddyRelationship).updatedAt
      if (updatedAt) {
        return formatDistanceToNow(new Date(updatedAt), { addSuffix: true })
      }
    }
    return null
  }

  // Get stats for relationships and matches
  const getStats = () => {
    if (isBuddyRelationship) {
      const relationship = buddy as BuddyRelationship
      return {
        goals: `${Math.max(relationship.completedGoals || 0, 0)}/${relationship.totalGoals || 0} goals`,
        sessions: `${relationship.totalSessions || 0} sessions`
      }
    } else if (isBuddyMatch) {
      const match = buddy as BuddyMatch
      return {
        goals: `${Math.max(match.completedGoalsCount || 0, 0)} goals`,
        sessions: null
      }
    }
    return null
  }

  // Get rating for matches
  const getAverageRating = () => {
    if (isBuddyMatch) {
      return (buddy as BuddyMatch).averageSessionRating
    }
    return null
  }

  // Get timezone overlap for full variant
  const getTimezoneOverlap = () => {
    if (isBuddyMatch && variant === 'full') {
      return `${(buddy as BuddyMatch).timezoneOverlapHours}h timezone overlap`
    }
    return null
  }

  // Handle button clicks
  const handleConnect = () => {
    if (onConnect && userId) {
      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId
      onConnect(numericUserId)
    }
  }

  const handleMessage = () => {
    if (onMessage && userId) {
      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId
      onMessage(numericUserId)
    }
  }

  const handleViewProfile = () => {
    if (onViewProfile && userId) {
      const numericUserId = typeof userId === 'string' ? parseInt(userId, 10) : userId
      onViewProfile(numericUserId)
    }
  }

  // Get button text based on state
  const getConnectButtonText = () => {
    if (isPending) return 'Pending'
    if (isConnected) return 'Connected'
    return 'Connect'
  }

  // Get avatar content
  const getAvatarContent = () => {
    if (avatar) {
      return (
        <img
          src={avatar}
          alt={`${username} avatar`}
          loading="lazy"
        />
      )
    }
    return username ? username.charAt(0).toUpperCase() : 'U'
  }

  const matchScore = getMatchScore()
  const communicationStyle = getCommunicationStyle()
  const sharedInterests = getSharedInterests()
  const achievementBadges = getAchievementBadges()
  const lastActiveText = getLastActiveText()
  const stats = getStats()
  const averageRating = getAverageRating()
  const timezoneOverlap = getTimezoneOverlap()

  return (
    <Card
      component="article"
      data-testid={testId}
      data-variant={variant}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        ...(variant === 'compact' && {
          maxHeight: 300
        })
      }}
      {...props}
    >
      <CardContent sx={{ flexGrow: 1, pb: 1 }}>
        {/* Header with Avatar and Basic Info */}
        <Stack direction="row" spacing={2} alignItems="flex-start" mb={2}>
          <Avatar
            sx={{ width: variant === 'compact' ? 40 : 56, height: variant === 'compact' ? 40 : 56 }}
          >
            {getAvatarContent()}
          </Avatar>

          <Box flexGrow={1} minWidth={0}>
            <Typography
              variant="h6"
              component="h6"
              noWrap={variant === 'compact'}
              sx={{
                fontWeight: 600,
                ...(variant === 'compact' && {
                  fontSize: '1rem'
                })
              }}
            >
              {username}
            </Typography>

            {/* Status Indicator */}
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <StatusIcon
                color={getStatusColor() as any}
                sx={{ fontSize: 12 }}
                data-testid="status-icon"
              />
              <Typography
                variant="body2"
                color="text.secondary"
                aria-label={`${getStatusText().toLowerCase()} status`}
              >
                {getStatusText()}
              </Typography>
              {!isOnline && lastActiveText && (
                <>
                  <TimeIcon sx={{ fontSize: 14, color: 'text.disabled' }} data-testid="time-icon" />
                  <Typography variant="caption" color="text.disabled">
                    {lastActiveText}
                  </Typography>
                </>
              )}
            </Stack>

            {/* Bio (hidden in compact mode or truncated) */}
            {bio && (
              <Typography
                variant="body2"
                color="text.secondary"
                className={variant === 'compact' ? 'truncated' : undefined}
                sx={{
                  ...(variant === 'compact' && {
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical'
                  })
                }}
              >
                {bio}
              </Typography>
            )}
          </Box>
        </Stack>

        {/* Match Score for BuddyMatch */}
        {matchScore !== null && (
          <Box mb={2}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1}>
              <Typography variant="body2" color="text.secondary">
                Compatibility
              </Typography>
              <Chip
                label={`${matchScore}%`}
                color={getMatchColor(matchScore) as any}
                size="small"
                sx={{ fontWeight: 600 }}
              />
            </Stack>
            <LinearProgress
              variant="determinate"
              value={matchScore}
              color={getMatchColor(matchScore) as any}
              sx={{ height: 6, borderRadius: 3 }}
            />
            <Typography variant="caption" color="text.secondary" mt={0.5} display="block">
              {getMatchLabel(matchScore)}
            </Typography>
          </Box>
        )}

        {/* Communication Style */}
        {communicationStyle && variant !== 'compact' && (
          <Box mb={2}>
            <Stack direction="row" alignItems="center" spacing={1}>
              <PsychologyIcon sx={{ fontSize: 16, color: 'text.secondary' }} data-testid="psychology-icon" />
              <Typography variant="body2" color="text.secondary">
                {communicationStyle}
              </Typography>
            </Stack>
          </Box>
        )}

        {/* Timezone Overlap (full variant only) */}
        {timezoneOverlap && (
          <Box mb={2}>
            <Stack direction="row" alignItems="center" spacing={1}>
              <ScheduleIcon sx={{ fontSize: 16, color: 'text.secondary' }} data-testid="schedule-icon" />
              <Typography variant="body2" color="text.secondary">
                {timezoneOverlap}
              </Typography>
            </Stack>
          </Box>
        )}

        {/* Shared Interests */}
        {sharedInterests.length > 0 && (
          <Box mb={2}>
            <Typography variant="body2" color="text.secondary" mb={1}>
              Shared Interests
            </Typography>
            <Stack direction="row" flexWrap="wrap" gap={0.5}>
              {sharedInterests.map((interest, index) => (
                <Chip
                  key={index}
                  label={interest}
                  size="small"
                  variant="outlined"
                  sx={{ fontSize: '0.75rem' }}
                />
              ))}
            </Stack>
          </Box>
        )}

        {/* Stats Section */}
        <Stack direction="row" spacing={2} mb={2} flexWrap="wrap">
          {/* Streak Days */}
          {streakDays && streakDays > 0 && (
            <Stack direction="row" alignItems="center" spacing={0.5}>
              <FireIcon sx={{ fontSize: 16, color: 'orange' }} data-testid="fire-icon" />
              <Typography variant="body2" color="text.secondary">
                {streakDays} day streak
              </Typography>
            </Stack>
          )}

          {/* Stats */}
          {stats && (
            <>
              {stats.goals && (
                <Stack direction="row" alignItems="center" spacing={0.5}>
                  <TrophyIcon sx={{ fontSize: 16, color: 'text.secondary' }} data-testid="trophy-icon" />
                  <Typography variant="body2" color="text.secondary">
                    {stats.goals}
                  </Typography>
                </Stack>
              )}
              {stats.sessions && (
                <Stack direction="row" alignItems="center" spacing={0.5}>
                  <GroupsIcon sx={{ fontSize: 16, color: 'text.secondary' }} data-testid="groups-icon" />
                  <Typography variant="body2" color="text.secondary">
                    {stats.sessions}
                  </Typography>
                </Stack>
              )}
            </>
          )}

          {/* Average Rating */}
          {averageRating && (
            <Stack direction="row" alignItems="center" spacing={0.5}>
              <StarIcon sx={{ fontSize: 16, color: 'gold' }} data-testid="star-icon" />
              <Typography variant="body2" color="text.secondary">
                {averageRating}
              </Typography>
            </Stack>
          )}
        </Stack>

        {/* Achievement Badges */}
        {achievementBadges.length > 0 && (
          <Box mb={2}>
            <Typography variant="body2" color="text.secondary" mb={1}>
              Achievements
            </Typography>
            <Stack direction="row" flexWrap="wrap" gap={0.5}>
              {achievementBadges.map((achievement, index) => (
                <Chip
                  key={index}
                  label={achievement}
                  size="small"
                  color="secondary"
                  sx={{ fontSize: '0.75rem' }}
                />
              ))}
            </Stack>
          </Box>
        )}
      </CardContent>

      {/* Action Buttons */}
      <CardActions sx={{ pt: 0, px: 2, pb: 2 }}>
        <Stack direction="row" spacing={1} width="100%">
          {variant !== 'compact' || !isConnected ? (
            <Button
              variant={isConnected ? "outlined" : "contained"}
              color="primary"
              onClick={handleConnect}
              disabled={isPending || !onConnect}
              aria-label={`Connect with ${username}`}
              sx={{ flexGrow: variant === 'compact' ? 1 : 0 }}
            >
              {getConnectButtonText()}
            </Button>
          ) : null}

          {variant !== 'compact' && (
            <Button
              variant="outlined"
              onClick={handleMessage}
              disabled={!onMessage}
              aria-label={`Send message to ${username}`}
              startIcon={<MessageIcon data-testid="message-icon" />}
            >
              Message
            </Button>
          )}

          <Button
            variant="text"
            onClick={handleViewProfile}
            disabled={!onViewProfile}
            aria-label={`View ${username}'s profile`}
            startIcon={<PersonIcon data-testid="person-icon" />}
            sx={{
              ...(variant === 'compact' && {
                minWidth: 'auto',
                px: 1
              })
            }}
          >
            {variant === 'compact' ? '' : 'View Profile'}
          </Button>
        </Stack>
      </CardActions>
    </Card>
  )
}

export default BuddyCard