import React from 'react'
import {Avatar, Box, Skeleton} from '@mui/material'
import type {LoadingSkeletonProps} from './types'

/**
 * Flexible skeleton loading component
 *
 * Features:
 * - Multiple lines support
 * - Avatar placeholder
 * - Action buttons placeholder
 * - Configurable animation
 * - Responsive design
 */
const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({
                                                           lines = 3,
                                                           avatar = false,
                                                           actions = false,
                                                           animation = 'wave',
                                                           width,
                                                           height,
                                                           variant = 'text',
                                                           sx,
                                                           ...skeletonProps
                                                         }) => {
  const renderLines = () => {
    const lineElements = []

    for (let i = 0; i < lines; i++) {
      const isLastLine = i === lines - 1
      const lineWidth = isLastLine ? '60%' : '100%'

      lineElements.push(
          <Skeleton
              key={i}
              variant="text"
              width={lineWidth}
              height={20}
              animation={animation}
              {...skeletonProps}
          />
      )
    }

    return lineElements
  }

  if (variant !== 'text' || (width && height)) {
    // Single skeleton with specific dimensions
    return (
        <Skeleton
            variant={variant}
            width={width}
            height={height}
            animation={animation}
            sx={sx}
            {...skeletonProps}
        />
    )
  }

  return (
      <Box sx={{width: '100%', ...sx}}>
        {avatar && (
            <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
              <Avatar sx={{mr: 2}}>
                <Skeleton variant="circular" width={40} height={40} animation={animation}/>
              </Avatar>
              <Box sx={{flex: 1}}>
                <Skeleton variant="text" width="40%" height={24} animation={animation}/>
                <Skeleton variant="text" width="60%" height={16} animation={animation}/>
              </Box>
            </Box>
        )}

        <Box sx={{mb: actions ? 2 : 0}}>
          {renderLines()}
        </Box>

        {actions && (
            <Box sx={{display: 'flex', gap: 1, justifyContent: 'flex-end'}}>
              <Skeleton
                  variant="rectangular"
                  width={80}
                  height={36}
                  animation={animation}
                  sx={{borderRadius: 1}}
              />
              <Skeleton
                  variant="rectangular"
                  width={80}
                  height={36}
                  animation={animation}
                  sx={{borderRadius: 1}}
              />
            </Box>
        )}
      </Box>
  )
}

export default LoadingSkeleton