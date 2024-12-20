import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  alpha,
  Avatar,
  Box,
  Button,
  Chip,
  Collapse,
  IconButton,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Popover,
  Skeleton,
  styled,
  Typography,
  useTheme,
} from '@mui/material'
import {
  ExpandMore as ExpandMoreIcon,
  Person as PersonIcon,
  Refresh as RefreshIcon,
  Computer as ComputerIcon,
} from '@mui/icons-material'
import { usePresence } from '../../../shared/contexts/PresenceContext'
import { UserPresence, PresenceStatus } from '../../../shared/types/presence'
import { User } from '../../../shared/types/auth'

const StyledContainer = styled(Box)(({ theme }) => ({
  padding: theme.spacing(2),
  borderRadius: theme.shape.borderRadius,
  backgroundColor: theme.palette.background.paper,
  border: `1px solid ${theme.palette.divider}`,
  '&.presence-panel--compact': {
    padding: theme.spacing(1),
  },
}))

const StatusBadge = styled(Chip)<{ status: PresenceStatus }>(({ theme, status }) => {
  let backgroundColor: string
  let color: string

  switch (status) {
    case 'focusing':
      backgroundColor = theme.palette.success.main
      color = theme.palette.success.contrastText
      break
    case 'online':
      backgroundColor = theme.palette.primary.main
      color = theme.palette.primary.contrastText
      break
    case 'break':
      backgroundColor = theme.palette.warning.main
      color = theme.palette.warning.contrastText
      break
    case 'away':
      backgroundColor = theme.palette.grey[500]
      color = theme.palette.grey[50]
      break
    case 'offline':
      backgroundColor = theme.palette.grey[300]
      color = theme.palette.grey[700]
      break
    default:
      backgroundColor = theme.palette.grey[300]
      color = theme.palette.grey[700]
  }

  return {
    backgroundColor,
    color,
    fontSize: '0.75rem',
    height: 20,
    '&[data-high-contrast="true"]': {
      border: `1px solid ${theme.palette.common.black}`,
    },
  }
})

const UserListItem = styled(ListItem)(({ theme }) => ({
  cursor: 'pointer',
  borderRadius: theme.shape.borderRadius,
  marginBottom: theme.spacing(0.5),
  '&:hover': {
    backgroundColor: theme.palette.action.hover,
  },
  '&:focus': {
    backgroundColor: theme.palette.action.focus,
    outline: `2px solid ${theme.palette.primary.main}`,
  },
}))

interface HivePresencePanelProps {
  hiveId: string
  onUserClick?: (user: User) => void
  onPresenceUpdate?: (presenceInfo: { hiveId: string; activeUsers: UserPresence[]; totalOnline: number }) => void
  compact?: boolean
  showActivityText?: boolean
  maxVisibleUsers?: number
  enableCollapse?: boolean
  isLoading?: boolean
  onError?: (error: Error) => void
  className?: string
}

const HivePresencePanel: React.FC<HivePresencePanelProps> = ({
  hiveId,
  onUserClick,
  onPresenceUpdate,
  compact = false,
  showActivityText = true,
  maxVisibleUsers = 10,
  enableCollapse = true,
  isLoading = false,
  onError,
  className,
}) => {
  const theme = useTheme()
  const [expanded, setExpanded] = useState(true)
  const [hoveredUser, setHoveredUser] = useState<UserPresence | null>(null)
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [hasError, setHasError] = useState(false)
  const [focusedIndex, setFocusedIndex] = useState(0)

  const { hivePresence, currentPresence, joinHivePresence } = usePresence()
  const presenceInfo = hivePresence[hiveId]

  // If no presence info and not loading, show empty state
  if (!presenceInfo && !isLoading) {
    return (
      <StyledContainer
        className={className}
        data-testid="hive-presence-panel"
        role="region"
        aria-label="Hive presence panel"
      >
        {compact ? (
          <Typography variant="body2" color="text.secondary">
            0 active
          </Typography>
        ) : (
          <>
            <Typography
              variant="h6"
              data-testid="presence-panel-title"
              role="heading"
              aria-level={3}
            >
              Online (0)
            </Typography>
            <Box data-testid="presence-empty-state" sx={{ textAlign: 'center', py: 3 }}>
              <PersonIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
              <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
                No one is online right now
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Be the first to join this hive!
              </Typography>
            </Box>
          </>
        )}
      </StyledContainer>
    )
  }

  const activeUsers = presenceInfo?.activeUsers || []
  const totalOnline = presenceInfo?.totalOnline || 0
  const totalFocusing = presenceInfo?.totalFocusing || 0
  const totalOnBreak = presenceInfo?.totalOnBreak || 0

  // Sort users by status priority (focusing > online > break > away > offline)
  const sortedUsers = useMemo(() => {
    return [...activeUsers].sort((a, b) => {
      const statusPriority = {
        focusing: 4,
        online: 3,
        break: 2,
        away: 1,
        offline: 0,
      }
      return (statusPriority[b.status] || 0) - (statusPriority[a.status] || 0)
    })
  }, [activeUsers])

  const visibleUsers = useMemo(() => {
    return sortedUsers.slice(0, maxVisibleUsers)
  }, [sortedUsers, maxVisibleUsers])

  const remainingUsersCount = Math.max(0, sortedUsers.length - maxVisibleUsers)

  // Handle presence updates
  useEffect(() => {
    if (presenceInfo && onPresenceUpdate) {
      const debounceTimer = setTimeout(() => {
        onPresenceUpdate({
          hiveId,
          activeUsers: presenceInfo.activeUsers,
          totalOnline: presenceInfo.totalOnline,
        })
      }, 100)

      return () => clearTimeout(debounceTimer)
    }
  }, [presenceInfo, onPresenceUpdate, hiveId])

  // Error handling
  useEffect(() => {
    try {
      if (!presenceInfo && !isLoading) {
        setHasError(false) // Reset error when data is available
      }
    } catch (error) {
      setHasError(true)
      onError?.(error as Error)
    }
  }, [presenceInfo, isLoading, onError])

  const handleRetry = useCallback(() => {
    setHasError(false)
    joinHivePresence(hiveId)
  }, [hiveId, joinHivePresence])

  const handleUserClick = useCallback((user: UserPresence) => {
    onUserClick?.(user.user)
  }, [onUserClick])

  const handleUserHover = useCallback((user: UserPresence, event: React.MouseEvent<HTMLElement>) => {
    setHoveredUser(user)
    setAnchorEl(event.currentTarget)
  }, [])

  const handleUserLeave = useCallback(() => {
    setHoveredUser(null)
    setAnchorEl(null)
  }, [])

  const formatSessionDuration = useCallback((startTime: string): string => {
    const start = new Date(startTime)
    const now = new Date()
    const diffMs = now.getTime() - start.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))

    if (diffMinutes < 60) return `${diffMinutes}m`
    const hours = Math.floor(diffMinutes / 60)
    const minutes = diffMinutes % 60
    return `${hours}h ${minutes}m`
  }, [])

  const getInitials = useCallback((name: string): string => {
    return name
      .split(' ')
      .map(part => part.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2)
  }, [])

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (!visibleUsers.length) return

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault()
        setFocusedIndex(prev => {
          const newIndex = Math.min(prev + 1, visibleUsers.length - 1)
          // Focus the element after state update
          setTimeout(() => {
            const element = document.querySelector(`[data-testid="presence-user-${visibleUsers[newIndex]?.user.username}"]`) as HTMLElement
            element?.focus()
          }, 0)
          return newIndex
        })
        break
      case 'ArrowUp':
        event.preventDefault()
        setFocusedIndex(prev => {
          const newIndex = Math.max(prev - 1, 0)
          // Focus the element after state update
          setTimeout(() => {
            const element = document.querySelector(`[data-testid="presence-user-${visibleUsers[newIndex]?.user.username}"]`) as HTMLElement
            element?.focus()
          }, 0)
          return newIndex
        })
        break
      case 'Enter':
        event.preventDefault()
        if (visibleUsers[focusedIndex]) {
          handleUserClick(visibleUsers[focusedIndex])
        }
        break
    }
  }, [visibleUsers, focusedIndex, handleUserClick])

  const renderUserAvatar = useCallback((user: UserPresence) => {
    if (user.user.avatar || user.user.profilePicture) {
      return (
        <Avatar
          src={user.user.avatar || user.user.profilePicture}
          alt={user.user.name}
          sx={{ width: 40, height: 40 }}
        />
      )
    }

    return (
      <Avatar
        data-testid="user-avatar-fallback"
        sx={{ width: 40, height: 40, bgcolor: theme.palette.primary.main }}
      >
        {getInitials(user.user.name)}
      </Avatar>
    )
  }, [theme.palette.primary.main, getInitials])

  const renderStatusBadge = useCallback((user: UserPresence) => {
    const statusClass = `status-badge--${user.status}`

    return (
      <StatusBadge
        status={user.status}
        data-testid={`status-${user.status}`}
        className={statusClass}
        data-high-contrast="true"
        label={user.status}
        size="small"
      />
    )
  }, [])

  const renderLoadingState = () => (
    <Box data-testid="presence-loading">
      {Array.from({ length: 3 }).map((_, index) => (
        <Box key={index} data-testid="presence-user-skeleton" sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Skeleton variant="circular" width={40} height={40} sx={{ mr: 2 }} />
          <Box sx={{ flex: 1 }}>
            <Skeleton variant="text" width="60%" />
            <Skeleton variant="text" width="40%" />
          </Box>
        </Box>
      ))}
    </Box>
  )

  const renderErrorState = () => (
    <Box data-testid="presence-error-state" sx={{ textAlign: 'center', py: 2 }}>
      <Typography variant="body2" color="error" sx={{ mb: 2 }}>
        Failed to load presence data
      </Typography>
      <Button startIcon={<RefreshIcon />} onClick={handleRetry} size="small">
        Retry
      </Button>
    </Box>
  )

  const renderEmptyState = () => (
    <Box data-testid="presence-empty-state" sx={{ textAlign: 'center', py: 3 }}>
      <PersonIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
      <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
        No one is online right now
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Be the first to join this hive!
      </Typography>
    </Box>
  )

  // Compact layout
  if (compact) {
    return (
      <StyledContainer
        className={`${className || ''} presence-panel--compact`.trim()}
        data-testid="hive-presence-panel"
        role="region"
        aria-label="Hive presence panel"
      >
        <Typography variant="body2" color="text.secondary">
          {totalOnline} active
        </Typography>
        {totalFocusing > 0 && (
          <Chip
            label={`${totalFocusing} focusing`}
            size="small"
            color="primary"
            variant="outlined"
          />
        )}
      </StyledContainer>
    )
  }

  return (
    <StyledContainer
      className={className}
      data-testid="hive-presence-panel"
      role="region"
      aria-label="Hive presence panel"
    >
      {/* Title */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
        <Typography
          variant="h6"
          data-testid="presence-panel-title"
          role="heading"
          aria-level={3}
        >
          Online ({totalOnline})
        </Typography>

        {enableCollapse && (
          <IconButton
            data-testid="collapse-toggle"
            onClick={() => setExpanded(!expanded)}
            aria-expanded={expanded}
            aria-controls="presence-user-list"
            aria-label={expanded ? 'Collapse presence panel' : 'Expand presence panel'}
            size="small"
          >
            <ExpandMoreIcon
              sx={{
                transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: 'transform 0.2s',
              }}
            />
          </IconButton>
        )}
      </Box>

      {/* Content */}
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        {isLoading ? (
          renderLoadingState()
        ) : hasError ? (
          renderErrorState()
        ) : totalOnline === 0 ? (
          renderEmptyState()
        ) : (
          <List
            data-testid="presence-user-list"
            role="list"
            id="presence-user-list"
            onKeyDown={handleKeyDown}
            sx={{ py: 0 }}
          >
            {/* User list */}
            {visibleUsers.map((user, index) => (
              <UserListItem
                key={user.userId}
                data-testid={`presence-user-${user.user.username}`}
                role="listitem"
                tabIndex={0}
                onClick={() => handleUserClick(user)}
                onMouseEnter={(e) => handleUserHover(user, e)}
                onMouseLeave={handleUserLeave}
                onFocus={() => setFocusedIndex(index)}
                sx={{
                  backgroundColor: focusedIndex === index ? theme.palette.action.focus : 'transparent',
                }}
              >
                <ListItemAvatar>
                  {renderUserAvatar(user)}
                </ListItemAvatar>

                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="subtitle2">
                        {user.user.name}
                      </Typography>
                      {renderStatusBadge(user)}
                      {user.status === 'focusing' && user.sessionStartTime && (
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          data-testid="session-duration"
                        >
                          {formatSessionDuration(user.sessionStartTime)}
                        </Typography>
                      )}
                      {user.deviceInfo && (
                        <ComputerIcon
                          data-testid={`device-${user.deviceInfo.type}`}
                          fontSize="small"
                          color="action"
                        />
                      )}
                    </Box>
                  }
                  secondary={
                    showActivityText && user.currentActivity ? (
                      <Typography variant="caption" color="text.secondary">
                        {user.currentActivity}
                      </Typography>
                    ) : null
                  }
                />
              </UserListItem>
            ))}

            {/* More users indicator */}
            {remainingUsersCount > 0 && (
              <ListItem data-testid="more-users-indicator-container">
                <ListItemText>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    data-testid="more-users-indicator"
                  >
                    and {remainingUsersCount} more
                  </Typography>
                </ListItemText>
              </ListItem>
            )}

            {/* Virtual list container for large lists */}
            {sortedUsers.length > 50 && (
              <Box data-testid="virtual-list-container" sx={{ display: 'none' }} />
            )}
          </List>
        )}
      </Collapse>

      {/* Profile preview popover */}
      <Popover
        open={Boolean(anchorEl && hoveredUser)}
        anchorEl={anchorEl}
        onClose={handleUserLeave}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        sx={{ pointerEvents: 'none' }}
      >
        {hoveredUser && (
          <Box data-testid="user-profile-preview" sx={{ p: 2, maxWidth: 300 }}>
            <Typography variant="subtitle2">{hoveredUser.user.name}</Typography>
            <Typography variant="body2" color="text.secondary">
              {hoveredUser.user.email}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Joined: {new Date(hoveredUser.user.createdAt).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric',
              })}
            </Typography>
          </Box>
        )}
      </Popover>

      {/* Status announcer for screen readers */}
      <Box
        data-testid="status-announcer"
        sx={{
          position: 'absolute',
          left: -10000,
          width: 1,
          height: 1,
          overflow: 'hidden',
        }}
        aria-live="polite"
        aria-atomic="true"
      />
    </StyledContainer>
  )
}

export default React.memo(HivePresencePanel)