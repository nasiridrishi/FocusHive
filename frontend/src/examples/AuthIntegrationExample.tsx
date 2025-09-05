/**
 * Authentication Integration Example
 * 
 * This example demonstrates how to integrate the complete authentication system
 * into your React application with proper setup and configuration.
 */

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline, Box } from '@mui/material';
import { theme } from '@shared/theme';

// Import authentication components and providers
import { 
  AuthProvider, 
  useAuth, 
  ProtectedRoute,
  LoginPage, 
  RegisterPage,
  UserProfileMenu 
} from '@features/auth';

// Example dashboard and other pages
import DashboardPage from '@features/hive/pages/DashboardPage';
import DiscoverPage from '@features/hive/pages/DiscoverPage';
import Header from '@shared/layout/Header';

/**
 * Protected App Content - Only rendered when authenticated
 */
function AuthenticatedApp() {
  const { authState, logout } = useAuth();
  const [isDarkMode, setIsDarkMode] = React.useState(false);

  const handleLogout = async () => {
    try {
      await logout();
      // Navigation will be handled automatically by the auth context
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  const handleThemeToggle = () => {
    setIsDarkMode(prev => !prev);
  };

  if (!authState.user) {
    return null; // This shouldn't happen in protected routes, but just in case
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header with user profile menu */}
      <Header>
        <UserProfileMenu
          user={authState.user}
          presenceStatus="online" // You would get this from presence context
          onLogout={handleLogout}
          onThemeToggle={handleThemeToggle}
          isDarkMode={isDarkMode}
          notificationCount={3} // You would get this from notifications context
        />
      </Header>

      {/* Main content */}
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Routes>
          {/* Protected routes */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute requireAuth={true}>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/discover"
            element={
              <ProtectedRoute requireAuth={true}>
                <DiscoverPage />
              </ProtectedRoute>
            }
          />
          {/* Default redirect to dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Box>
    </Box>
  );
}

/**
 * Authentication Routes - Login, Register, etc.
 */
function AuthRoutes() {
  return (
    <Routes>
      {/* Public auth routes - redirect if already authenticated */}
      <Route
        path="/login"
        element={
          <ProtectedRoute requireAuth={false} redirectTo="/dashboard">
            <LoginPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/register"
        element={
          <ProtectedRoute requireAuth={false} redirectTo="/dashboard">
            <RegisterPage />
          </ProtectedRoute>
        }
      />
      {/* Default redirect to login for unauthenticated users */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

/**
 * Main App Router - Handles authentication-based routing
 */
function AppRouter() {
  const { authState } = useAuth();

  // Show loading screen while checking authentication
  if (authState.isLoading) {
    return (
      <ProtectedRoute 
        isAuthenticated={false} 
        isLoading={true}
        fallback={
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '100vh',
              backgroundColor: 'background.default'
            }}
          >
            {/* Your custom loading component can go here */}
            <div>Loading FocusHive...</div>
          </Box>
        }
      >
        <div />
      </ProtectedRoute>
    );
  }

  // Route based on authentication status
  return authState.isAuthenticated ? <AuthenticatedApp /> : <AuthRoutes />;
}

/**
 * Complete Authentication Integration Example
 * 
 * Usage in your main App.tsx:
 * 
 * ```tsx
 * import AuthIntegrationExample from './examples/AuthIntegrationExample';
 * 
 * function App() {
 *   return <AuthIntegrationExample />;
 * }
 * ```
 */
function AuthIntegrationExample() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <AppRouter />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}

// Alternative: Step-by-step integration for existing apps
export const AuthIntegrationSteps = {
  /**
   * Step 1: Wrap your app with AuthProvider
   * Place this at the root level, after ThemeProvider but before Router
   */
  step1_WrapWithAuthProvider: `
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  `,

  /**
   * Step 2: Use authentication hooks in components
   */
  step2_UseAuthHooks: `
    import { useAuth } from '@features/auth';
    
    function MyComponent() {
      const { authState, login, logout } = useAuth();
      
      if (authState.isLoading) return <div>Loading...</div>;
      if (!authState.isAuthenticated) return <LoginForm onSubmit={login} />;
      
      return (
        <div>
          Welcome, {authState.user?.firstName}!
          <button onClick={logout}>Logout</button>
        </div>
      );
    }
  `,

  /**
   * Step 3: Protect routes with ProtectedRoute
   */
  step3_ProtectRoutes: `
    import { ProtectedRoute } from '@features/auth';
    
    <Routes>
      <Route path="/login" element={
        <ProtectedRoute requireAuth={false}>
          <LoginPage />
        </ProtectedRoute>
      } />
      
      <Route path="/dashboard" element={
        <ProtectedRoute requireAuth={true}>
          <DashboardPage />
        </ProtectedRoute>
      } />
    </Routes>
  `,

  /**
   * Step 4: Configure API services (already done in authApi.ts)
   * The authentication system automatically handles:
   * - Token storage and retrieval
   * - Automatic token refresh
   * - Request/response interceptors
   * - Error handling
   */
  step4_ApiConfiguration: `
    // Already configured in authApi.ts and httpInterceptors.ts
    // No additional setup required
    
    // Your other API services can use the same authenticated client:
    import { apiClient } from '@services/api';
    
    export const myApiService = {
      async getData() {
        const response = await apiClient.get('/my-endpoint');
        return response.data;
      }
    };
  `
};

export default AuthIntegrationExample;