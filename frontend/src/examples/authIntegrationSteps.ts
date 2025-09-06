// Alternative: Step-by-step integration for existing apps
export const AuthIntegrationSteps = {
  /**
   * Step 1: Wrap your app with AuthProvider
   * Place this at the root level, after ThemeProvider but before Router
   */
  step1WrapWithAuthProvider: `
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
   * Step 2: Create authentication pages
   * Use the pre-built pages or customize them
   */
  step2CreateAuthPages: `
    import { LoginPage } from '@features/auth/pages/LoginPage';
    import { RegisterPage } from '@features/auth/pages/RegisterPage';
    
    // In your routes:
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
  `,

  /**
   * Step 3: Protect authenticated routes
   * Use ProtectedRoute wrapper for authenticated-only pages
   */
  step3ProtectRoutes: `
    import { ProtectedRoute } from '@features/auth/components/ProtectedRoute';
    
    <Route element={<ProtectedRoute />}>
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/profile" element={<ProfilePage />} />
    </Route>
  `,

  /**
   * Step 4: Use auth hooks in components
   * Access auth state and actions anywhere
   */
  step4UseAuthHooks: `
    import { useAuth } from '@features/auth/hooks/useAuth';
    
    function MyComponent() {
      const { authState, logout } = useAuth();
      
      if (authState.isAuthenticated) {
        return <div>Welcome, {authState.user?.email}!</div>;
      }
      
      return <div>Please log in</div>;
    }
  `,

  /**
   * Step 5: Configure API interceptors
   * Automatically add auth tokens to API requests
   */
  step5ConfigureAPI: `
    import { setupApiInterceptors } from '@services/api/auth';
    
    // Call this once when your app initializes:
    setupApiInterceptors();
  `
};