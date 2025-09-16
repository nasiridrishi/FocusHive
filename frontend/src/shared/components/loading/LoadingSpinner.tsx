import React from 'react'
import {Box, CircularProgress, Typography} from '@mui/material'
import type {LoadingSpinnerProps} from './types'

/**
 * Reusable loading spinner component with optional text
 *
 * Features:
 * - Customizable size and color
 * - Optional loading text
 * - Centered or inline display
 * - Responsive design
 */
const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
                                                         text,
                                                         centered = true,
                                                         inline = false,
                                                         size = 40,
                                                         ...circularProgressProps
                                                       }) => {
  const Wrapper = inline ? 'span' : Box

  if (inline) {
    return (
        <Wrapper
            component="span"
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 1,
              verticalAlign: 'middle'
            }}
        >
          <CircularProgress size={size} {...circularProgressProps} />
          {text && (
              <Typography
                  component="span"
                  variant="body2"
                  color="text.secondary"
                  sx={{whiteSpace: 'nowrap'}}
              >
                {text}
              </Typography>
          )}
        </Wrapper>
    )
  }

  return (
      <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: centered ? 'center' : 'flex-start',
            justifyContent: centered ? 'center' : 'flex-start',
            gap: 2,
            ...(centered && {
              minHeight: '200px',
            }),
          }}
      >
        <CircularProgress size={size} {...circularProgressProps} />
        {text && (
            <Typography
                variant="body1"
                color="text.secondary"
                align={centered ? 'center' : 'left'}
            >
              {text}
            </Typography>
        )}
      </Box>
  )
}

export default LoadingSpinner