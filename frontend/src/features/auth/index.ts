// Auth context exports
export { AuthProvider } from './contexts/AuthContext'

// Auth hooks exports (from separate file to avoid Fast Refresh warnings)
export { useAuth, useAuthState, useAuthActions } from './hooks/useAuth'

// Auth contexts exports (from separate file to avoid Fast Refresh warnings)
export { AuthStateContext, AuthActionsContext } from './contexts/authContexts'

// Auth components exports
export { default as LoginForm } from './components/LoginForm'
export { default as RegisterForm } from './components/RegisterForm'
export { default as ProtectedRoute } from './components/ProtectedRoute'
export { default as UserProfileMenu } from './components/UserProfileMenu'

// Auth pages exports
export { default as HomePage } from './pages/HomePage'
export { default as LoginPage } from './pages/LoginPage'
export { default as RegisterPage } from './pages/RegisterPage'

// Auth services exports
export { default as authApiService } from '../../services/api/authApi'