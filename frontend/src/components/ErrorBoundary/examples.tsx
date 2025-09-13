/**
 * Error Boundary Usage Examples
 * Demonstrates how to integrate comprehensive error handling in the FocusHive application
 */

import React from 'react'
import {
  ErrorBoundaryConfig,
  APIErrorBoundary,
  AuthErrorBoundary,
  PermissionErrorBoundary,
  FeatureErrorBoundary,
  withErrorBoundary,
} from './index'

// Example 1: App-level error boundary with monitoring integration
export const AppWithErrorHandling: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <ErrorBoundaryConfig
      level="app"
      name="FocusHiveApp"
      onError={(error, errorInfo) => {
        // Custom error handling - could send to analytics
        console.error('App-level error:', error, errorInfo)
      }}
    >
      {children}
    </ErrorBoundaryConfig>
  )
}

// Example 2: Route-level error boundary with fallback navigation
export const RouteWithErrorHandling: React.FC<{ 
  children: React.ReactNode
  routeName: string 
}> = ({ children, routeName }) => {
  return (
    <ErrorBoundaryConfig
      level="route"
      name={`${routeName}Route`}
      resetKeys={[window.location.pathname]} // Reset when route changes
      onReset={() => {
        // Could refresh route data here
        console.log(`Resetting ${routeName} route after error`)
      }}
    >
      {children}
    </ErrorBoundaryConfig>
  )
}

// Example 3: API component with network error handling
export const UserListWithErrorHandling: React.FC = () => {
  return (
    <APIErrorBoundary 
      endpoint="/api/users" 
      method="GET"
    >
      <UserListComponent />
    </APIErrorBoundary>
  )
}

// Example 4: Protected component with authentication error handling
export const ProtectedDashboard: React.FC = () => {
  const handleLogin = () => {
    window.location.href = '/login'
  }

  return (
    <AuthErrorBoundary onLogin={handleLogin}>
      <DashboardComponent />
    </AuthErrorBoundary>
  )
}

// Example 5: Admin component with permission error handling
export const AdminPanel: React.FC = () => {
  const handleContactSupport = () => {
    window.open('mailto:support@focushive.com?subject=Access Request')
  }

  return (
    <PermissionErrorBoundary
      requiredPermission="admin:access"
      userRole="user" // This would come from auth context
      onContactSupport={handleContactSupport}
    >
      <AdminPanelComponent />
    </PermissionErrorBoundary>
  )
}

// Example 6: Feature with isolated error handling
export const ChatFeature: React.FC = () => {
  return (
    <FeatureErrorBoundary 
      featureName="Chat" 
      fallbackType="network"
    >
      <ChatComponent />
    </FeatureErrorBoundary>
  )
}

// Example 7: HOC usage for reusable components
const SafeUserCard = withErrorBoundary(UserCard, {
  level: 'component',
  errorType: 'default',
})

export const UserCardExample: React.FC<{ userId: string }> = ({ userId }) => {
  return <SafeUserCard userId={userId} />
}

// Example 8: Complex nested error boundaries
export const ComplexAppStructure: React.FC = () => {
  return (
    <AppWithErrorHandling>
      <Header />
      
      <FeatureErrorBoundary featureName="Navigation">
        <Navigation />
      </FeatureErrorBoundary>
      
      <main>
        <RouteWithErrorHandling routeName="Dashboard">
          <div className="dashboard-grid">
            
            <FeatureErrorBoundary featureName="UserStats" fallbackType="network">
              <APIErrorBoundary endpoint="/api/stats">
                <UserStats />
              </APIErrorBoundary>
            </FeatureErrorBoundary>
            
            <PermissionErrorBoundary requiredPermission="hive:create">
              <CreateHiveButton />
            </PermissionErrorBoundary>
            
            <FeatureErrorBoundary featureName="HiveList">
              <APIErrorBoundary endpoint="/api/hives" method="GET">
                <HiveList />
              </APIErrorBoundary>
            </FeatureErrorBoundary>
            
          </div>
        </RouteWithErrorHandling>
      </main>
      
      <Footer />
    </AppWithErrorHandling>
  )
}

// Example 9: Error boundary with custom fallback
export const CustomFallbackExample: React.FC = () => {
  const CustomErrorFallback = ({ error, resetErrorBoundary }: {
    error: Error
    resetErrorBoundary: () => void
  }) => (
    <div className="custom-error-fallback">
      <h2>Oops! Something went wrong in our chat feature</h2>
      <p>Don't worry, the rest of the app is still working.</p>
      <button onClick={resetErrorBoundary}>
        Try reloading the chat
      </button>
      <details>
        <summary>Error details</summary>
        <pre>{error.message}</pre>
      </details>
    </div>
  )

  return (
    <ErrorBoundaryConfig
      customFallback={CustomErrorFallback}
      level="feature"
      name="ChatWithCustomFallback"
    >
      <ChatComponent />
    </ErrorBoundaryConfig>
  )
}

// Example 10: Error boundary with recovery logic
export const ErrorBoundaryWithRecovery: React.FC = () => {
  const [retryCount, setRetryCount] = React.useState(0)

  const handleReset = React.useCallback(() => {
    setRetryCount(prev => prev + 1)
    
    // Could implement exponential backoff here
    if (retryCount < 3) {
      console.log(`Retry attempt ${retryCount + 1}`)
      // Refresh data or reset state
    } else {
      console.log('Max retries reached, redirecting to safe state')
      window.location.href = '/dashboard'
    }
  }, [retryCount])

  return (
    <ErrorBoundaryConfig
      level="feature"
      name="DataComponentWithRecovery"
      onReset={handleReset}
      resetKeys={[retryCount]} // Reset when retry count changes
    >
      <DataIntensiveComponent />
    </ErrorBoundaryConfig>
  )
}

// Mock components for examples (these would be your real components)
const UserListComponent = () => <div>User List</div>
const DashboardComponent = () => <div>Dashboard</div>
const AdminPanelComponent = () => <div>Admin Panel</div>
const ChatComponent = () => <div>Chat</div>
const UserCard = ({ userId }: { userId: string }) => <div>User Card: {userId}</div>
const Header = () => <div>Header</div>
const Navigation = () => <div>Navigation</div>
const Footer = () => <div>Footer</div>
const UserStats = () => <div>User Stats</div>
const CreateHiveButton = () => <button>Create Hive</button>
const HiveList = () => <div>Hive List</div>
const DataIntensiveComponent = () => <div>Data Component</div>