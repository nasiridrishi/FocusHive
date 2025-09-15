import React, { useState, useEffect } from 'react'
import {
  Box,
  CircularProgress,
  LinearProgress,
  Typography,
  Backdrop,
  Skeleton,
  useTheme,
} from '@mui/material'
import { styled, keyframes } from '@mui/material/styles'

const pulse = keyframes`
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
  100% {
    opacity: 1;
  }
`

const PulseBox = styled(Box)`
  animation: ${pulse} 1.5s ease-in-out infinite;
`

interface LoadingSpinnerProps {
  size?: number
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' | 'inherit'
  message?: string
  withBackdrop?: boolean
  fullscreen?: boolean
  variant?: 'circular' | 'linear' | 'skeleton' | 'pulse'
  isLoading?: boolean
  children?: React.ReactNode
  delay?: number
}

const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 40,
  color = 'primary',
  message,
  withBackdrop = false,
  fullscreen = false,
  variant = 'circular',
  isLoading = true,
  children,
  delay = 0,
}) => {
  const theme = useTheme()
  const [showSpinner, setShowSpinner] = useState(delay === 0)

  useEffect(() => {
    if (delay > 0) {
      const timer = setTimeout(() => {
        setShowSpinner(true)
      }, delay)
      return () => clearTimeout(timer)
    }
  }, [delay])

  // If not loading and has children, render children
  if (!isLoading && children) {
    return <>{children}</>
  }

  // If not loading and no children, render nothing
  if (!isLoading) {
    return null
  }

  // If delay is set and not yet showing, render nothing
  if (!showSpinner) {
    return null
  }

  const getProgressComponent = () => {
    switch (variant) {
      case 'linear':
        return (
          <LinearProgress
            color={color}
            role="progressbar"
            aria-busy="true"
            aria-label={message || 'Loading'}
            sx={{ width: '100%', minWidth: 200 }}
          />
        )
      case 'skeleton':
        return (
          <Box data-testid="loading-skeleton">
            <Skeleton variant="rectangular" width={size * 5} height={size} />
            <Skeleton variant="text" sx={{ mt: 1 }} />
            <Skeleton variant="text" width="60%" />
          </Box>
        )
      case 'pulse':
        return (
          <PulseBox
            data-testid="loading-pulse"
            className="pulse-animation"
            sx={{
              width: size,
              height: size,
              borderRadius: '50%',
              backgroundColor: theme.palette[color].main,
            }}
          />
        )
      case 'circular':
      default:
        return (
          <CircularProgress
            size={size}
            color={color}
            role="progressbar"
            aria-busy="true"
            aria-label={message || 'Loading'}
            sx={{
              width: `${size}px`,
              height: `${size}px`,
            }}
          />
        )
    }
  }

  const spinnerContent = (
    <Box
      data-testid="loading-spinner"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
      }}
    >
      {getProgressComponent()}
      {message && (
        <Typography
          variant="body2"
          color="text.secondary"
          aria-live="polite"
          sx={{ mt: 1 }}
        >
          {message}
        </Typography>
      )}
    </Box>
  )

  const containerStyles = {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    ...(fullscreen && {
      position: 'fixed' as const,
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      zIndex: theme.zIndex.modal,
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
    }),
  }

  if (withBackdrop) {
    return (
      <Backdrop
        data-testid="loading-backdrop"
        open={true}
        sx={{
          color: '#fff',
          zIndex: theme.zIndex.drawer + 1,
        }}
      >
        {spinnerContent}
      </Backdrop>
    )
  }

  return (
    <Box
      data-testid="loading-spinner-container"
      sx={containerStyles}
    >
      {spinnerContent}
    </Box>
  )
}

export default LoadingSpinner