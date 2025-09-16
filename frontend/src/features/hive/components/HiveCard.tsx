import React from 'react'
import {
  Avatar,
  AvatarGroup,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material'
import {
  Circle as CircleIcon,
  ExitToApp as ExitToAppIcon,
  Lock as LockIcon,
  MoreVert as MoreVertIcon,
  People as PeopleIcon,
  PlayArrow as PlayArrowIcon,
  Public as PublicIcon,
  Settings as SettingsIcon,
  Share as ShareIcon,
  Timer as TimerIcon,
} from '@mui/icons-material'
import {Hive, HiveMember} from '@shared/types'
import {JoinHiveButton} from './JoinHiveButton'

interface HiveCardProps {
  hive: Hive
  members?: HiveMember[]
  currentUserId?: string
  onJoin?: (hiveId: string) => void
  onLeave?: (hiveId: string) => void
  onEnter?: (hiveId: string) => void
  onSettings?: (hiveId: string) => void
  onShare?: (hiveId: string) => void
  variant?: 'default' | 'compact' | 'detailed'
  isLoading?: boolean
}

export const HiveCard: React.FC<HiveCardProps> = ({
                                                    hive,
                                                    members = [],
                                                    currentUserId,
                                                    onJoin,
                                                    onLeave,
                                                    onEnter,
                                                    onSettings,
                                                    onShare,
                                                    variant = 'default',
                                                    isLoading = false,
                                                  }) => {
  const theme = useTheme()
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null)

  // Check if current user is a member
  const isMember = currentUserId && members.some(member => member.userId === currentUserId)
  const isOwner = currentUserId === hive.ownerId

  // Get online members
  const onlineMembers = members.filter(member => member.isActive)
  const onlineMembersCount = onlineMembers.length

  // Get current user's role if they're a member
  const currentUserMember = members.find(member => member.userId === currentUserId)
  const canManage = currentUserMember?.permissions.canManageSettings || isOwner

  const handleMenuOpen = (event: React.MouseEvent<HTMLButtonElement>): void => {
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = (): void => {
    setAnchorEl(null)
  }

  const handleAction = (action: () => void): void => {
    action()
    handleMenuClose()
  }

  const renderMembers = (): React.ReactElement | null => {
    if (variant === 'compact') return null

    return (
        <Box sx={{display: 'flex', alignItems: 'center', gap: 1, mt: 1}}>
          <AvatarGroup
              max={4}
              sx={{
                '& .MuiAvatar-root': {
                  width: 24,
                  height: 24,
                  fontSize: '0.75rem'
                }
              }}
          >
            {onlineMembers.slice(0, 4).map((member) => (
                <Tooltip key={member.id}
                         title={`${member.user.firstName} ${member.user.lastName} - Online`}>
                  <Avatar
                      src={member.user.profilePicture}
                      alt={`${member.user.firstName} ${member.user.lastName}`}
                      sx={{
                        border: `2px solid ${theme.palette.success.main}`,
                      }}
                  >
                    {member.user.firstName[0]}{member.user.lastName[0]}
                  </Avatar>
                </Tooltip>
            ))}
          </AvatarGroup>

          {onlineMembersCount > 0 && (
              <Chip
                  size="small"
                  icon={<CircleIcon sx={{color: 'success.main'}}/>}
                  label={`${onlineMembersCount} online`}
                  variant="outlined"
                  sx={{fontSize: '0.7rem'}}
              />
          )}
        </Box>
    )
  }

  const renderStats = (): React.ReactElement | null => {
    return (
        <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1}}>
          <Chip
              size="small"
              icon={<PeopleIcon/>}
              label={`${hive.currentMembers}/${hive.maxMembers}`}
              variant="outlined"
          />

          {hive.settings.focusMode && (
              <Chip
                  size="small"
                  icon={<TimerIcon/>}
                  label={hive.settings.focusMode}
                  variant="outlined"
                  color="primary"
              />
          )}

          <Chip
              size="small"
              icon={hive.isPublic ? <PublicIcon/> : <LockIcon/>}
              label={hive.isPublic ? 'Public' : 'Private'}
              variant="outlined"
              color={hive.isPublic ? 'success' : 'warning'}
          />
        </Box>
    )
  }

  const renderActions = (): React.ReactElement | null => {
    if (isMember) {
      return (
          <>
            <Button
                variant="contained"
                startIcon={<PlayArrowIcon/>}
                onClick={() => onEnter?.(hive.id)}
                disabled={isLoading}
                sx={{minWidth: 120}}
            >
              Enter Hive
            </Button>

            <IconButton
                size="small"
                onClick={handleMenuOpen}
                disabled={isLoading}
            >
              <MoreVertIcon/>
            </IconButton>

            <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={handleMenuClose}
                anchorOrigin={{
                  vertical: 'bottom',
                  horizontal: 'right',
                }}
                transformOrigin={{
                  vertical: 'top',
                  horizontal: 'right',
                }}
            >
              {canManage && (
                  <MenuItem onClick={() => handleAction(() => onSettings?.(hive.id))}>
                    <SettingsIcon sx={{mr: 1}}/>
                    Settings
                  </MenuItem>
              )}
              <MenuItem onClick={() => handleAction(() => onShare?.(hive.id))}>
                <ShareIcon sx={{mr: 1}}/>
                Share
              </MenuItem>
              {!isOwner && (
                  <MenuItem
                      onClick={() => handleAction(() => onLeave?.(hive.id))}
                      sx={{color: 'error.main'}}
                  >
                    <ExitToAppIcon sx={{mr: 1}}/>
                    Leave Hive
                  </MenuItem>
              )}
            </Menu>
          </>
      )
    }

    return (
        <JoinHiveButton
            hive={hive}
            onJoin={onJoin}
            isLoading={isLoading}
            variant="contained"
            size="medium"
        />
    )
  }

  const cardHeight = variant === 'compact' ? 120 : variant === 'detailed' ? 240 : 180

  return (
      <Card
          sx={{
            height: cardHeight,
            display: 'flex',
            flexDirection: 'column',
            transition: 'all 0.2s ease-in-out',
            '&:hover': {
              transform: 'translateY(-2px)',
              boxShadow: theme.shadows[4],
            },
            border: isMember ? `2px solid ${theme.palette.primary.main}` : undefined,
            opacity: isLoading ? 0.7 : 1,
          }}
      >
        <CardContent sx={{flex: 1, pb: 1}}>
          {/* Header */}
          <Box sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            mb: 1
          }}>
            <Typography
                variant="h6"
                component="h3"
                sx={{
                  fontWeight: 600,
                  fontSize: variant === 'compact' ? '1rem' : '1.1rem',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  display: '-webkit-box',
                  WebkitLineClamp: 1,
                  WebkitBoxOrient: 'vertical',
                }}
            >
              {hive.name}
            </Typography>

            {isMember && (
                <Chip
                    size="small"
                    label="Member"
                    color="primary"
                    variant="filled"
                    sx={{fontSize: '0.7rem'}}
                />
            )}
          </Box>

          {/* Description */}
          {variant !== 'compact' && (
              <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{
                    mb: 2,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: variant === 'detailed' ? 3 : 2,
                    WebkitBoxOrient: 'vertical',
                  }}
              >
                {hive.description}
              </Typography>
          )}

          {/* Tags */}
          {variant === 'detailed' && hive.tags.length > 0 && (
              <Box sx={{display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 1}}>
                {hive.tags.slice(0, 3).map((tag) => (
                    <Chip
                        key={tag}
                        size="small"
                        label={tag}
                        variant="outlined"
                        sx={{fontSize: '0.7rem', height: 20}}
                    />
                ))}
                {hive.tags.length > 3 && (
                    <Chip
                        size="small"
                        label={`+${hive.tags.length - 3} more`}
                        variant="outlined"
                        sx={{fontSize: '0.7rem', height: 20}}
                    />
                )}
              </Box>
          )}

          {/* Stats and Members */}
          {renderStats()}
          {renderMembers()}
        </CardContent>

        {/* Actions */}
        <CardActions sx={{pt: 0, px: 2, pb: 2, justifyContent: 'space-between'}}>
          {renderActions()}
        </CardActions>
      </Card>
  )
}

export default HiveCard