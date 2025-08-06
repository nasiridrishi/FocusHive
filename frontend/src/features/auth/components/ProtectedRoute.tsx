import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Box, CircularProgress, Typography } from '@mui/material'

interface ProtectedRouteProps {
  children: React.ReactNode
  isAuthenticated: boolean
  isLoading?: boolean
  requireAuth?: boolean
  redirectTo?: string
}

export default function ProtectedRoute({
  children,
  isAuthenticated,
  isLoading = false,
  requireAuth = true,
  redirectTo = '/login'
}: ProtectedRouteProps) {
  const location = useLocation()

  // Show loading spinner while authentication status is being determined
  if (isLoading) {
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
          Loading...
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