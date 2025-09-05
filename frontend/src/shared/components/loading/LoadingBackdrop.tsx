import React from 'react'
import {
  Backdrop,
  Box,
  CircularProgress,
  LinearProgress,
  Typography,
  Card,
  CardContent
} from '@mui/material'
import type { LoadingBackdropProps } from './types'

/**
 * Full-screen loading backdrop with optional progress
 * 
 * Features:
 * - Full-screen overlay
 * - Simple or detailed variant
 * - Progress indicator support
 * - Customizable text
 * - High z-index for modals
 */
const LoadingBackdrop: React.FC<LoadingBackdropProps> = ({
  open,
  text = 'Loading...',
  progress,
  variant = 'simple',
  sx,
  ...backdropProps
}) => {
  const renderSimpleVariant = () => (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 2,
        color: 'white'
      }}
    >
      <CircularProgress
        size={60}
        thickness={4}
        sx={{ color: 'white' }}
        {...(progress !== undefined && {
          variant: 'determinate',
          value: progress
        })}
      />
      <Typography
        variant="h6"
        sx={{
          color: 'white',
          textAlign: 'center',
          textShadow: '0 2px 4px rgba(0,0,0,0.5)'
        }}
      >
        {text}
      </Typography>
      {progress !== undefined && (
        <Typography
          variant="body2"
          sx={{
            color: 'white',
            textShadow: '0 2px 4px rgba(0,0,0,0.5)'
          }}
        >
          {Math.round(progress)}%
        </Typography>
      )}
    </Box>
  )

  const renderDetailedVariant = () => (
    <Card
      sx={{
        minWidth: 300,
        maxWidth: 400,
        backgroundColor: 'background.paper',
        boxShadow: 3
      }}
    >
      <CardContent sx={{ textAlign: 'center', py: 4 }}>
        <Box sx={{ mb: 3 }}>
          <CircularProgress
            size={60}
            thickness={4}
            {...(progress !== undefined && {
              variant: 'determinate',
              value: progress
            })}
          />
        </Box>

        <Typography variant="h6" gutterBottom>
          {text}
        </Typography>

        {progress !== undefined && (
          <Box sx={{ mt: 2 }}>
            <LinearProgress
              variant="determinate"
              value={progress}
              sx={{ height: 8, borderRadius: 4, mb: 1 }}
            />
            <Typography variant="body2" color="text.secondary">
              {Math.round(progress)}% complete
            </Typography>
          </Box>
        )}

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mt: 2 }}
        >
          Please wait while we process your request...
        </Typography>
      </CardContent>
    </Card>
  )

  return (
    <Backdrop
      open={open}
      sx={{
        color: '#fff',
        zIndex: (theme) => theme.zIndex.drawer + 1,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        backdropFilter: 'blur(2px)',
        ...sx
      }}
      {...backdropProps}
    >
      {variant === 'simple' ? renderSimpleVariant() : renderDetailedVariant()}
    </Backdrop>
  )
}

export default LoadingBackdrop