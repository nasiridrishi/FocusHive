import React from 'react'
import { Button, CircularProgress, Box } from '@mui/material'
import type { LoadingButtonProps } from './types'

/**
 * Button component with integrated loading state
 * 
 * Features:
 * - Material UI v6.4.0+ Button loading prop support
 * - Backwards compatibility for older versions
 * - Customizable loading text
 * - Loading indicator positioning
 * - Disabled interaction during loading
 */
const LoadingButton: React.FC<LoadingButtonProps> = ({
  loading = false,
  loadingText,
  loadingPosition = 'center',
  children,
  disabled,
  startIcon,
  endIcon,
  ...buttonProps
}) => {
  // Custom implementation for all versions
  // Note: MUI Button doesn't have a native loading prop

  // Custom implementation for backwards compatibility
  const renderLoadingIndicator = () => (
    <CircularProgress
      size={16}
      thickness={4}
      sx={{ 
        color: 'inherit',
        ...(loadingPosition !== 'center' && { position: 'absolute' })
      }}
    />
  )

  const getStartIcon = () => {
    if (loading && loadingPosition === 'start') {
      return renderLoadingIndicator()
    }
    return loading ? null : startIcon
  }

  const getEndIcon = () => {
    if (loading && loadingPosition === 'end') {
      return renderLoadingIndicator()
    }
    return loading ? null : endIcon
  }

  const getChildren = () => {
    if (loading) {
      if (loadingText) {
        return loadingText
      }
      if (loadingPosition === 'center') {
        return (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {renderLoadingIndicator()}
            <span style={{ visibility: 'hidden' }}>{children}</span>
          </Box>
        )
      }
    }
    return children
  }

  return (
    <Button
      disabled={disabled || loading}
      startIcon={getStartIcon()}
      endIcon={getEndIcon()}
      sx={{
        position: 'relative',
        ...(loading && loadingPosition === 'center' && {
          '& .MuiButton-startIcon, & .MuiButton-endIcon': {
            visibility: 'hidden'
          }
        })
      }}
      {...buttonProps}
    >
      {getChildren()}
    </Button>
  )
}

export default LoadingButton