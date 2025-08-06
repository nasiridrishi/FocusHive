/**
 * Smart Modal Component
 * 
 * Intelligent modal system that adapts to screen size, device capabilities,
 * and user preferences. Provides optimal UX across all devices.
 */

import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogProps,
  Slide,
  Fade,
  Grow,
  Drawer,
  IconButton,
  Typography,
  Box,
  useTheme,
  alpha,
  SxProps,
  Theme,
} from '@mui/material'
import {
  Close as CloseIcon,
  ArrowBack as ArrowBackIcon,
  Fullscreen as FullscreenIcon,
  FullscreenExit as FullscreenExitIcon,
} from '@mui/icons-material'
import { TransitionProps } from '@mui/material/transitions'
import { styled } from '@mui/material/styles'
import { useResponsive, useDynamicViewportHeight, useReducedMotion, useTouchGestures } from '../hooks'

// Transition components
const SlideUpTransition = React.forwardRef<
  unknown,
  TransitionProps & { children: React.ReactElement }
>((props, ref) => <Slide direction="up" ref={ref} {...props} />)

const SlideLeftTransition = React.forwardRef<
  unknown,
  TransitionProps & { children: React.ReactElement }
>((props, ref) => <Slide direction="left" ref={ref} {...props} />)

// Styled components
const StyledDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiDialog-paper': {
    margin: 0,
    borderRadius: theme.shape.borderRadius * 2,
    boxShadow: theme.shadows[24],
  },
  
  // Mobile styles
  [theme.breakpoints.down('tablet')]: {
    '& .MuiDialog-paper': {
      margin: theme.spacing(2),
      width: `calc(100% - ${theme.spacing(4)})`,
      maxHeight: `calc(100% - ${theme.spacing(4)})`,
      borderRadius: theme.shape.borderRadius,
    },
  },
  
  // Full screen mobile
  '&.mobile-fullscreen .MuiDialog-paper': {
    margin: 0,
    width: '100%',
    height: '100%',
    maxHeight: '100%',
    borderRadius: 0,
  },
}))

const MobileDrawer = styled(Drawer)(({ theme }) => ({
  '& .MuiDrawer-paper': {
    borderTopLeftRadius: theme.shape.borderRadius * 2,
    borderTopRightRadius: theme.shape.borderRadius * 2,
    maxHeight: '90vh',
    minHeight: 200,
  },
}))

const DragHandle = styled(Box)(({ theme }) => ({
  width: 40,
  height: 4,
  backgroundColor: theme.palette.action.disabled,
  borderRadius: 2,
  margin: `${theme.spacing(1)} auto`,
  cursor: 'grab',
  '&:active': {
    cursor: 'grabbing',
  },
}))

const ModalHeader = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: theme.spacing(2, 3),
  borderBottom: `1px solid ${theme.palette.divider}`,
  minHeight: 64,
}))

// Props interfaces
interface SmartModalProps extends Omit<DialogProps, 'TransitionComponent'> {
  // Content props
  title?: string
  subtitle?: string
  children: React.ReactNode
  actions?: React.ReactNode
  
  // Behavior props
  variant?: 'dialog' | 'drawer' | 'fullscreen' | 'adaptive'
  size?: 'small' | 'medium' | 'large' | 'fullWidth'
  closable?: boolean
  dismissible?: boolean
  persistent?: boolean
  
  // Mobile-specific props
  showDragHandle?: boolean
  allowSwipeDown?: boolean
  mobileFullscreen?: boolean
  
  // Animation props
  transition?: 'fade' | 'slide' | 'grow' | 'none'
  transitionDuration?: number
  
  // Event handlers
  onClose?: () => void
  onFullscreenToggle?: (fullscreen: boolean) => void
  onSwipeDown?: () => void
}

// Hook for managing modal state
const useModalBehavior = ({
  variant = 'adaptive',
  mobileFullscreen = false,
}: Pick<SmartModalProps, 'variant' | 'mobileFullscreen'>) => {
  const { isMobile, isTablet } = useResponsive()
  const { height: viewportHeight } = useDynamicViewportHeight()
  const prefersReducedMotion = useReducedMotion()
  const [isFullscreen, setIsFullscreen] = useState(false)
  
  // Determine modal variant based on device and preferences
  const effectiveVariant = React.useMemo(() => {
    if (variant !== 'adaptive') return variant
    
    if (isMobile) {
      return mobileFullscreen ? 'fullscreen' : 'drawer'
    }
    
    if (isTablet) {
      return 'dialog'
    }
    
    return 'dialog'
  }, [variant, isMobile, isTablet, mobileFullscreen])
  
  // Animation preferences
  const shouldReduceMotion = prefersReducedMotion
  const transitionDuration = shouldReduceMotion ? 0 : 300
  
  return {
    effectiveVariant,
    isFullscreen,
    setIsFullscreen,
    viewportHeight,
    shouldReduceMotion,
    transitionDuration,
    isMobile,
    isTablet,
  }
}

// Main SmartModal component
export const SmartModal: React.FC<SmartModalProps> = ({
  title,
  subtitle,
  children,
  actions,
  variant = 'adaptive',
  size = 'medium',
  closable = true,
  dismissible = true,
  persistent = false,
  showDragHandle = true,
  allowSwipeDown = true,
  mobileFullscreen = false,
  transition = 'fade',
  onClose,
  onFullscreenToggle,
  onSwipeDown,
  open,
  ...dialogProps
}) => {
  const theme = useTheme()
  const {
    effectiveVariant,
    isFullscreen,
    setIsFullscreen,
    viewportHeight,
    shouldReduceMotion,
    transitionDuration,
    isMobile,
  } = useModalBehavior({ variant, mobileFullscreen })
  
  const modalRef = React.useRef<HTMLDivElement>(null)
  const gesture = useTouchGestures(modalRef)
  
  // Handle swipe down to close on mobile
  useEffect(() => {
    if (effectiveVariant === 'drawer' && gesture.type === 'swipe' && gesture.direction === 'down') {
      if (allowSwipeDown && onSwipeDown) {
        onSwipeDown()
      } else if (dismissible && onClose) {
        onClose()
      }
    }
  }, [gesture, effectiveVariant, allowSwipeDown, dismissible, onSwipeDown, onClose])
  
  // Handle escape key
  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && dismissible && !persistent && onClose) {
        onClose()
      }
    }
    
    if (open) {
      document.addEventListener('keydown', handleEscape)
      return () => document.removeEventListener('keydown', handleEscape)
    }
  }, [open, dismissible, persistent, onClose])
  
  // Size configuration
  const sizeConfig = {
    small: { maxWidth: 400 },
    medium: { maxWidth: 600 },
    large: { maxWidth: 800 },
    fullWidth: { maxWidth: false },
  }
  
  // Transition component selection
  const getTransitionComponent = () => {
    if (shouldReduceMotion || transition === 'none') return undefined
    
    switch (transition) {
      case 'slide':
        return isMobile ? SlideUpTransition : SlideLeftTransition
      case 'grow':
        return Grow
      default:
        return Fade
    }
  }
  
  // Toggle fullscreen mode
  const handleFullscreenToggle = () => {
    const newFullscreen = !isFullscreen
    setIsFullscreen(newFullscreen)
    onFullscreenToggle?.(newFullscreen)
  }
  
  // Render modal header
  const renderHeader = () => (
    <ModalHeader>
      <Box>
        {title && (
          <Typography variant="h6" component="h2" fontWeight={600}>
            {title}
          </Typography>
        )}
        {subtitle && (
          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        )}
      </Box>
      
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {effectiveVariant === 'dialog' && !isMobile && (
          <IconButton
            onClick={handleFullscreenToggle}
            size="small"
            aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
          >
            {isFullscreen ? <FullscreenExitIcon /> : <FullscreenIcon />}
          </IconButton>
        )}
        
        {closable && (
          <IconButton
            onClick={onClose}
            size="small"
            aria-label="Close"
          >
            {isMobile ? <ArrowBackIcon /> : <CloseIcon />}
          </IconButton>
        )}
      </Box>
    </ModalHeader>
  )
  
  // Render content
  const renderContent = () => (
    <Box
      sx={{
        flex: 1,
        overflowY: 'auto',
        padding: theme.spacing(3),
        // Custom scrollbar styling
        '&::-webkit-scrollbar': {
          width: 6,
        },
        '&::-webkit-scrollbar-thumb': {
          backgroundColor: alpha(theme.palette.text.primary, 0.2),
          borderRadius: 3,
        },
      }}
    >
      {children}
    </Box>
  )
  
  // Render actions
  const renderActions = () => {
    if (!actions) return null
    
    return (
      <Box
        sx={{
          padding: theme.spacing(2, 3),
          borderTop: `1px solid ${theme.palette.divider}`,
          display: 'flex',
          justifyContent: 'flex-end',
          gap: 1,
        }}
      >
        {actions}
      </Box>
    )
  }
  
  // Render based on variant
  switch (effectiveVariant) {
    case 'drawer':
      return (
        <MobileDrawer
          anchor="bottom"
          open={open}
          onClose={dismissible ? onClose : undefined}
          disableEscapeKeyDown={!dismissible}
          ModalProps={{
            keepMounted: true,
          }}
          {...dialogProps}
        >
          <Box
            ref={modalRef}
            sx={{
              display: 'flex',
              flexDirection: 'column',
              height: '100%',
              maxHeight: viewportHeight * 0.9,
            }}
          >
            {showDragHandle && <DragHandle />}
            {(title || subtitle) && renderHeader()}
            {renderContent()}
            {renderActions()}
          </Box>
        </MobileDrawer>
      )
    
    case 'fullscreen':
      return (
        <StyledDialog
          fullScreen
          open={open}
          onClose={dismissible ? onClose : undefined}
          TransitionComponent={getTransitionComponent()}
          transitionDuration={transitionDuration}
          {...dialogProps}
        >
          <Box
            ref={modalRef}
            sx={{
              display: 'flex',
              flexDirection: 'column',
              height: '100%',
            }}
          >
            {renderHeader()}
            {renderContent()}
            {renderActions()}
          </Box>
        </StyledDialog>
      )
    
    default: // dialog
      return (
        <StyledDialog
          open={open}
          onClose={dismissible ? onClose : undefined}
          maxWidth={sizeConfig[size].maxWidth ? undefined : false}
          fullWidth={size === 'fullWidth'}
          fullScreen={isFullscreen}
          TransitionComponent={getTransitionComponent()}
          transitionDuration={transitionDuration}
          className={isMobile && mobileFullscreen ? 'mobile-fullscreen' : ''}
          PaperProps={{
            sx: {
              display: 'flex',
              flexDirection: 'column',
              maxHeight: isFullscreen ? '100%' : '90vh',
              ...(sizeConfig[size].maxWidth !== false ? { maxWidth: sizeConfig[size].maxWidth } : {}),
            } as SxProps<Theme>,
          }}
          {...dialogProps}
        >
          <Box
            ref={modalRef}
            sx={{
              display: 'flex',
              flexDirection: 'column',
              height: '100%',
              minHeight: 200,
            }}
          >
            {(title || subtitle) && renderHeader()}
            {renderContent()}
            {renderActions()}
          </Box>
        </StyledDialog>
      )
  }
}

// Pre-configured modal variants
export const ConfirmationModal: React.FC<{
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  onConfirm: () => void
  onCancel: () => void
  severity?: 'info' | 'warning' | 'error' | 'success'
}> = ({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  onConfirm,
  onCancel,
  severity = 'info',
}) => {
  const theme = useTheme()
  
  const severityColors = {
    info: theme.palette.info.main,
    warning: theme.palette.warning.main,
    error: theme.palette.error.main,
    success: theme.palette.success.main,
  }
  
  return (
    <SmartModal
      open={open}
      title={title}
      size="small"
      onClose={onCancel}
      actions={
        <>
          <button onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            style={{
              backgroundColor: severityColors[severity],
              color: theme.palette.getContrastText(severityColors[severity]),
            }}
          >
            {confirmLabel}
          </button>
        </>
      }
    >
      <Typography>{message}</Typography>
    </SmartModal>
  )
}

export const FormModal: React.FC<{
  open: boolean
  title: string
  children: React.ReactNode
  submitLabel?: string
  cancelLabel?: string
  onSubmit: () => void
  onCancel: () => void
  loading?: boolean
  disabled?: boolean
}> = ({
  open,
  title,
  children,
  submitLabel = 'Submit',
  cancelLabel = 'Cancel',
  onSubmit,
  onCancel,
  loading = false,
  disabled = false,
}) => (
  <SmartModal
    open={open}
    title={title}
    size="medium"
    onClose={onCancel}
    persistent={loading}
    actions={
      <>
        <button onClick={onCancel} disabled={loading}>
          {cancelLabel}
        </button>
        <button
          onClick={onSubmit}
          disabled={disabled || loading}
        >
          {loading ? 'Loading...' : submitLabel}
        </button>
      </>
    }
  >
    {children}
  </SmartModal>
)

export default SmartModal