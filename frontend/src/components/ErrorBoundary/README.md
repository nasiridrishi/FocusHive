# Error Boundary Components

Comprehensive error handling infrastructure for the FocusHive React application. This system
provides graceful error recovery, specialized fallback UI components, and integration with
monitoring services.

## Overview

The error boundary system consists of:

- **Base Error Boundaries**: Core error boundary implementations
- **Specialized Fallbacks**: UI components for different error types
- **Configuration Components**: Pre-configured boundaries for common scenarios
- **Monitoring Integration**: Sentry and LogRocket integration for error tracking
- **Offline Support**: Error queuing and retry mechanisms

## Quick Start

### Basic Usage

```tsx
import { ErrorBoundaryConfig } from '@/components/ErrorBoundary'

function App() {
  return (
    <ErrorBoundaryConfig level="app">
      <YourAppContent />
    </ErrorBoundaryConfig>
  )
}
```

### Network Error Handling

```tsx
import { APIErrorBoundary } from '@/components/ErrorBoundary'

function UserDashboard() {
  return (
    <APIErrorBoundary endpoint="/api/users" method="GET">
      <UserList />
    </APIErrorBoundary>
  )
}
```

### Authentication Error Handling

```tsx
import { AuthErrorBoundary } from '@/components/ErrorBoundary'

function ProtectedRoute() {
  return (
    <AuthErrorBoundary onLogin={() => navigate('/login')}>
      <ProtectedContent />
    </AuthErrorBoundary>
  )
}
```

## Components

### ErrorBoundaryConfig

Main configuration component that provides different error handling strategies.

```tsx
interface ErrorBoundaryConfigProps {
  children: React.ReactNode
  errorType?: 'default' | 'network' | 'permission' | 'authentication' | 'authorization'
  level?: 'app' | 'route' | 'feature' | 'component'
  name?: string
  
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
```

#### Error Types

- **`default`**: Standard error boundary with basic fallback
- **`network`**: Network-specific errors with retry mechanisms
- **`permission`**: Authorization/authentication errors with login options
- **`authentication`**: User authentication required
- **`authorization`**: User lacks required permissions

#### Levels

- **`app`**: Application-wide errors (critical)
- **`route`**: Page-level errors
- **`feature`**: Feature-specific errors (isolated)
- **`component`**: Component-level errors

### Pre-configured Boundaries

#### APIErrorBoundary

For API call failures and network issues.

```tsx
<APIErrorBoundary endpoint="/api/data" method="POST">
  <DataForm />
</APIErrorBoundary>
```

#### AuthErrorBoundary

For authentication-related errors.

```tsx
<AuthErrorBoundary onLogin={handleLogin}>
  <ProtectedComponent />
</AuthErrorBoundary>
```

#### PermissionErrorBoundary

For authorization and permission errors.

```tsx
<PermissionErrorBoundary 
  requiredPermission="admin:write"
  userRole="user"
  onContactSupport={handleContactSupport}
>
  <AdminPanel />
</PermissionErrorBoundary>
```

#### FeatureErrorBoundary

For feature-specific error isolation.

```tsx
<FeatureErrorBoundary featureName="Chat" fallbackType="network">
  <ChatComponent />
</FeatureErrorBoundary>
```

#### ComponentErrorBoundary

For individual component error handling.

```tsx
<ComponentErrorBoundary componentName="UserCard">
  <UserCard />
</ComponentErrorBoundary>
```

## Specialized Fallback Components

### NetworkErrorFallback

Handles network connectivity issues with:

- Network status monitoring
- Automatic retry with exponential backoff
- Offline detection
- Connection restoration notifications

```tsx
<NetworkErrorFallback
  error={error}
  resetErrorBoundary={resetErrorBoundary}
  endpoint="/api/users"
  requestMethod="GET"
  maxRetries={3}
  retryDelay={1000}
  onNetworkRestore={() => console.log('Network restored')}
/>
```

### PermissionErrorFallback

Handles authentication and authorization errors with:

- Different error type handling (auth/permission/forbidden/expired)
- Login redirect options
- Support contact integration
- Role-based messaging

```tsx
<PermissionErrorFallback
  error={error}
  resetErrorBoundary={resetErrorBoundary}
  errorType="authorization"
  requiredPermission="admin:read"
  userRole="user"
  onLogin={handleLogin}
  onContactSupport={handleSupport}
/>
```

## Higher-Order Component

### withErrorBoundary

Wrap any component with error boundaries.

```tsx
import { withErrorBoundary } from '@/components/ErrorBoundary'

const SafeUserCard = withErrorBoundary(UserCard, {
  errorType: 'network',
  level: 'component',
})

// Usage
<SafeUserCard userId="123" />
```

## Error Logging Integration

The system integrates with enhanced error logging that includes:

### Features

- **Offline Storage**: Errors are queued when offline and synced when connection returns
- **Retry Mechanisms**: Exponential backoff for failed error reports
- **Monitoring Integration**: Automatic Sentry and LogRocket integration
- **Context Preservation**: Full error context including component stack

### Configuration

```typescript
import { errorLogger } from '@/shared/services/errorLogging'

// Configure error logging
const logger = new ErrorLoggingService({
  enableConsoleLogging: true,
  enableRemoteLogging: true,
  enableOfflineStorage: true,
  enableMonitoringIntegration: true,
  maxRetries: 3,
  retryDelay: 1000,
  batchSize: 10,
  flushInterval: 30000,
})
```

### Manual Error Logging

```typescript
import { errorLogger } from '@/shared/services/errorLogging'

// Log different types of errors
errorLogger.logError(error, errorInfo, context, 'high')
errorLogger.logNetworkError(error, '/api/users', 'GET', 500)
errorLogger.logAsyncError(error, { source: 'promise' }, 'critical')
```

## Monitoring Services Integration

### Setup

1. **Install dependencies** (optional - dynamically imported):

```bash
npm install @sentry/react logrocket
```

2. **Configure environment variables**:

```env
VITE_SENTRY_DSN=your_sentry_dsn
VITE_LOGROCKET_APP_ID=your_logrocket_app_id
VITE_APP_VERSION=1.0.0
```

3. **Initialize monitoring**:

```typescript
import { initializeErrorReporting, defaultErrorReportingConfig } from '@/services/monitoring'

// Initialize in your app
await initializeErrorReporting({
  ...defaultErrorReportingConfig,
  userId: user.id,
  userEmail: user.email,
})
```

### Features

- **Sentry Integration**: Error tracking, release monitoring, performance insights
- **LogRocket Integration**: Session recording, user interaction tracking
- **Automatic Context**: User info, session data, error breadcrumbs
- **Privacy-First**: Sensitive data sanitization
- **Development Mode**: Disabled by default in development

## Testing

### Unit Tests

```typescript
import { render, screen } from '@testing-library/react'
import { ErrorBoundaryConfig } from '@/components/ErrorBoundary'

const ThrowError = ({ shouldThrow }: { shouldThrow?: boolean }) => {
  if (shouldThrow) throw new Error('Test error')
  return <div>Success</div>
}

test('handles errors gracefully', () => {
  render(
    <ErrorBoundaryConfig errorType="network">
      <ThrowError shouldThrow={true} />
    </ErrorBoundaryConfig>
  )
  
  expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
})
```

### Integration Testing

```typescript
// Test error boundary with real components
test('API error boundary catches fetch failures', async () => {
  server.use(
    rest.get('/api/users', (req, res, ctx) => {
      return res(ctx.status(500))
    })
  )
  
  render(
    <APIErrorBoundary endpoint="/api/users">
      <UserList />
    </APIErrorBoundary>
  )
  
  await waitFor(() => {
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
  })
})
```

## Best Practices

### 1. Error Boundary Placement

```tsx
// ✅ Good: Granular boundaries
<div>
  <Header />
  <FeatureErrorBoundary featureName="Navigation">
    <Navigation />
  </FeatureErrorBoundary>
  <main>
    <RouteErrorBoundary routeName="Dashboard">
      <DashboardContent />
    </RouteErrorBoundary>
  </main>
</div>

// ❌ Avoid: Single boundary for everything
<AppErrorBoundary>
  <Header />
  <Navigation />
  <DashboardContent />
</AppErrorBoundary>
```

### 2. Error Type Selection

```tsx
// ✅ Good: Specific error types
<ErrorBoundaryConfig errorType="network" endpoint="/api/data">
  <DataComponent />
</ErrorBoundaryConfig>

<ErrorBoundaryConfig errorType="permission" requiredPermission="admin:read">
  <AdminComponent />
</ErrorBoundaryConfig>

// ✅ Good: Default for unknown error types
<ErrorBoundaryConfig>
  <GenericComponent />
</ErrorBoundaryConfig>
```

### 3. Context Preservation

```tsx
// ✅ Good: Provide useful context
<ErrorBoundaryConfig 
  name="UserDashboard"
  level="feature"
  onError={(error, errorInfo) => {
    analytics.track('error_occurred', {
      feature: 'user_dashboard',
      error: error.message,
      userId: user.id,
    })
  }}
>
  <UserDashboard />
</ErrorBoundaryConfig>
```

### 4. Recovery Mechanisms

```tsx
// ✅ Good: Provide multiple recovery options
<ErrorBoundaryConfig 
  errorType="network"
  maxRetries={3}
  onNetworkRestore={() => {
    // Refresh data when network returns
    queryClient.invalidateQueries('users')
  }}
>
  <UserList />
</ErrorBoundaryConfig>
```

## Troubleshooting

### Common Issues

1. **Error boundaries not catching errors**
    - Check that errors occur during render, not in event handlers
    - Use `useErrorBoundary` hook for event handler errors

2. **Monitoring services not receiving errors**
    - Verify environment variables are set
    - Check network connectivity
    - Ensure services are initialized before use

3. **Offline errors not syncing**
    - Check localStorage permissions
    - Verify network status detection
    - Monitor console for sync attempts

### Debug Mode

Enable debug logging:

```typescript
// Enable verbose error logging
const errorLogger = new ErrorLoggingService({
  enableConsoleLogging: true,
  // ... other config
})

// Check error queue status
console.log(errorLogger.getOfflineQueueStatus())
console.log(errorLogger.getErrorStats())
```

## Migration Guide

### From Basic Error Boundaries

```typescript
// Before
<ErrorBoundary FallbackComponent={ErrorFallback}>
  <Component />
</ErrorBoundary>

// After
<ErrorBoundaryConfig level="component">
  <Component />
</ErrorBoundaryConfig>
```

### Adding Network Error Handling

```typescript
// Before
<Component />

// After
<APIErrorBoundary endpoint="/api/endpoint">
  <Component />
</APIErrorBoundary>
```

### Adding Monitoring

```typescript
// Before - basic error logging
console.error(error)

// After - comprehensive error tracking
import { errorLogger } from '@/shared/services/errorLogging'
errorLogger.logError(error, errorInfo, context, 'high')
```

## Performance Considerations

- Error boundaries have minimal performance impact
- Monitoring services use dynamic imports to avoid bundle bloat
- Offline error storage uses localStorage efficiently
- Retry mechanisms use exponential backoff to avoid spam

## Security Considerations

- Sensitive data is automatically sanitized in error reports
- Stack traces are only shown in development mode
- User data in error context is configurable
- Monitoring services respect privacy settings

## API Reference

See the TypeScript interfaces in the component files for complete API documentation:

- `ErrorBoundaryConfigProps`
- `NetworkErrorFallbackProps`
- `PermissionErrorFallbackProps`
- `ErrorLoggerConfig`
- `ErrorReportingConfig`