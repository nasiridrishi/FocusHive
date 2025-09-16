import React, {useState} from 'react'
import {
  alpha,
  Avatar,
  AvatarGroup,
  Box,
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Popover,
  styled,
  Typography,
  useTheme,
} from '@mui/material'
import {PresenceAvatar} from './PresenceIndicator'
import UserStatusBadge, {StatusDot} from './UserStatusBadge'
import {PresenceStatus, UserPresence} from '../../../shared/types/presence'

const StyledAvatarGroup = styled(AvatarGroup)(({theme}) => ({
  '& .MuiAvatar-root': {
    border: `2px solid ${theme.palette.background.paper}`,
    transition: 'all 0.2s ease-in-out',
    cursor: 'pointer',
    '&:hover': {
      transform: 'scale(1.1)',
      zIndex: 10,
    },
  },
}))

const UserDetailCard = styled(Card)(() => ({
  minWidth: 300,
  maxWidth: 400,
}))

const StatusSummary = styled(Box)(({theme}) => ({
  display: 'flex',
  gap: theme.spacing(1),
  alignItems: 'center',
  marginBottom: theme.spacing(1),
}))

interface ActiveUsersListProps {
  users: UserPresence[]
  maxVisible?: number
  size?: 'small' | 'medium' | 'large'
  showStatusSummary?: boolean
  showUserDetails?: boolean
  onUserClick?: (user: UserPresence) => void
  title?: string
  className?: string
}

const ActiveUsersList: React.FC<ActiveUsersListProps> = ({
                                                           users,
                                                           maxVisible = 5,
                                                           size = 'medium',
                                                           showStatusSummary = true,
                                                           showUserDetails = true,
                                                           onUserClick,
                                                           title = 'Active Users',
                                                           className,
                                                         }) => {
  const theme = useTheme()
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const [selectedUsers, setSelectedUsers] = useState<UserPresence[]>([])

  const avatarSize = size === 'small' ? 32 : size === 'medium' ? 40 : 48

  // Calculate status summary
  const statusCounts = users.reduce((acc, user) => {
    acc[user.status] = (acc[user.status] || 0) + 1
    return acc
  }, {} as Record<PresenceStatus, number>)


  const handleAvatarClick = (event: React.MouseEvent<HTMLElement>, clickedUsers: UserPresence[]): void => {
    if (!showUserDetails) {
      return
    }

    setAnchorEl(event.currentTarget)
    setSelectedUsers(clickedUsers)
  }

  const handleClose = (): void => {
    setAnchorEl(null)
    setSelectedUsers([])
  }

  const formatLastSeen = (lastSeenStr: string): string => {
    const lastSeenDate = new Date(lastSeenStr)
    const now = new Date()
    const diffMs = now.getTime() - lastSeenDate.getTime()
    const diffMinutes = Math.floor(diffMs / (1000 * 60))

    if (diffMinutes < 1) return 'Just now'
    if (diffMinutes < 60) return `${diffMinutes}m ago`

    const diffHours = Math.floor(diffMinutes / 60)
    if (diffHours < 24) return `${diffHours}h ago`

    const diffDays = Math.floor(diffHours / 24)
    return `${diffDays}d ago`
  }

  const renderStatusSummary = (): React.ReactElement | null => {
    if (!showStatusSummary) return null

    return (
        <StatusSummary>
          {Object.entries(statusCounts).map(([status, count]) => (
              <Box key={status} sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                <StatusDot status={status as PresenceStatus} size={6} showTooltip={false}/>
                <Typography variant="caption" color="text.secondary">
                  {count}
                </Typography>
              </Box>
          ))}
        </StatusSummary>
    )
  }

  const renderUserDetail = (user: UserPresence) => (
      <ListItem key={user.userId} sx={{py: 1}}>
        <ListItemAvatar>
          <PresenceAvatar
              status={user.status}
              src={user.user.avatar}
              name={user.user.name}
              size={32}
              showAnimation={false}
          />
        </ListItemAvatar>
        <ListItemText
            primary={
              <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                <Typography variant="subtitle2">
                  {user.user.name}
                </Typography>
                <UserStatusBadge
                    status={user.status}
                    currentActivity={user.currentActivity}
                    variant="compact"
                    showTooltip={false}
                />
              </Box>
            }
            secondary={
              <Box>
                {user.currentActivity && (
                    <Typography variant="caption" color="text.secondary" sx={{fontStyle: 'italic'}}>
                      {user.currentActivity}
                    </Typography>
                )}
                {user.status !== 'online' && (
                    <Typography variant="caption" color="text.secondary" display="block">
                      {formatLastSeen(user.lastSeen)}
                    </Typography>
                )}
              </Box>
            }
        />
      </ListItem>
  )

  if (users.length === 0) {
    return (
        <Box className={className}>
          <Typography variant="body2" color="text.secondary" textAlign="center">
            No active users
          </Typography>
        </Box>
    )
  }

  const visibleUsers = users.slice(0, maxVisible)
  const remainingUsers = users.slice(maxVisible)

  return (
      <Box className={className}>
        {title && (
            <Typography variant="subtitle2" sx={{mb: 1, fontWeight: 600}}>
              {title} ({users.length})
            </Typography>
        )}

        {renderStatusSummary()}

        <StyledAvatarGroup
            max={maxVisible + 1}
            sx={{
              '& .MuiAvatar-root': {
                width: avatarSize,
                height: avatarSize,
              },
            }}
        >
          {visibleUsers.map((user) => (
              <PresenceAvatar
                  key={user.userId}
                  status={user.status}
                  src={user.user.avatar}
                  name={user.user.name}
                  size={avatarSize}
                  onClick={() => {
                    if (onUserClick) {
                      onUserClick(user)
                    }
                  }}
              />
          ))}

          {remainingUsers.length > 0 && (
              <Avatar
                  sx={{
                    bgcolor: alpha(theme.palette.primary.main, 0.1),
                    color: theme.palette.primary.main,
                    border: `2px solid ${theme.palette.background.paper}`,
                    cursor: 'pointer',
                    '&:hover': {
                      bgcolor: alpha(theme.palette.primary.main, 0.2),
                      transform: 'scale(1.1)',
                    },
                  }}
                  onClick={(e) => handleAvatarClick(e, remainingUsers)}
              >
                +{remainingUsers.length}
              </Avatar>
          )}
        </StyledAvatarGroup>

        <Popover
            open={Boolean(anchorEl)}
            anchorEl={anchorEl}
            onClose={handleClose}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'center',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'center',
            }}
            sx={{mt: 1}}
        >
          <UserDetailCard>
            <CardContent sx={{p: 2, '&:last-child': {pb: 2}}}>
              <Typography variant="h6" sx={{mb: 1}}>
                {selectedUsers.length === 1 ? 'User Details' : `${selectedUsers.length} Users`}
              </Typography>

              <Divider sx={{mb: 1}}/>

              <List dense sx={{maxHeight: 300, overflow: 'auto'}}>
                {selectedUsers.map(renderUserDetail)}
              </List>
            </CardContent>
          </UserDetailCard>
        </Popover>
      </Box>
  )
}

export default React.memo(ActiveUsersList)

// Simplified version for inline use
export const ActiveUsersInline: React.FC<{
  users: UserPresence[]
  maxVisible?: number
  size?: 'small' | 'medium' | 'large'
}> = ({users, maxVisible = 3, size = 'small'}) => {
  if (users.length === 0) return null

  const avatarSize = size === 'small' ? 24 : size === 'medium' ? 32 : 40

  return (
      <AvatarGroup
          max={maxVisible + 1}
          sx={{
            '& .MuiAvatar-root': {
              width: avatarSize,
              height: avatarSize,
              fontSize: '0.75rem',
            },
          }}
      >
        {users.slice(0, maxVisible).map((user) => (
            <PresenceAvatar
                key={user.userId}
                status={user.status}
                src={user.user.avatar}
                name={user.user.name}
                size={avatarSize}
                showAnimation={false}
            />
        ))}
      </AvatarGroup>
  )
}