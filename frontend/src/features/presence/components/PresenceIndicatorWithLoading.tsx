import React from 'react'
import { Badge, Avatar, Skeleton } from '@mui/material'
import { PresenceStatus } from '@shared/types/presence'
import { LoadingSkeleton } from '@shared/components/loading'
import PresenceIndicator from './PresenceIndicator'

interface PresenceIndicatorWithLoadingProps {
  /** User's avatar URL */
  avatarSrc?: string
  /** User's display name for alt text */
  displayName: string
  /** Current presence status */
  status?: PresenceStatus
  /** Whether data is loading */
  isLoading?: boolean
  /** Avatar size */
  size?: 'small' | 'medium' | 'large'
  /** Show status animation */
  showAnimation?: boolean
  /** Click handler */
  onClick?: () => void
}

/**
 * Enhanced PresenceIndicator with loading state support
 * 
 * Shows skeleton loading while presence data is being fetched,
 * then displays the actual presence indicator when loaded.
 */
const PresenceIndicatorWithLoading: React.FC<PresenceIndicatorWithLoadingProps> = ({
  avatarSrc,
  displayName,
  status = 'offline',
  isLoading = false,
  size = 'medium',
  showAnimation = true,
  onClick
}) => {
  const getAvatarSize = () => {
    switch (size) {
      case 'small': return 32
      case 'medium': return 40
      case 'large': return 56
      default: return 40
    }
  }

  const avatarSize = getAvatarSize()

  if (isLoading) {
    return (
      <Badge
        overlap="circular"
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        badgeContent={
          <Skeleton 
            variant="circular" 
            width={12} 
            height={12} 
            animation="pulse"
          />
        }
      >
        <Skeleton 
          variant="circular" 
          width={avatarSize} 
          height={avatarSize}
          animation="pulse"
        />
      </Badge>
    )
  }

  return (
    <PresenceIndicator
      avatarSrc={avatarSrc}
      displayName={displayName}
      status={status}
      size={size}
      showAnimation={showAnimation}
      onClick={onClick}
    />
  )
}

export default PresenceIndicatorWithLoading