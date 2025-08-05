import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import { CssBaseline } from '@mui/material'
import { HomePage, LoginPage, RegisterPage } from '@features/auth'
import { DashboardPage, DiscoverPage } from '@features/hive'
import { AppLayout } from '@shared/layout'
import { PWAProvider, PWAUpdateNotification } from '@shared/pwa'

// Create Material UI theme
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#fafafa',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
    h5: {
      fontWeight: 600,
    },
  },
  shape: {
    borderRadius: 8,
  },
})

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
            <AppLayout>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/hives" element={<DashboardPage />} />
                <Route path="/discover" element={<DiscoverPage />} />
              </Routes>
            </AppLayout>
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