import React from 'react'
import { ErrorBoundary, type FallbackProps } from 'react-error-boundary'
import { ErrorFallback } from './ErrorFallback'
import { logErrorBoundaryError, errorLogger } from '@shared/services/errorLogging'

// Re-export ErrorFallback for convenience
export { ErrorFallback }

export interface AppErrorBoundaryProps {
  children: React.ReactNode
  fallbackComponent?: React.ComponentType<FallbackProps>
  onError?: (error: Error, errorInfo: { componentStack?: string }) => void
  onReset?: () => void
  resetKeys?: Array<string | number | boolean | null | undefined>
  resetOnPropsChange?: boolean
  isolate?: boolean
  level?: 'app' | 'route' | 'feature' | 'component'
  name?: string
}

/**
 * Enhanced App-level Error Boundary with comprehensive error handling
 */
export const AppErrorBoundary: React.FC<AppErrorBoundaryProps> = ({
  children,
  fallbackComponent,
  onError,
  onReset,
  resetKeys,
  resetOnPropsChange: _resetOnPropsChange = true,
  isolate = false,
  level = 'component',
  name,
  ...props
}) => {
  const boundaryName = name || `${level}ErrorBoundary`

  const handleError = React.useCallback(
    (error: Error, errorInfo: { componentStack?: string }) => {
      // Log to our error service
      logErrorBoundaryError(error, { componentStack: errorInfo.componentStack || '', errorBoundary: boundaryName }, boundaryName, {
        level,
        isolate,
        timestamp: new Date().toISOString(),
        url: window.location.href,
        userAgent: navigator.userAgent,
      })

      // Call custom error handler if provided
      if (onError) {
        onError(error, errorInfo)
      }

      // Additional error tracking for different levels
      switch (level) {
        case 'app':
          // Critical application-level error
          console.error('🔴 CRITICAL: App-level error boundary triggered', error)
          break
        case 'route':
          // Route-level error
          console.error('🟠 Route-level error boundary triggered', error)
          break
        case 'feature':
          // Feature-level error
          console.error('🟡 Feature-level error boundary triggered', error)
          break
        default:
          // Component-level error
          console.error('🔵 Component-level error boundary triggered', error)
      }
    },
    [onError, level, boundaryName, isolate]
  )

  const handleReset = React.useCallback(() => {
    console.log(`🔄 Error boundary reset: ${boundaryName}`)
    
    if (onReset) {
      onReset()
    }

    // Clear any relevant application state based on level
    switch (level) {
      case 'app':
        // For app-level resets, might want to clear some global state
        console.log('Performing app-level reset...')
        break
      case 'route':
        // For route-level resets, might want to reset route-specific state
        console.log('Performing route-level reset...')
        break
      case 'feature':
        // For feature-level resets, might want to reset feature state
        console.log('Performing feature-level reset...')
        break
    }
  }, [onReset, level, boundaryName])

  const handleGoHome = React.useCallback(() => {
    // Clear error logs and navigate home
    console.log('Navigating to home due to error boundary')
    window.location.href = '/'
  }, [])

  const handleReportError = React.useCallback((error: Error) => {
    // Enhanced error reporting
    errorLogger.logError(
      error,
      { componentStack: '', errorBoundary: boundaryName },
      {
        level,
        isolate,
        reportedByUser: true,
        timestamp: new Date().toISOString(),
      },
      level === 'app' ? 'critical' : 'high'
    )

    // In a real app, this might open a bug report modal or send to analytics
    console.log('Error reported by user:', error)
  }, [level, boundaryName, isolate])

  const defaultFallback = React.useCallback(
    (fallbackProps: { error: Error; resetErrorBoundary: () => void }) => (
      <ErrorFallback
        error={fallbackProps.error}
        resetErrorBoundary={fallbackProps.resetErrorBoundary}
        title={getErrorTitle(level)}
        subtitle={getErrorSubtitle(level)}
        errorBoundaryName={boundaryName}
        showErrorDetails={import.meta.env.DEV || level === 'app'}
        showResetButton={true}
        showHomeButton={level === 'app' || level === 'route'}
        showReportButton={true}
        onGoHome={handleGoHome}
        onReportError={handleReportError}
        severity={level === 'app' ? 'error' : 'warning'}
      />
    ),
    [level, boundaryName, handleGoHome, handleReportError]
  )

  return (
    <ErrorBoundary
      FallbackComponent={fallbackComponent || defaultFallback}
      onError={handleError}
      onReset={handleReset}
      resetKeys={resetKeys}
      {...props}
    >
      {children}
    </ErrorBoundary>
  )
}

/**
 * Specialized error boundaries for different levels
 */

export const AppLevelErrorBoundary: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <AppErrorBoundary
    level="app"
    name="AppLevelErrorBoundary"
    resetOnPropsChange={false}
    isolate={false}
  >
    {children}
  </AppErrorBoundary>
)

export const RouteLevelErrorBoundary: React.FC<{
  children: React.ReactNode
  routeName?: string
}> = ({ children, routeName }) => (
  <AppErrorBoundary
    level="route"
    name={routeName ? `${routeName}RouteErrorBoundary` : 'RouteErrorBoundary'}
    resetKeys={[window.location.pathname]}
  >
    {children}
  </AppErrorBoundary>
)

export const FeatureLevelErrorBoundary: React.FC<{
  children: React.ReactNode
  featureName: string
}> = ({ children, featureName }) => (
  <AppErrorBoundary
    level="feature"
    name={`${featureName}FeatureErrorBoundary`}
    isolate={true}
  >
    {children}
  </AppErrorBoundary>
)

/**
 * Helper functions for error messaging
 */
function getErrorTitle(level: string): string {
  switch (level) {
    case 'app':
      return 'Application Error'
    case 'route':
      return 'Page Loading Error'
    case 'feature':
      return 'Feature Unavailable'
    default:
      return 'Something went wrong'
  }
}

function getErrorSubtitle(level: string): string {
  switch (level) {
    case 'app':
      return 'A critical error has occurred. Please try refreshing the application.'
    case 'route':
      return 'This page encountered an error while loading. You can try again or navigate elsewhere.'
    case 'feature':
      return 'This feature is temporarily unavailable due to an error. Other parts of the application should work normally.'
    default:
      return 'An unexpected error occurred while loading this section.'
  }
}