import React from 'react'
import {
  Box,
  Card,
  CardContent,
  CardActions,
  CardMedia,
  Typography,
  Chip,
  Button,
  Avatar,
  AvatarGroup,
  Stack,
  LinearProgress,
  Skeleton,
  useTheme,
  IconButton,
  Tooltip,
} from '@mui/material'
import {
  People as PeopleIcon,
  Lock as LockIcon,
  Public as PublicIcon,
  Mail as MailIcon,
  School as SchoolIcon,
  Work as WorkIcon,
  Group as GroupIcon,
  Code as CodeIcon,
  PersonAdd as PersonAddIcon,
  ExitToApp as ExitToAppIcon,
  Launch as LaunchIcon,
  Timer as TimerIcon,
} from '@mui/icons-material'
import { Hive as SharedHive, HiveMember } from '@shared/types'

// Extend the shared Hive type with local properties
interface Hive extends Omit<SharedHive, 'settings'> {
  settings: {
    privacyLevel: 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY'
    category: 'STUDY' | 'WORK' | 'SOCIAL' | 'CODING'
    focusMode: 'POMODORO' | 'TIMEBLOCK' | 'FREEFORM'
    maxParticipants: number
    [key: string]: any
  }
  status?: 'ACTIVE' | 'INACTIVE'
  members?: Array<{ userId: string; joinedAt: string }>
  statistics?: {
    totalSessions: number
    totalFocusTime: number
    averageRating: number
    weeklyActiveUsers: number
  }
  nextSession?: any
  imageUrl?: string
}

export type { Hive }

export interface HiveCardProps {
  hive: Hive
  members?: any[] // Optional members array
  onJoin?: (hiveId: string, message?: string) => void
  onLeave?: (hiveId: string) => void
  onView?: (hiveId: string) => void
  onEnter?: (hiveId: string) => void
  onSettings?: (hiveId: string) => void
  onShare?: (hiveId: string) => void
  isLoading?: boolean
  compact?: boolean
  variant?: 'default' | 'compact' | string
  currentUserId?: string
}

const getCategoryIcon = (category: string) => {
  switch (category) {
    case 'STUDY':
      return <SchoolIcon />
    case 'WORK':
      return <WorkIcon />
    case 'SOCIAL':
      return <GroupIcon />
    case 'CODING':
      return <CodeIcon />
    default:
      return <GroupIcon />
  }
}

const getPrivacyIcon = (privacyLevel: string) => {
  switch (privacyLevel) {
    case 'PUBLIC':
      return <PublicIcon />
    case 'PRIVATE':
      return <LockIcon />
    case 'INVITE_ONLY':
      return <MailIcon />
    default:
      return <PublicIcon />
  }
}

export const HiveCard: React.FC<HiveCardProps> = ({
  hive,
  onJoin,
  onLeave,
  onView,
  isLoading = false,
  compact = false,
  currentUserId,
}) => {
  const theme = useTheme()
  const memberCount = hive.members?.length || 0
  const isMember = Boolean(
    currentUserId && hive.members?.some((m) => m.userId === currentUserId)
  )
  const isFull = memberCount >= hive.settings.maxParticipants
  const progressPercentage = (memberCount / hive.settings.maxParticipants) * 100

  const handleCardClick = (e: React.MouseEvent) => {
    // Don't trigger onView if clicking on action buttons
    const target = e.target as HTMLElement
    if (
      target.closest('button') ||
      target.closest('[role="button"]')
    ) {
      return
    }
    onView?.(hive.id)
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onView?.(hive.id)
    }
  }

  // Loading state
  if (isLoading) {
    return (
      <Card data-testid="hive-card-skeleton">
        <CardContent>
          <Skeleton variant="rectangular" height={200} sx={{ mb: 2 }} />
          <Skeleton variant="text" width="60%" height={32} sx={{ mb: 1 }} />
          <Skeleton variant="text" width="100%" />
          <Skeleton variant="text" width="80%" />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card
      data-testid="hive-card"
      role="article"
      className={compact ? 'hive-card--compact' : ''}
      onClick={handleCardClick}
      onKeyPress={handleKeyPress}
      tabIndex={0}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
        '&:hover': {
          boxShadow: 3,
          transform: 'translateY(-2px)',
          '& .view-details-text': {
            opacity: 1,
          },
        },
      }}
    >
      {/* Image or Placeholder */}
      {hive.imageUrl ? (
        <CardMedia
          component="img"
          height="200"
          image={hive.imageUrl}
          alt={hive.name}
          sx={{ objectFit: 'cover' }}
          role="img"
        />
      ) : (
        <Box
          data-testid="default-hive-image"
          sx={{
            height: 200,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'grey.200',
          }}
        >
          <GroupIcon sx={{ fontSize: 60, color: 'grey.500' }} />
        </Box>
      )}

      {/* Status Badge */}
      {hive.status === 'ACTIVE' ? (
        <Chip
          label="Active"
          color="success"
          size="small"
          data-testid="active-indicator"
          sx={{
            position: 'absolute',
            top: 10,
            right: 10,
          }}
        />
      ) : (
        <Chip
          label="Inactive"
          color="default"
          size="small"
          data-testid="inactive-indicator"
          sx={{
            position: 'absolute',
            top: 10,
            right: 10,
          }}
        />
      )}

      <CardContent sx={{ flex: 1 }}>
        {/* Member Badge */}
        {isMember && (
          <Chip
            label="Member"
            color="primary"
            size="small"
            data-testid="member-badge"
            sx={{ mb: 1 }}
          />
        )}

        {/* Title */}
        <Typography variant="h6" component="h2" gutterBottom>
          {hive.name}
        </Typography>

        {/* Description */}
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            mb: 2,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
          }}
        >
          {hive.description}
        </Typography>

        {/* Tags */}
        {!compact && hive.tags && hive.tags.length > 0 && (
          <Stack
            direction="row"
            spacing={0.5}
            sx={{ flexWrap: 'wrap', gap: 0.5, mb: 2 }}
            data-testid="hive-tags"
          >
            {hive.tags.slice(0, 3).map((tag) => (
              <Chip
                key={tag}
                label={tag}
                size="small"
                variant="outlined"
              />
            ))}
          </Stack>
        )}

        {/* Metadata */}
        <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
          <Chip
            icon={getCategoryIcon(hive.settings.category)}
            label={hive.settings.category}
            size="small"
            color="primary"
            data-testid="category-chip"
          />
          <Chip
            icon={<TimerIcon />}
            label={hive.settings.focusMode}
            size="small"
            color="secondary"
            data-testid="focus-mode-chip"
          />
          <Chip
            icon={getPrivacyIcon(hive.settings.privacyLevel) as React.ReactElement}
            label={hive.settings.privacyLevel}
            size="small"
            color="warning"
            variant="outlined"
            data-testid="privacy-badge"
          />
        </Stack>

        {/* Privacy Icon for specific types */}
        {(hive.settings.privacyLevel === 'PRIVATE' ||
          hive.settings.privacyLevel === 'INVITE_ONLY') && (
          <Box sx={{ display: 'none' }}>
            <span data-testid="privacy-icon" />
          </Box>
        )}

        {/* Members */}
        <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
          <AvatarGroup max={4}>
            {hive.members?.slice(0, 4).map((member) => (
              <Avatar key={member.userId} sx={{ width: 32, height: 32 }}>
                {member.userId.charAt(0).toUpperCase()}
              </Avatar>
            ))}
          </AvatarGroup>
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid="member-count"
            aria-label={`${memberCount} out of ${hive.settings.maxParticipants} members`}
          >
            {memberCount}/{hive.settings.maxParticipants}
          </Typography>
        </Box>

        {/* Progress Bar */}
        <LinearProgress
          variant="determinate"
          value={progressPercentage}
          sx={{ mt: 1, height: 6, borderRadius: 1 }}
          color={isFull ? 'error' : 'primary'}
          role="progressbar"
          aria-valuenow={progressPercentage}
        />

        {/* Owner Info */}
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ mt: 1, display: 'block' }}
          data-testid="owner-info"
        >
          Created by {hive.ownerId}
        </Typography>

        {/* View Details Text (shown on hover) */}
        <Typography
          className="view-details-text"
          variant="caption"
          color="primary"
          sx={{
            mt: 1,
            display: 'block',
            opacity: 0,
            transition: 'opacity 0.3s ease',
          }}
        >
          View Details
        </Typography>
      </CardContent>

      <CardActions>
        {isMember ? (
          <Button
            variant="outlined"
            color="error"
            startIcon={<ExitToAppIcon />}
            onClick={(e) => {
              e.stopPropagation()
              onLeave?.(hive.id)
            }}
            fullWidth
          >
            Leave
          </Button>
        ) : (
          <Button
            variant="contained"
            color="primary"
            startIcon={<PersonAddIcon />}
            onClick={(e) => {
              e.stopPropagation()
              onJoin?.(hive.id)
            }}
            disabled={isFull}
            fullWidth
          >
            {isFull ? 'Full' : 'Join'}
          </Button>
        )}
      </CardActions>
    </Card>
  )
}

export default HiveCard