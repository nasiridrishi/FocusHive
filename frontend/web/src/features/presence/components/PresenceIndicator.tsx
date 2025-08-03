import React from 'react'
import { Badge, Avatar, styled, keyframes, useTheme } from '@mui/material'
import { PresenceStatus } from '../../../shared/types/presence'

const ripple = keyframes`
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(2.4);
    opacity: 0;
  }
`

const pulse = keyframes`
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
`

const getStatusColor = (status: PresenceStatus, theme: any) => {
  switch (status) {
    case 'online':
      return theme.palette.success.main
    case 'focusing':
      return theme.palette.primary.main
    case 'break':
      return theme.palette.warning.main
    case 'away':
      return theme.palette.grey[500]
    case 'offline':
      return theme.palette.grey[400]
    default:
      return theme.palette.grey[400]
  }
}

const StyledBadge = styled(Badge)<{ 
  presenceStatus: PresenceStatus 
  showAnimation?: boolean 
}>(({ theme, presenceStatus, showAnimation }) => {
  const statusColor = getStatusColor(presenceStatus, theme)
  
  return {
    '& .MuiBadge-badge': {
      backgroundColor: statusColor,
      color: statusColor,
      boxShadow: `0 0 0 2px ${theme.palette.background.paper}`,
      width: 12,
      height: 12,
      borderRadius: '50%',
      position: 'relative',
      '&::after': showAnimation && (presenceStatus === 'online' || presenceStatus === 'focusing') ? {
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        borderRadius: '50%',
        animation: presenceStatus === 'focusing' 
          ? `${pulse} 2s infinite ease-in-out`
          : `${ripple} 2s infinite ease-in-out`,
        border: `1px solid ${statusColor}`,
        content: '""',
      } : {},
      ...(presenceStatus === 'focusing' && {
        animation: showAnimation ? `${pulse} 1.5s infinite ease-in-out` : 'none',
      }),
    },
  }
})

interface PresenceIndicatorProps {
  status: PresenceStatus
  children: React.ReactNode
  showAnimation?: boolean
  size?: 'small' | 'medium' | 'large'
  overlap?: 'circular' | 'rectangular'
  anchorOrigin?: {
    vertical: 'top' | 'bottom'
    horizontal: 'left' | 'right'
  }
  className?: string
}

const PresenceIndicator: React.FC<PresenceIndicatorProps> = ({
  status,
  children,
  showAnimation = true,
  size = 'medium',
  overlap = 'circular',
  anchorOrigin = { vertical: 'bottom', horizontal: 'right' },
  className,
}) => {
  const theme = useTheme()

  const getBadgeProps = () => {
    const baseProps = {
      overlap,
      anchorOrigin,
      variant: 'dot' as const,
      presenceStatus: status,
      showAnimation,
      className,
    }

    // Hide badge for offline status
    if (status === 'offline') {
      return {
        ...baseProps,
        invisible: true,
      }
    }

    return baseProps
  }

  return (
    <StyledBadge {...getBadgeProps()}>
      {children}
    </StyledBadge>
  )
}

export default React.memo(PresenceIndicator)

// Convenience wrapper for Avatar with presence
export const PresenceAvatar: React.FC<{
  status: PresenceStatus
  src?: string
  alt?: string
  name?: string
  size?: number
  showAnimation?: boolean
  onClick?: () => void
}> = ({ 
  status, 
  src, 
  alt, 
  name, 
  size = 40, 
  showAnimation = true,
  onClick 
}) => {
  const getInitials = (fullName?: string) => {
    if (!fullName) return '?'
    return fullName
      .split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .slice(0, 2)
  }

  return (
    <PresenceIndicator status={status} showAnimation={showAnimation}>
      <Avatar
        src={src}
        alt={alt}
        onClick={onClick}
        sx={{
          width: size,
          height: size,
          cursor: onClick ? 'pointer' : 'default',
          transition: 'transform 0.2s ease-in-out',
          '&:hover': onClick ? {
            transform: 'scale(1.05)',
          } : {},
        }}
      >
        {!src && getInitials(name)}
      </Avatar>
    </PresenceIndicator>
  )
}