import React, {useEffect, useState} from 'react'
import {Navigate, useLocation} from 'react-router-dom'
import {Alert, Box, CircularProgress, Typography} from '@mui/material'
import {useAuthState} from '../hooks/useAuth'
import {tokenManager} from '../../../utils/tokenManager'

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
                                       }: ProtectedRouteProps): JSX.Element {
  const location = useLocation()
  const [tokenValidation, setTokenValidation] = useState<{
    isValidating: boolean;
    isValid: boolean;
    error?: string;
  }>({isValidating: true, isValid: false})

  // Use auth context if props not provided
  const authState = useAuthState()

  // Enhanced token validation
  useEffect(() => {
    const validateTokens = async (): Promise<void> => {
      setTokenValidation({isValidating: true, isValid: false})

      try {
        // Check if we have valid tokens
        const hasTokens = tokenManager.hasValidTokens()

        if (!hasTokens) {
          setTokenValidation({
            isValidating: false,
            isValid: false,
            error: 'No valid authentication tokens found'
          })
          return
        }

        // Check token expiration
        const tokenInfo = tokenManager.getTokenExpirationInfo()

        if (tokenInfo.needsRefresh) {
          // Token needs refresh, but we have a refresh token
          if (tokenInfo.refreshTokenExpiresAt && tokenInfo.refreshTokenExpiresAt > new Date()) {
            setTokenValidation({
              isValidating: false,
              isValid: true // Auth context will handle refresh
            })
          } else {
            setTokenValidation({
              isValidating: false,
              isValid: false,
              error: 'Authentication session has expired'
            })
          }
        } else {
          setTokenValidation({
            isValidating: false,
            isValid: true
          })
        }
      } catch (error) {
        setTokenValidation({
          isValidating: false,
          isValid: false,
          error: error instanceof Error ? error.message : 'Token validation failed'
        })
      }
    }

    // Only validate if we're supposed to require auth
    if (requireAuth) {
      validateTokens()
    } else {
      setTokenValidation({isValidating: false, isValid: true})
    }
  }, [requireAuth, authState.isAuthenticated])

  // Determine final auth state
  const isAuthenticated = propIsAuthenticated ?? (authState.isAuthenticated && tokenValidation.isValid)
  const isLoading = propIsLoading ?? (authState.isLoading || tokenValidation.isValidating)
  const error = authState.error || tokenValidation.error

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
          <CircularProgress size={48}/>
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
          <Alert severity="error" sx={{maxWidth: 400}}>
            {error}
          </Alert>
          <Typography variant="body2" color="text.secondary" align="center">
            Please try refreshing the page or{' '}
            <Box
                component="button"
                sx={{
                  background: 'none',
                  border: 'none',
                  color: 'inherit',
                  cursor: 'pointer',
                  textDecoration: 'underline',
                  fontWeight: 'bold',
                  fontSize: 'inherit',
                  fontFamily: 'inherit',
                  padding: 0,
                  '&:focus': {
                    outline: '2px solid currentColor',
                    outlineOffset: '2px',
                  },
                }}
                onClick={() => window.location.href = '/login'}
            >
              sign in again
            </Box>
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
            state={{from: location}}
            replace
        />
    )
  }

  // If route requires no authentication (like login/register) and user is authenticated
  if (!requireAuth && isAuthenticated) {
    // Redirect to the dashboard or the originally intended location
    const from = location.state?.from?.pathname || '/dashboard'
    return <Navigate to={from} replace/>
  }

  // User has proper access, render the component
  return <>{children}</>
}