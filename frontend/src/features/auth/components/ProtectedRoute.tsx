import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Box, CircularProgress, Typography, Alert } from '@mui/material'
import { useAuthState } from '../contexts/AuthContext'

interface ProtectedRouteProps {
  children: React.ReactNode
  isAuthenticated?: boolean // Optional - will use auth context if not provided
  isLoading?: boolean // Optional - will use auth context if not provided
  requireAuth?: boolean
  redirectTo?: string
  fallback?: React.ReactNode // Custom loading component
}

export default function ProtectedRoute({
  children,
  isAuthenticated: propIsAuthenticated,
  isLoading: propIsLoading,
  requireAuth = true,
  redirectTo = '/login',
  fallback
}: ProtectedRouteProps) {
  const location = useLocation()
  
  // Use auth context if props not provided
  const authState = useAuthState()
  const isAuthenticated = propIsAuthenticated ?? authState.isAuthenticated
  const isLoading = propIsLoading ?? authState.isLoading
  const error = authState.error

  // Show custom fallback or default loading spinner while authentication status is being determined
  if (isLoading) {
    if (fallback) {
      return <>{fallback}</>
    }
    
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          gap: 2
        }}
      >
        <CircularProgress size={48} />
        <Typography variant="body1" color="text.secondary">
          Authenticating...
        </Typography>
      </Box>
    )
  }

  // Show authentication error if present
  if (error && requireAuth) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          gap: 2,
          px: 3
        }}
      >
        <Alert severity="error" sx={{ maxWidth: 400 }}>
          {error}
        </Alert>
        <Typography variant="body2" color="text.secondary" align="center">
          Please try refreshing the page or{' '}
          <strong 
            style={{ cursor: 'pointer', textDecoration: 'underline' }}
            onClick={() => window.location.href = '/login'}
          >
            sign in again
          </strong>
        </Typography>
      </Box>
    )
  }

  // If route requires authentication and user is not authenticated
  if (requireAuth && !isAuthenticated) {
    // Save the attempted location for redirect after login
    return (
      <Navigate
        to={redirectTo}
        state={{ from: location }}
        replace
      />
    )
  }

  // If route requires no authentication (like login/register) and user is authenticated
  if (!requireAuth && isAuthenticated) {
    // Redirect to the dashboard or the originally intended location
    const from = location.state?.from?.pathname || '/dashboard'
    return <Navigate to={from} replace />
  }

  // User has proper access, render the component
  return <>{children}</>
}