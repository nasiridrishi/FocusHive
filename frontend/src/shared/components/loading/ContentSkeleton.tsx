import React from 'react'
import {Box, Card, CardContent, List, ListItem, Skeleton} from '@mui/material'
import type {ContentSkeletonProps} from './types'

/**
 * Content-specific skeleton loading components
 *
 * Features:
 * - Pre-built skeletons for common content types
 * - Configurable count and animation
 * - Responsive design
 * - FocusHive-specific layouts
 */
const ContentSkeleton: React.FC<ContentSkeletonProps> = ({
                                                           type,
                                                           count = 1,
                                                           animation = 'wave'
                                                         }) => {
  const renderCardSkeleton = () => (
      <Card sx={{mb: 2}}>
        <CardContent>
          <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
            <Skeleton variant="circular" width={40} height={40} animation={animation}/>
            <Box sx={{ml: 2, flex: 1}}>
              <Skeleton variant="text" width="60%" height={24} animation={animation}/>
              <Skeleton variant="text" width="40%" height={16} animation={animation}/>
            </Box>
          </Box>
          <Skeleton variant="text" width="100%" height={20} animation={animation}/>
          <Skeleton variant="text" width="80%" height={20} animation={animation}/>
          <Skeleton variant="text" width="60%" height={20} animation={animation}/>
          <Box sx={{display: 'flex', gap: 1, mt: 2}}>
            <Skeleton variant="rectangular" width={80} height={32} animation={animation}
                      sx={{borderRadius: 1}}/>
            <Skeleton variant="rectangular" width={80} height={32} animation={animation}
                      sx={{borderRadius: 1}}/>
          </Box>
        </CardContent>
      </Card>
  )

  const renderListSkeleton = () => (
      <List>
        <ListItem>
          <Box sx={{display: 'flex', alignItems: 'center', width: '100%'}}>
            <Skeleton variant="circular" width={32} height={32} animation={animation}/>
            <Box sx={{ml: 2, flex: 1}}>
              <Skeleton variant="text" width="70%" height={20} animation={animation}/>
              <Skeleton variant="text" width="50%" height={16} animation={animation}/>
            </Box>
            <Skeleton variant="rectangular" width={60} height={24} animation={animation}
                      sx={{borderRadius: 1}}/>
          </Box>
        </ListItem>
      </List>
  )

  const renderFormSkeleton = () => (
      <Box sx={{p: 2}}>
        <Skeleton variant="text" width="40%" height={32} animation={animation} sx={{mb: 3}}/>

        {[1, 2, 3].map((field) => (
            <Box key={field} sx={{mb: 3}}>
              <Skeleton variant="text" width="30%" height={20} animation={animation} sx={{mb: 1}}/>
              <Skeleton variant="rectangular" width="100%" height={56} animation={animation}
                        sx={{borderRadius: 1}}/>
            </Box>
        ))}

        <Box sx={{display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 4}}>
          <Skeleton variant="rectangular" width={80} height={40} animation={animation}
                    sx={{borderRadius: 1}}/>
          <Skeleton variant="rectangular" width={100} height={40} animation={animation}
                    sx={{borderRadius: 1}}/>
        </Box>
      </Box>
  )

  const renderTableSkeleton = () => (
      <Box>
        {/* Header */}
        <Box sx={{display: 'flex', gap: 2, p: 2, borderBottom: 1, borderColor: 'divider'}}>
          {[1, 2, 3, 4].map((col) => (
              <Skeleton key={col} variant="text" width="20%" height={24} animation={animation}/>
          ))}
        </Box>

        {/* Rows */}
        {Array.from({length: 5}).map((_, rowIndex) => (
            <Box key={rowIndex}
                 sx={{display: 'flex', gap: 2, p: 2, borderBottom: 1, borderColor: 'divider'}}>
              {[1, 2, 3, 4].map((col) => (
                  <Skeleton key={col} variant="text" width="20%" height={20} animation={animation}/>
              ))}
            </Box>
        ))}
      </Box>
  )

  const renderChatSkeleton = () => (
      <Box sx={{p: 2}}>
        {[1, 2, 3].map((message) => (
            <Box
                key={message}
                sx={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  mb: 2,
                  ...(message % 2 === 0 && {justifyContent: 'flex-end'})
                }}
            >
              {message % 2 === 1 && (
                  <Skeleton variant="circular" width={32} height={32} animation={animation}
                            sx={{mr: 2}}/>
              )}
              <Box sx={{maxWidth: '70%'}}>
                <Skeleton
                    variant="rectangular"
                    width={Math.random() * 200 + 100}
                    height={40}
                    animation={animation}
                    sx={{borderRadius: 2}}
                />
                <Skeleton variant="text" width="40%" height={12} animation={animation}
                          sx={{mt: 0.5}}/>
              </Box>
              {message % 2 === 0 && (
                  <Skeleton variant="circular" width={32} height={32} animation={animation}
                            sx={{ml: 2}}/>
              )}
            </Box>
        ))}
      </Box>
  )

  const renderHiveSkeleton = () => (
      <Card sx={{mb: 2}}>
        <CardContent>
          {/* Hive header */}
          <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
            <Skeleton variant="circular" width={48} height={48} animation={animation}/>
            <Box sx={{ml: 2, flex: 1}}>
              <Skeleton variant="text" width="50%" height={28} animation={animation}/>
              <Skeleton variant="text" width="30%" height={16} animation={animation}/>
            </Box>
            <Skeleton variant="rectangular" width={80} height={32} animation={animation}
                      sx={{borderRadius: 1}}/>
          </Box>

          {/* Hive description */}
          <Skeleton variant="text" width="100%" height={20} animation={animation}/>
          <Skeleton variant="text" width="85%" height={20} animation={animation}/>

          {/* Tags */}
          <Box sx={{display: 'flex', gap: 1, mt: 2, mb: 2}}>
            {[1, 2, 3].map((tag) => (
                <Skeleton
                    key={tag}
                    variant="rectangular"
                    width={60}
                    height={24}
                    animation={animation}
                    sx={{borderRadius: 3}}
                />
            ))}
          </Box>

          {/* Stats */}
          <Box sx={{display: 'flex', gap: 3}}>
            <Skeleton variant="text" width="80px" height={16} animation={animation}/>
            <Skeleton variant="text" width="60px" height={16} animation={animation}/>
            <Skeleton variant="text" width="100px" height={16} animation={animation}/>
          </Box>
        </CardContent>
      </Card>
  )

  const renderSkeleton = () => {
    switch (type) {
      case 'card':
        return renderCardSkeleton()
      case 'list':
        return renderListSkeleton()
      case 'form':
        return renderFormSkeleton()
      case 'table':
        return renderTableSkeleton()
      case 'chat':
        return renderChatSkeleton()
      case 'hive':
        return renderHiveSkeleton()
      default:
        return renderCardSkeleton()
    }
  }

  return (
      <Box>
        {Array.from({length: count}).map((_, index) => (
            <Box key={index}>
              {renderSkeleton()}
            </Box>
        ))}
      </Box>
  )
}

export default ContentSkeleton