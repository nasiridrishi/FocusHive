// Main error boundary components
export {
  ErrorBoundaryConfig,
  APIErrorBoundary,
  AuthErrorBoundary,
  PermissionErrorBoundary,
  FeatureErrorBoundary,
  ComponentErrorBoundary,
  withErrorBoundary,
  useErrorBoundaryConfig,
} from './ErrorBoundaryConfig'
export type {ErrorBoundaryConfigProps} from './ErrorBoundaryConfig'

// Specialized fallback components
export {
  NetworkErrorFallback,
  PermissionErrorFallback,
} from './fallbacks'
export type {
  NetworkErrorFallbackProps,
  PermissionErrorFallbackProps,
} from './fallbacks'

// Re-export shared components for convenience
export {
  AppErrorBoundary,
  AppLevelErrorBoundary,
  RouteLevelErrorBoundary,
  FeatureLevelErrorBoundary,
  ErrorFallback,
  ErrorBoundaryDemo,
  ErrorBoundary,
  useErrorBoundary,
  withErrorBoundary as withSharedErrorBoundary,
} from '@shared/components/error-boundary'
export type {
  AppErrorBoundaryProps,
  ErrorFallbackProps,
} from '@shared/components/error-boundary'