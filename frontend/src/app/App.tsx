import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider } from '@mui/material/styles'
import { CssBaseline } from '@mui/material'
import { HomePage, LoginPage, RegisterPage } from '@features/auth'
import { DashboardPage, DiscoverPage } from '@features/hive'
import { GamificationDemo } from '@features/gamification'
import { ResponsiveLayout } from '@shared/layout'
import { PWAProvider, PWAUpdateNotification } from '@shared/pwa'
import { createLightTheme } from '@shared/theme'

// Create comprehensive responsive theme
const theme = createLightTheme()

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <PWAProvider serviceWorkerOptions={{ immediate: true }}>
          <Router>
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
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/hives" element={<DashboardPage />} />
                <Route path="/discover" element={<DiscoverPage />} />
                <Route path="/gamification" element={<GamificationDemo />} />
              </Routes>
            </ResponsiveLayout>
          </Router>
          {/* PWA Update Notifications */}
          <PWAUpdateNotification 
            anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
            messages={{
              updateAvailable: 'A new version of FocusHive is available with improved features!',
              offlineReady: 'FocusHive is now ready to work offline.',
              updating: 'Updating FocusHive to the latest version...',
            }}
          />
          <ReactQueryDevtools initialIsOpen={false} />
        </PWAProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export default App