import {Box, CircularProgress, Skeleton, Stack, Typography} from '@mui/material'
import LoadingSpinner from './LoadingSpinner'

export interface LazyLoadingFallbackProps {
  /**
   * Type of loading fallback to display
   * - spinner: Simple circular progress indicator
   * - skeleton: Content-shaped loading placeholder
   * - page: Full page loading with branded message
   * - feature: Feature-specific loading state
   */
  variant?: 'spinner' | 'skeleton' | 'page' | 'feature'

  /**
   * Custom loading message
   */
  message?: string

  /**
   * Minimum height for the loading area
   */
  minHeight?: string | number

  /**
   * Show loading message alongside indicator
   */
  showMessage?: boolean

  /**
   * Feature name for contextual loading (used with 'feature' variant)
   */
  featureName?: string
}

/**
 * Optimized loading fallback component for React.lazy components
 * Provides different loading states based on context
 */
export function LazyLoadingFallback({
                                      variant = 'spinner',
                                      message,
                                      minHeight = 400,
                                      showMessage = true,
                                      featureName
                                    }: LazyLoadingFallbackProps) {
  const getLoadingMessage = (): string => {
    if (message) return message

    switch (variant) {
      case 'page':
        return 'Loading FocusHive...'
      case 'feature':
        return featureName ? `Loading ${featureName}...` : 'Loading feature...'
      case 'skeleton':
        return 'Preparing content...'
      default:
        return 'Loading...'
    }
  }

  const renderSpinner = () => (
      <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          justifyContent="center"
          minHeight={minHeight}
          gap={2}
      >
        <CircularProgress size={40}/>
        {showMessage && (
            <Typography variant="body2" color="text.secondary">
              {getLoadingMessage()}
            </Typography>
        )}
      </Box>
  )

  const renderSkeleton = () => (
      <Box sx={{p: 2, minHeight}}>
        <Stack spacing={2}>
          {/* Page header skeleton */}
          <Skeleton variant="text" width="60%" height={40}/>
          <Skeleton variant="text" width="40%" height={20}/>

          {/* Content skeleton */}
          <Box sx={{mt: 3}}>
            <Skeleton variant="rectangular" height={120} sx={{mb: 2}}/>
            <Stack direction="row" spacing={2}>
              <Skeleton variant="rectangular" width="30%" height={80}/>
              <Skeleton variant="rectangular" width="30%" height={80}/>
              <Skeleton variant="rectangular" width="30%" height={80}/>
            </Stack>
          </Box>

          {/* List items skeleton */}
          <Box sx={{mt: 3}}>
            {Array.from({length: 3}, (_, i) => (
                <Stack key={i} direction="row" spacing={2} sx={{mb: 2}}>
                  <Skeleton variant="circular" width={40} height={40}/>
                  <Box sx={{flex: 1}}>
                    <Skeleton variant="text" width="80%"/>
                    <Skeleton variant="text" width="60%"/>
                  </Box>
                </Stack>
            ))}
          </Box>
        </Stack>

        {showMessage && (
            <Box display="flex" justifyContent="center" mt={2}>
              <Typography variant="body2" color="text.secondary">
                {getLoadingMessage()}
              </Typography>
            </Box>
        )}
      </Box>
  )

  const renderPageLoading = () => (
      <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          justifyContent="center"
          minHeight="100vh"
          bgcolor="background.default"
      >
        <LoadingSpinner size="large"/>
        <Typography variant="h6" sx={{mt: 2, mb: 1}}>
          {getLoadingMessage()}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Setting up your workspace...
        </Typography>
      </Box>
  )

  const renderFeatureLoading = () => (
      <Box
          display="flex"
          flexDirection="column"
          alignItems="center"
          justifyContent="center"
          minHeight={minHeight}
          sx={{
            bgcolor: 'action.hover',
            borderRadius: 2,
            border: '1px dashed',
            borderColor: 'divider'
          }}
      >
        <CircularProgress size={32} thickness={4}/>
        <Typography variant="body1" sx={{mt: 2, fontWeight: 500}}>
          {getLoadingMessage()}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{mt: 0.5}}>
          Preparing interactive content
        </Typography>
      </Box>
  )

  switch (variant) {
    case 'skeleton':
      return renderSkeleton()
    case 'page':
      return renderPageLoading()
    case 'feature':
      return renderFeatureLoading()
    default:
      return renderSpinner()
  }
}

// Pre-configured loading fallbacks for common use cases
export const RouteLoadingFallback = () => (
    <LazyLoadingFallback variant="skeleton" minHeight="60vh"/>
)

export const PageLoadingFallback = () => (
    <LazyLoadingFallback variant="page"/>
)

export const FeatureLoadingFallback = ({featureName}: { featureName?: string }) => (
    <LazyLoadingFallback variant="feature" featureName={featureName}/>
)

export const ComponentLoadingFallback = () => (
    <LazyLoadingFallback variant="spinner" minHeight={200}/>
)