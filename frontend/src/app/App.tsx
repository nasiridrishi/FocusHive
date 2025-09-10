import React, { useEffect, Suspense } from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material/styles'
import { CssBaseline } from '@mui/material'
import { ResponsiveLayout } from '@shared/layout'
import { PWAProvider, PWAUpdateNotification } from '@shared/pwa'
import { I18nProvider } from '@shared/components/i18n'
import { createAccessibleLightTheme } from '@shared/accessibility/theme/accessibleTheme'
import { AppLevelErrorBoundary, RouteLevelErrorBoundary } from '@shared/components/error-boundary'
import EnvironmentProvider from '../providers/EnvironmentProvider'
import { AuthProvider } from '../features/auth/contexts'
import { SkipLink } from '@shared/components/SkipLink'
// Import i18n configuration
import '../lib/i18n'
import {
  LazyHomePage,
  LazyLoginPage,
  LazyRegisterPage,
  LazyDashboardPage,
  LazyDiscoverPage,
  LazyErrorBoundaryDemo
} from './routes/LazyRoutes'
import { LazyGamificationDemo } from '@shared/components/lazy-features'
import { initializeBundleOptimization, featurePreloader } from '../utils/bundleOptimization'
import { queryClient } from '../lib/queryClient'

// Lazy load React Query Devtools only in development
const ReactQueryDevtools = import.meta.env.DEV 
  ? React.lazy(() => import('@tanstack/react-query-devtools').then(module => ({ 
      default: module.ReactQueryDevtools 
    })))
  : null

// Create accessible responsive theme
const theme = createAccessibleLightTheme()

function App() {
  // Initialize bundle optimization on app start
  useEffect(() => {
    initializeBundleOptimization({
      criticalRoutes: true,
      commonIcons: true,
      charts: false, // Load on demand
      datePickers: false, // Load on demand
      adaptive: true,
      heavyFeatures: false, // Load heavy features on user interaction
      utilityLibs: true, // Preload essential utilities
      smartPreloading: true // Use connection-aware preloading
    })
  }, [])

  // Track route changes for adaptive preloading
  useEffect(() => {
    featurePreloader.trackUserInteraction('app-initialized')
  }, [])

  return (
    <EnvironmentProvider>
      <AppLevelErrorBoundary>
        <I18nProvider>
          <QueryClientProvider client={queryClient}>
            <ThemeProvider theme={theme}>
              <CssBaseline />
              <PWAProvider serviceWorkerOptions={{ immediate: true }}>
                <AuthProvider>
                  <Router>
                    <SkipLink />
                    <ResponsiveLayout
                  currentUser={{
                    name: 'John Doe',
                    email: 'john.doe@example.com',
                    avatar: undefined,
                  }}
                  isConnected={true}
                  notificationCount={3}
                >
                  <Routes>
                    <Route 
                      path="/" 
                      element={
                        <RouteLevelErrorBoundary routeName="Home">
                          <LazyHomePage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/login" 
                      element={
                        <RouteLevelErrorBoundary routeName="Login">
                          <LazyLoginPage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/register" 
                      element={
                        <RouteLevelErrorBoundary routeName="Register">
                          <LazyRegisterPage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/dashboard" 
                      element={
                        <RouteLevelErrorBoundary routeName="Dashboard">
                          <LazyDashboardPage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/hives" 
                      element={
                        <RouteLevelErrorBoundary routeName="Hives">
                          <LazyDashboardPage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/discover" 
                      element={
                        <RouteLevelErrorBoundary routeName="Discover">
                          <LazyDiscoverPage />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/gamification" 
                      element={
                        <RouteLevelErrorBoundary routeName="Gamification">
                          <LazyGamificationDemo />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                    <Route 
                      path="/error-boundary-demo" 
                      element={
                        <RouteLevelErrorBoundary routeName="ErrorBoundaryDemo">
                          <LazyErrorBoundaryDemo />
                        </RouteLevelErrorBoundary>
                      } 
                    />
                  </Routes>
                    </ResponsiveLayout>
                  </Router>
                </AuthProvider>
              {/* PWA Update Notifications */}
              <PWAUpdateNotification 
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                messages={{
                  updateAvailable: 'A new version of FocusHive is available with improved features!',
                  offlineReady: 'FocusHive is now ready to work offline.',
                  updating: 'Updating FocusHive to the latest version...',
                }}
              />
              {/* Conditionally render React Query Devtools only in development */}
              {ReactQueryDevtools && (
                <Suspense fallback={null}>
                  <ReactQueryDevtools 
                    initialIsOpen={false} 
                    buttonPosition="bottom-right"
                    position="bottom"
                    client={queryClient}
                    errorTypes={[
                      {
                        name: 'Network Error',
                        initializer: () => new Error('Simulated network error'),
                      },
                      {
                        name: 'Timeout Error', 
                        initializer: () => new Error('Request timeout'),
                      },
                    ]}
                  />
                </Suspense>
              )}
                </PWAProvider>
              </ThemeProvider>
            </QueryClientProvider>
          </I18nProvider>
        </AppLevelErrorBoundary>
      </EnvironmentProvider>
    )
}

export default App