/**
 * Smart Floating Action Button (FAB) System
 * 
 * Intelligent FAB component that adapts positioning, behavior, and interactions
 * based on screen size, scroll position, and navigation context
 */

import React, { useState, useEffect } from 'react'
import {
  Fab,
  FabProps,
  SpeedDial,
  SpeedDialAction,
  SpeedDialIcon,
  Tooltip,
  Box,
  useTheme,
  alpha,
  Zoom,
  Slide,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Share as ShareIcon,
  Favorite as FavoriteIcon,
  Close as CloseIcon,
  KeyboardArrowUp as ArrowUpIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import { styled } from '@mui/material/styles'
import { useResponsive, useScrollDirection } from '../hooks'

// Types
interface FABAction {
  id: string
  icon: React.ReactNode
  label: string
  onClick: () => void
  disabled?: boolean
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success'
}

interface SmartFABProps extends Omit<FabProps, 'color'> {
  // Positioning
  position?: 'bottom-right' | 'bottom-left' | 'bottom-center' | 'top-right' | 'custom'
  offset?: { x?: number; y?: number }
  avoidNavigation?: boolean
  
  // Behavior
  hideOnScroll?: boolean
  showOnlyWhenScrolled?: boolean
  scrollThreshold?: number
  
  // Multiple actions
  actions?: FABAction[]
  speedDialDirection?: 'up' | 'down' | 'left' | 'right'
  
  // Appearance
  variant?: 'circular' | 'extended'
  icon?: React.ReactNode
  label?: string
  tooltip?: string
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success' | 'inherit'
  
  // Animation
  transition?: 'zoom' | 'slide' | 'fade'
  
  // Event handlers
  onClick?: () => void
}

// Styled components
const StyledFab = styled(Fab, {
  shouldForwardProp: (prop) => !['hideOnScroll', 'isVisible'].includes(prop as string),
})<{
  hideOnScroll?: boolean
  isVisible?: boolean
}>(({ theme, hideOnScroll, isVisible }) => ({
  position: 'fixed',
  zIndex: theme.zIndex.fab,
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  
  // Visibility animation
  ...(hideOnScroll && {
    transform: isVisible ? 'scale(1)' : 'scale(0)',
    opacity: isVisible ? 1 : 0,
  }),
  
  // Enhanced shadow for better visibility
  boxShadow: `${theme.shadows[6]}, 0 0 0 1px ${alpha(theme.palette.common.white, 0.1)}`,
  
  '&:hover': {
    transform: isVisible ? 'scale(1.1)' : 'scale(0)',
    boxShadow: `${theme.shadows[12]}, 0 0 0 1px ${alpha(theme.palette.common.white, 0.15)}`,
  },
  
  // Touch device optimizations
  '@media (hover: none)': {
    '&:hover': {
      transform: isVisible ? 'scale(1)' : 'scale(0)',
      boxShadow: `${theme.shadows[6]}, 0 0 0 1px ${alpha(theme.palette.common.white, 0.1)}`,
    },
  },
}))

const StyledSpeedDial = styled(SpeedDial)(({ theme }) => ({
  position: 'fixed',
  zIndex: theme.zIndex.speedDial,
  
  '& .MuiSpeedDial-fab': {
    boxShadow: `${theme.shadows[6]}, 0 0 0 1px ${alpha(theme.palette.common.white, 0.1)}`,
  },
  
  '& .MuiSpeedDialAction-fab': {
    boxShadow: theme.shadows[4],
    
    '&:hover': {
      boxShadow: theme.shadows[8],
    },
  },
}))

// Position calculation hook
const useFABPosition = ({
  position = 'bottom-right',
  offset = {},
  avoidNavigation = true,
}: Pick<SmartFABProps, 'position' | 'offset' | 'avoidNavigation'>) => {
  const { isMobile } = useResponsive()
  const theme = useTheme()
  
  const getPosition = () => {
    const baseOffset = {
      x: offset.x || theme.spacing(2),
      y: offset.y || theme.spacing(2),
    }
    
    // Adjust for mobile navigation
    const bottomOffset = avoidNavigation && isMobile 
      ? theme.spacing(10) // Account for bottom navigation
      : baseOffset.y
    
    switch (position) {
      case 'bottom-right':
        return {
          bottom: bottomOffset,
          right: baseOffset.x,
        }
      
      case 'bottom-left':
        return {
          bottom: bottomOffset,
          left: baseOffset.x,
        }
      
      case 'bottom-center':
        return {
          bottom: bottomOffset,
          left: '50%',
          transform: 'translateX(-50%)',
        }
      
      case 'top-right':
        return {
          top: baseOffset.y,
          right: baseOffset.x,
        }
      
      default:
        return {}
    }
  }
  
  return getPosition()
}

// Scroll behavior hook
const useScrollBehavior = ({
  hideOnScroll = false,
  showOnlyWhenScrolled = false,
  scrollThreshold = 100,
}: Pick<SmartFABProps, 'hideOnScroll' | 'showOnlyWhenScrolled' | 'scrollThreshold'>) => {
  const [isVisible, setIsVisible] = useState(!showOnlyWhenScrolled)
  const [scrollY, setScrollY] = useState(0)
  const scrollDirection = useScrollDirection()
  
  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY
      setScrollY(currentScrollY)
      
      if (showOnlyWhenScrolled) {
        setIsVisible(currentScrollY > scrollThreshold)
      } else if (hideOnScroll) {
        // Hide when scrolling down, show when scrolling up or at top
        setIsVisible(scrollDirection !== 'down' || currentScrollY < scrollThreshold)
      }
    }
    
    window.addEventListener('scroll', handleScroll, { passive: true })
    handleScroll() // Initial check
    
    return () => window.removeEventListener('scroll', handleScroll)
  }, [hideOnScroll, showOnlyWhenScrolled, scrollThreshold, scrollDirection])
  
  return { isVisible, scrollY }
}

// Main SmartFAB component
export const SmartFAB: React.FC<SmartFABProps> = ({
  position = 'bottom-right',
  offset,
  avoidNavigation = true,
  hideOnScroll = false,
  showOnlyWhenScrolled = false,
  scrollThreshold = 100,
  actions,
  speedDialDirection = 'up',
  variant = 'circular',
  icon = <AddIcon />,
  label,
  tooltip = 'Action',
  color = 'primary',
  transition = 'zoom',
  onClick,
  ...fabProps
}) => {
  const fabPosition = useFABPosition({ position, offset, avoidNavigation })
  const { isVisible } = useScrollBehavior({ hideOnScroll, showOnlyWhenScrolled, scrollThreshold })
  
  // Speed dial state
  const [speedDialOpen, setSpeedDialOpen] = useState(false)
  
  // Handle FAB click
  const handleFABClick = () => {
    if (actions && actions.length > 0) {
      setSpeedDialOpen(!speedDialOpen)
    } else if (onClick) {
      onClick()
    }
  }
  
  // Render transition wrapper
  const TransitionWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    switch (transition) {
      case 'slide':
        return (
          <Slide in={isVisible} direction="up" timeout={300}>
            <div>{children}</div>
          </Slide>
        )
      case 'fade':
        return (
          <Zoom in={isVisible} timeout={300} style={{ transitionDelay: isVisible ? '0ms' : '100ms' }}>
            <div>{children}</div>
          </Zoom>
        )
      default: // zoom
        return (
          <Zoom in={isVisible} timeout={300}>
            <div>{children}</div>
          </Zoom>
        )
    }
  }
  
  // Render speed dial if actions are provided
  if (actions && actions.length > 0) {
    return (
      <TransitionWrapper>
        <StyledSpeedDial
          ariaLabel="Speed dial actions"
          icon={<SpeedDialIcon icon={icon} openIcon={<CloseIcon />} />}
          open={speedDialOpen}
          onClose={() => setSpeedDialOpen(false)}
          onOpen={() => setSpeedDialOpen(true)}
          direction={speedDialDirection}
          sx={fabPosition}
          FabProps={{
            color,
            ...fabProps,
          }}
        >
          {actions.map((action) => (
            <SpeedDialAction
              key={action.id}
              icon={action.icon}
              tooltipTitle={action.label}
              onClick={() => {
                action.onClick()
                setSpeedDialOpen(false)
              }}
              FabProps={{
                disabled: action.disabled,
                color: action.color || 'default',
              }}
            />
          ))}
        </StyledSpeedDial>
      </TransitionWrapper>
    )
  }
  
  // Render single FAB
  const fabContent = (
    <StyledFab
      color={color}
      variant={variant}
      onClick={handleFABClick}
      hideOnScroll={hideOnScroll || showOnlyWhenScrolled}
      isVisible={isVisible}
      sx={fabPosition}
      {...fabProps}
    >
      {icon}
      {variant === 'extended' && label && (
        <Box sx={{ ml: 1 }}>{label}</Box>
      )}
    </StyledFab>
  )
  
  // Wrap with tooltip if provided
  if (tooltip && variant !== 'extended') {
    return (
      <TransitionWrapper>
        <Tooltip title={tooltip} placement="left">
          {fabContent}
        </Tooltip>
      </TransitionWrapper>
    )
  }
  
  return <TransitionWrapper>{fabContent}</TransitionWrapper>
}

// Pre-configured FAB variants
export const ScrollToTopFAB: React.FC<{
  threshold?: number
  smooth?: boolean
}> = ({ threshold = 300, smooth = true }) => {
  const handleScrollToTop = () => {
    window.scrollTo({
      top: 0,
      behavior: smooth ? 'smooth' : 'auto',
    })
  }
  
  return (
    <SmartFAB
      icon={<ArrowUpIcon />}
      tooltip="Scroll to top"
      showOnlyWhenScrolled
      scrollThreshold={threshold}
      onClick={handleScrollToTop}
      color="secondary"
      position="bottom-right"
    />
  )
}

export const RefreshFAB: React.FC<{
  onRefresh: () => void
  loading?: boolean
}> = ({ onRefresh, loading = false }) => (
  <SmartFAB
    icon={<RefreshIcon sx={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />}
    tooltip="Refresh"
    onClick={onRefresh}
    disabled={loading}
    color="info"
    sx={{
      '@keyframes spin': {
        from: { transform: 'rotate(0deg)' },
        to: { transform: 'rotate(360deg)' },
      },
    }}
  />
)

export const QuickActionFAB: React.FC<{
  onAdd?: () => void
  onEdit?: () => void
  onShare?: () => void
  onFavorite?: () => void
}> = ({ onAdd, onEdit, onShare, onFavorite }) => {
  const actions: FABAction[] = []
  
  if (onAdd) {
    actions.push({
      id: 'add',
      icon: <AddIcon />,
      label: 'Add',
      onClick: onAdd,
    })
  }
  
  if (onEdit) {
    actions.push({
      id: 'edit',
      icon: <EditIcon />,
      label: 'Edit',
      onClick: onEdit,
    })
  }
  
  if (onShare) {
    actions.push({
      id: 'share',
      icon: <ShareIcon />,
      label: 'Share',
      onClick: onShare,
    })
  }
  
  if (onFavorite) {
    actions.push({
      id: 'favorite',
      icon: <FavoriteIcon />,
      label: 'Favorite',
      onClick: onFavorite,
    })
  }
  
  return (
    <SmartFAB
      actions={actions}
      hideOnScroll
      color="primary"
    />
  )
}

// Context-aware FAB that changes based on page
export const ContextualFAB: React.FC<{
  context: 'dashboard' | 'hives' | 'chat' | 'timer' | 'default'
  onAction: (action: string) => void
}> = ({ context, onAction }) => {
  const getContextActions = (): FABAction[] => {
    switch (context) {
      case 'dashboard':
        return [
          {
            id: 'create-hive',
            icon: <AddIcon />,
            label: 'Create Hive',
            onClick: () => onAction('create-hive'),
          },
          {
            id: 'start-timer',
            icon: <EditIcon />,
            label: 'Start Timer',
            onClick: () => onAction('start-timer'),
          },
        ]
      
      case 'hives':
        return [
          {
            id: 'join-hive',
            icon: <AddIcon />,
            label: 'Join Hive',
            onClick: () => onAction('join-hive'),
          },
          {
            id: 'create-hive',
            icon: <EditIcon />,
            label: 'Create Hive',
            onClick: () => onAction('create-hive'),
          },
        ]
      
      case 'chat':
        return [
          {
            id: 'new-message',
            icon: <AddIcon />,
            label: 'New Message',
            onClick: () => onAction('new-message'),
          },
        ]
      
      case 'timer':
        return [
          {
            id: 'start-session',
            icon: <AddIcon />,
            label: 'Start Session',
            onClick: () => onAction('start-session'),
          },
        ]
      
      default:
        return [
          {
            id: 'quick-action',
            icon: <AddIcon />,
            label: 'Quick Action',
            onClick: () => onAction('quick-action'),
          },
        ]
    }
  }
  
  return (
    <SmartFAB
      actions={getContextActions()}
      hideOnScroll
      color="primary"
    />
  )
}

export default SmartFAB