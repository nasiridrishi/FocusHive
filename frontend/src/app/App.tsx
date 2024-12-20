import React, {Suspense, useEffect} from 'react'
import {BrowserRouter as Router} from 'react-router-dom'
import {QueryClientProvider} from '@tanstack/react-query'
import {ThemeProvider} from '@mui/material/styles'
import {CssBaseline} from '@mui/material'
import {PWAProvider, PWAUpdateNotification} from '@shared/pwa'
import {I18nProvider} from '@shared/components/i18n'
import {createNoFocusTheme} from '@shared/theme/noFocusTheme'
import {AppLevelErrorBoundary} from '@shared/components/error-boundary'
import EnvironmentProvider from '../providers/EnvironmentProvider'
import {AuthProvider} from '../features/auth/contexts'
// Import i18n configuration
import '../lib/i18n'
import {featurePreloader, initializeBundleOptimization} from '../utils/bundleOptimization'
import {queryClient} from '../lib/queryClient'
import {AuthenticatedApp} from './AuthenticatedApp'

// Lazy load React Query Devtools only in development
const ReactQueryDevtools = import.meta.env.DEV
    ? React.lazy(() => import('@tanstack/react-query-devtools').then(module => ({
      default: module.ReactQueryDevtools
    })))
    : null

// Create theme with no focus highlights
const theme = createNoFocusTheme()

function App(): React.ReactElement {
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
                <CssBaseline/>
                <PWAProvider serviceWorkerOptions={{immediate: true}}>
                  <AuthProvider>
                    <Router
                      future={{
                        v7_startTransition: true,
                        v7_relativeSplatPath: true
                      }}
                    >
                      <AuthenticatedApp />
                    </Router>
                  </AuthProvider>
                  {/* PWA Update Notifications */}
                  <PWAUpdateNotification
                      anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}
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