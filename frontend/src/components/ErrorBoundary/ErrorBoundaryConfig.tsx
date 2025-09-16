import React from 'react'
import {AppErrorBoundary, AppErrorBoundaryProps} from '@shared/components/error-boundary'
import {NetworkErrorFallback, PermissionErrorFallback} from './fallbacks'
import {FallbackProps} from 'react-error-boundary'

export interface ErrorBoundaryConfigProps extends Omit<AppErrorBoundaryProps, 'children' | 'fallbackComponent'> {
  children: React.ReactNode
  errorType?: 'default' | 'network' | 'permission' | 'authentication' | 'authorization'
  customFallback?: React.ComponentType<FallbackProps>
  // Network-specific props
  endpoint?: string
  requestMethod?: string
  maxRetries?: number
  retryDelay?: number
  onNetworkRestore?: () => void
  // Permission-specific props
  requiredPermission?: string
  userRole?: string
  onLogin?: () => void
  onContactSupport?: () => void
}

/**
 * Comprehensive Error Boundary Configuration Component
 * Provides pre-configured error boundaries for different application areas
 */
export const ErrorBoundaryConfig: React.FC<ErrorBoundaryConfigProps> = ({
                                                                          children,
                                                                          errorType = 'default',
                                                                          customFallback,
                                                                          endpoint,
                                                                          requestMethod,
                                                                          maxRetries = 3,
                                                                          retryDelay = 1000,
                                                                          onNetworkRestore,
                                                                          requiredPermission,
                                                                          userRole,
                                                                          onLogin,
                                                                          onContactSupport,
                                                                          ...boundaryProps
                                                                        }) => {
  // Create error-type-specific fallback components
  const createFallbackComponent = React.useCallback((type: string) => {
    return (fallbackProps: FallbackProps) => {
      switch (type) {
        case 'network':
          return (
              <NetworkErrorFallback
                  {...fallbackProps}
                  endpoint={endpoint}
                  requestMethod={requestMethod}
                  maxRetries={maxRetries}
                  retryDelay={retryDelay}
                  onNetworkRestore={onNetworkRestore}
              />
          )

        case 'permission':
          return (
              <PermissionErrorFallback
                  {...fallbackProps}
                  errorType="authorization"
                  requiredPermission={requiredPermission}
                  userRole={userRole}
                  onLogin={onLogin}
                  onContactSupport={onContactSupport}
              />
          )

        case 'authentication':
          return (
              <PermissionErrorFallback
                  {...fallbackProps}
                  errorType="authentication"
                  onLogin={onLogin}
                  onContactSupport={onContactSupport}
              />
          )

        case 'authorization':
          return (
              <PermissionErrorFallback
                  {...fallbackProps}
                  errorType="authorization"
                  requiredPermission={requiredPermission}
                  userRole={userRole}
                  onContactSupport={onContactSupport}
              />
          )

        default:
          return undefined // Use default fallback
      }
    }
  }, [
    endpoint,
    requestMethod,
    maxRetries,
    retryDelay,
    onNetworkRestore,
    requiredPermission,
    userRole,
    onLogin,
    onContactSupport,
  ])

  const fallbackComponent = customFallback || (errorType !== 'default' ? createFallbackComponent(errorType) : undefined)

  return (
      <AppErrorBoundary
          {...boundaryProps}
          fallbackComponent={fallbackComponent}
      >
        {children}
      </AppErrorBoundary>
  )
}

/**
 * Pre-configured error boundaries for specific application areas
 */

export const APIErrorBoundary: React.FC<{
  children: React.ReactNode
  endpoint?: string
  method?: string
}> = ({children, endpoint, method}) => (
    <ErrorBoundaryConfig
        errorType="network"
        level="feature"
        name="APIErrorBoundary"
        endpoint={endpoint}
        requestMethod={method}
        isolate={true}
    >
      {children}
    </ErrorBoundaryConfig>
)

export const AuthErrorBoundary: React.FC<{
  children: React.ReactNode
  onLogin?: () => void
}> = ({children, onLogin}) => (
    <ErrorBoundaryConfig
        errorType="authentication"
        level="feature"
        name="AuthErrorBoundary"
        onLogin={onLogin}
        isolate={true}
    >
      {children}
    </ErrorBoundaryConfig>
)

export const PermissionErrorBoundary: React.FC<{
  children: React.ReactNode
  requiredPermission: string
  userRole?: string
  onContactSupport?: () => void
}> = ({children, requiredPermission, userRole, onContactSupport}) => (
    <ErrorBoundaryConfig
        errorType="permission"
        level="feature"
        name="PermissionErrorBoundary"
        requiredPermission={requiredPermission}
        userRole={userRole}
        onContactSupport={onContactSupport}
        isolate={true}
    >
      {children}
    </ErrorBoundaryConfig>
)

export const FeatureErrorBoundary: React.FC<{
  children: React.ReactNode
  featureName: string
  fallbackType?: 'default' | 'network' | 'permission'
}> = ({children, featureName, fallbackType = 'default'}) => (
    <ErrorBoundaryConfig
        errorType={fallbackType}
        level="feature"
        name={`${featureName}FeatureErrorBoundary`}
        isolate={true}
    >
      {children}
    </ErrorBoundaryConfig>
)

export const ComponentErrorBoundary: React.FC<{
  children: React.ReactNode
  componentName: string
}> = ({children, componentName}) => (
    <ErrorBoundaryConfig
        level="component"
        name={`${componentName}ComponentErrorBoundary`}
        isolate={true}
    >
      {children}
    </ErrorBoundaryConfig>
)

/**
 * Higher-Order Component for wrapping components with error boundaries
 */
export function withErrorBoundary<P extends object>(
    Component: React.ComponentType<P>,
    errorBoundaryProps: Partial<ErrorBoundaryConfigProps> = {}
) {
  const WrappedComponent = React.forwardRef<unknown, P>((props, ref) => {
    return (
      <ErrorBoundaryConfig
          level="component"
          name={`${Component.displayName || Component.name}ErrorBoundary`}
          {...errorBoundaryProps}
      >
        <Component {...(props as P)} ref={ref}/>
      </ErrorBoundaryConfig>
    );
  })

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`

  return WrappedComponent
}

/**
 * Hook for creating error boundary configurations
 */
export function useErrorBoundaryConfig(
    errorType: ErrorBoundaryConfigProps['errorType'] = 'default',
    options: Partial<ErrorBoundaryConfigProps> = {}
): Pick<ErrorBoundaryConfigProps, 'errorType' | 'customFallback'> {
  const config = React.useMemo(() => ({
    errorType,
    ...options,
  }), [errorType, options])

  return config
}