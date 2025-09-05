import React from 'react'
import { 
  Chip, 
  Tooltip, 
  styled, 
  alpha, 
  useTheme,
  Typography,
  Box,
  Theme 
} from '@mui/material'
import {
  FiberManualRecord as OnlineIcon,
  Visibility as FocusingIcon,
  Coffee as BreakIcon,
  Schedule as AwayIcon,
  RadioButtonUnchecked as OfflineIcon,
} from '@mui/icons-material'
import { PresenceStatus } from '../../../shared/types/presence'

const getStatusConfig = (status: PresenceStatus, theme: Theme) => {
  switch (status) {
    case 'online':
      return {
        label: 'Online',
        color: theme.palette.success.main,
        backgroundColor: alpha(theme.palette.success.main, 0.1),
        icon: <OnlineIcon sx={{ fontSize: 12 }} />,
        description: 'Available and active',
      }
    case 'focusing':
      return {
        label: 'Focusing',
        color: theme.palette.primary.main,
        backgroundColor: alpha(theme.palette.primary.main, 0.1),
        icon: <FocusingIcon sx={{ fontSize: 12 }} />,
        description: 'In a focus session',
      }
    case 'break':
      return {
        label: 'On Break',
        color: theme.palette.warning.main,
        backgroundColor: alpha(theme.palette.warning.main, 0.1),
        icon: <BreakIcon sx={{ fontSize: 12 }} />,
        description: 'Taking a break',
      }
    case 'away':
      return {
        label: 'Away',
        color: theme.palette.grey[600],
        backgroundColor: alpha(theme.palette.grey[600], 0.1),
        icon: <AwayIcon sx={{ fontSize: 12 }} />,
        description: 'Away from keyboard',
      }
    case 'offline':
      return {
        label: 'Offline',
        color: theme.palette.grey[400],
        backgroundColor: alpha(theme.palette.grey[400], 0.1),
        icon: <OfflineIcon sx={{ fontSize: 12 }} />,
        description: 'Not connected',
      }
    default:
      return {
        label: 'Unknown',
        color: theme.palette.grey[400],
        backgroundColor: alpha(theme.palette.grey[400], 0.1),
        icon: <OfflineIcon sx={{ fontSize: 12 }} />,
        description: 'Status unknown',
      }
  }
}

const StyledChip = styled(Chip)<{ statusColor: string; statusBg: string }>(
  ({ statusColor, statusBg }) => ({
    height: 24,
    fontSize: '0.75rem',
    fontWeight: 500,
    color: statusColor,
    backgroundColor: statusBg,
    border: `1px solid ${alpha(statusColor, 0.3)}`,
    transition: 'all 0.2s ease-in-out',
    '& .MuiChip-icon': {
      color: statusColor,
      marginLeft: '4px',
    },
    '& .MuiChip-label': {
      paddingLeft: '6px',
      paddingRight: '8px',
    },
    '&:hover': {
      backgroundColor: alpha(statusColor, 0.15),
      border: `1px solid ${alpha(statusColor, 0.5)}`,
      transform: 'translateY(-1px)',
      boxShadow: `0 2px 8px ${alpha(statusColor, 0.2)}`,
    },
  })
)

interface UserStatusBadgeProps {
  status: PresenceStatus
  currentActivity?: string
  lastSeen?: string
  showTooltip?: boolean
  size?: 'small' | 'medium'
  variant?: 'default' | 'compact' | 'icon-only'
  onClick?: () => void
  className?: string
}

const UserStatusBadge: React.FC<UserStatusBadgeProps> = ({
  status,
  currentActivity,
  lastSeen,
  showTooltip = true,
  size = 'medium',
  variant = 'default',
  onClick,
  className,
}) => {
  const theme = useTheme()
  const statusConfig = getStatusConfig(status, theme)

  const formatLastSeen = (lastSeenStr?: string) => {
    if (!lastSeenStr) return ''
    
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

  const getChipProps = () => {
    const baseProps = {
      statusColor: statusConfig.color,
      statusBg: statusConfig.backgroundColor,
      className,
      onClick,
      clickable: Boolean(onClick),
    }

    if (variant === 'icon-only') {
      return {
        ...baseProps,
        icon: statusConfig.icon,
        size: size as 'small' | 'medium',
        label: '',
      }
    }

    if (variant === 'compact') {
      return {
        ...baseProps,
        icon: statusConfig.icon,
        label: statusConfig.label,
        size: 'small' as const,
      }
    }

    return {
      ...baseProps,
      icon: statusConfig.icon,
      label: statusConfig.label,
      size: size as 'small' | 'medium',
    }
  }

  const _tooltipContent = () => (
    <Box sx={{ textAlign: 'center' }}>
      <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 0.5 }}>
        {statusConfig.label}
      </Typography>
      <Typography variant="body2" sx={{ mb: currentActivity ? 1 : 0.5 }}>
        {statusConfig.description}
      </Typography>
      {currentActivity && (
        <Typography variant="body2" sx={{ fontStyle: 'italic', mb: 0.5 }}>
          "{currentActivity}"
        </Typography>
      )}
      {lastSeen && status !== 'online' && (
        <Typography variant="caption" color="text.secondary">
          Last seen: {formatLastSeen(lastSeen)}
        </Typography>
      )}
    </Box>
  )

  const chipElement = (
    <StyledChip
      {...getChipProps()}
    />
  )

  if (!showTooltip) {
    return chipElement
  }

  return (
    <Tooltip
      title={<tooltipContent />}
      placement="top"
      arrow
      enterDelay={500}
      leaveDelay={200}
    >
      {chipElement}
    </Tooltip>
  )
}

export default React.memo(UserStatusBadge)

// Convenience component for just the status dot
export const StatusDot: React.FC<{
  status: PresenceStatus
  size?: number
  showTooltip?: boolean
  currentActivity?: string
}> = ({ status, size = 8, showTooltip = true, currentActivity }) => {
  const theme = useTheme()
  const statusConfig = getStatusConfig(status, theme)

  const dot = (
    <Box
      sx={{
        width: size,
        height: size,
        borderRadius: '50%',
        backgroundColor: statusConfig.color,
        border: `1px solid ${theme.palette.background.paper}`,
        boxShadow: `0 0 0 1px ${alpha(statusConfig.color, 0.3)}`,
        transition: 'all 0.2s ease-in-out',
        ...(status === 'focusing' && {
          animation: 'pulse 2s infinite ease-in-out',
          '@keyframes pulse': {
            '0%': { transform: 'scale(1)' },
            '50%': { transform: 'scale(1.2)' },
            '100%': { transform: 'scale(1)' },
          },
        }),
      }}
    />
  )

  if (!showTooltip) {
    return dot
  }

  return (
    <Tooltip
      title={
        <Box>
          <Typography variant="body2">{statusConfig.label}</Typography>
          {currentActivity && (
            <Typography variant="caption" sx={{ fontStyle: 'italic' }}>
              {currentActivity}
            </Typography>
          )}
        </Box>
      }
      placement="top"
      arrow
    >
      {dot}
    </Tooltip>
  )
}