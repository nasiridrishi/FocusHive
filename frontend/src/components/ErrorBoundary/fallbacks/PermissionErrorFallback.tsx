import React, {useCallback, useState} from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Stack,
  Typography,
  useTheme,
} from '@mui/material'
import {
  ContactSupport as SupportIcon,
  Home as HomeIcon,
  Lock as LockIcon,
  Login as LoginIcon,
  Person as PersonIcon,
  Refresh as RefreshIcon,
  Security as SecurityIcon,
} from '@mui/icons-material'
import {FallbackProps} from 'react-error-boundary'

export interface PermissionErrorFallbackProps extends FallbackProps {
  title?: string
  subtitle?: string
  showLoginButton?: boolean
  showRefreshButton?: boolean
  showHomeButton?: boolean
  showSupportButton?: boolean
  onLogin?: () => void
  onContactSupport?: () => void
  errorType?: 'authentication' | 'authorization' | 'forbidden' | 'expired'
  requiredPermission?: string
  userRole?: string
}

type PermissionErrorType = 'authentication' | 'authorization' | 'forbidden' | 'expired'

/**
 * Specialized Error Fallback for Permission and Authentication Errors
 * Handles different types of access control errors with appropriate recovery options
 */
export const PermissionErrorFallback: React.FC<PermissionErrorFallbackProps> = ({
                                                                                  error,
                                                                                  resetErrorBoundary,
                                                                                  title,
                                                                                  subtitle,
                                                                                  showLoginButton = true,
                                                                                  showRefreshButton = true,
                                                                                  showHomeButton = true,
                                                                                  showSupportButton = true,
                                                                                  onLogin,
                                                                                  onContactSupport,
                                                                                  errorType = 'authentication',
                                                                                  requiredPermission,
                                                                                  userRole,
                                                                                }) => {
  const theme = useTheme()
  const [isLoggingIn, setIsLoggingIn] = useState(false)

  // Get error-specific content
  const getErrorContent = (type: PermissionErrorType): {
    title: string;
    subtitle: string;
    icon: React.ReactElement;
    severity: 'error' | 'warning' | 'info';
    color: 'error' | 'warning' | 'info';
  } => {
    switch (type) {
      case 'authentication':
        return {
          title: title || 'Authentication Required',
          subtitle: subtitle || 'You need to sign in to access this feature.',
          icon: <LoginIcon/>,
          severity: 'warning' as const,
          color: 'warning' as const,
        }
      case 'authorization':
        return {
          title: title || 'Access Denied',
          subtitle: subtitle || 'You do not have permission to access this resource.',
          icon: <SecurityIcon/>,
          severity: 'error' as const,
          color: 'error' as const,
        }
      case 'forbidden':
        return {
          title: title || 'Forbidden',
          subtitle: subtitle || 'This action is restricted for your account level.',
          icon: <LockIcon/>,
          severity: 'error' as const,
          color: 'error' as const,
        }
      case 'expired':
        return {
          title: title || 'Session Expired',
          subtitle: subtitle || 'Your session has expired. Please sign in again.',
          icon: <PersonIcon/>,
          severity: 'info' as const,
          color: 'info' as const,
        }
      default:
        return {
          title: title || 'Permission Error',
          subtitle: subtitle || 'Unable to access this resource.',
          icon: <LockIcon/>,
          severity: 'warning' as const,
          color: 'warning' as const,
        }
    }
  }

  const errorContent = getErrorContent(errorType)

  const handleLogin = useCallback(async () => {
    setIsLoggingIn(true)
    try {
      if (onLogin) {
        await onLogin()
      } else {
        // Default: redirect to login page
        window.location.href = '/login'
      }
    } catch {
      // console.error('Login failed')
    } finally {
      setIsLoggingIn(false)
    }
  }, [onLogin])

  const handleContactSupport = useCallback(() => {
    if (onContactSupport) {
      onContactSupport()
    } else {
      // Default: open support email
      window.location.href = 'mailto:support@focushive.com?subject=Permission Error&body=Error: ' + error.message
    }
  }, [onContactSupport, error.message])

  const handleGoHome = useCallback(() => {
    window.location.href = '/'
  }, [])

  const getRecommendedActions = (type: PermissionErrorType): string[] => {
    switch (type) {
      case 'authentication':
        return [
          'Sign in with your account credentials',
          'Create a new account if you don\'t have one',
          'Reset your password if you\'ve forgotten it',
        ]
      case 'authorization':
        return [
          'Contact your administrator to request access',
          'Check if you\'re signed in with the correct account',
          'Verify your account has the required permissions',
        ]
      case 'forbidden':
        return [
          'Contact support to upgrade your account',
          'Check if this feature requires a premium subscription',
          'Verify you\'re using the correct workspace',
        ]
      case 'expired':
        return [
          'Sign in again to refresh your session',
          'Clear your browser cache and cookies',
          'Try using an incognito/private browsing window',
        ]
      default:
        return [
          'Refresh the page and try again',
          'Sign out and sign in again',
          'Contact support if the problem persists',
        ]
    }
  }

  const recommendedActions = getRecommendedActions(errorType)

  return (
      <Box
          role="alert"
          sx={{
            minHeight: '300px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            p: 2,
          }}
      >
        <Card
            sx={{
              maxWidth: 600,
              width: '100%',
              boxShadow: theme.shadows[4],
            }}
        >
          <CardContent sx={{p: 3}}>
            {/* Header */}
            <Box sx={{display: 'flex', alignItems: 'center', mb: 2}}>
              <Box
                  sx={{
                    color: `${errorContent.color}.main`,
                    fontSize: 32,
                    mr: 2,
                    display: 'flex',
                    alignItems: 'center',
                  }}
              >
                {errorContent.icon}
              </Box>
              <Box sx={{flex: 1}}>
                <Typography variant="h6" color="text.primary" gutterBottom>
                  {errorContent.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {errorContent.subtitle}
                </Typography>
              </Box>
            </Box>

            {/* Metadata */}
            <Box sx={{mb: 2}}>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                <Chip
                    size="small"
                    label={`Type: ${errorType.charAt(0).toUpperCase() + errorType.slice(1)}`}
                    color={errorContent.color}
                    variant="outlined"
                />
                {requiredPermission && (
                    <Chip
                        size="small"
                        label={`Required: ${requiredPermission}`}
                        variant="outlined"
                    />
                )}
                {userRole && (
                    <Chip
                        size="small"
                        label={`Current Role: ${userRole}`}
                        color="info"
                        variant="outlined"
                    />
                )}
              </Stack>
            </Box>

            {/* Error Details */}
            <Alert severity={errorContent.severity} sx={{mb: 2}}>
              <Typography variant="body2">
                <strong>Error:</strong> {error.message}
              </Typography>
              {errorType === 'authorization' && requiredPermission && (
                  <Typography variant="body2" sx={{mt: 1}}>
                    This action requires the <strong>{requiredPermission}</strong> permission.
                  </Typography>
              )}
            </Alert>

            {/* Action Buttons */}
            <Stack direction="row" spacing={1} flexWrap="wrap" sx={{mb: 3}}>
              {showLoginButton && (errorType === 'authentication' || errorType === 'expired') && (
                  <Button
                      variant="contained"
                      startIcon={<LoginIcon/>}
                      onClick={handleLogin}
                      disabled={isLoggingIn}
                      color="primary"
                  >
                    {isLoggingIn ? 'Signing In...' : 'Sign In'}
                  </Button>
              )}

              {showRefreshButton && (
                  <Button
                      variant="outlined"
                      startIcon={<RefreshIcon/>}
                      onClick={resetErrorBoundary}
                      color="primary"
                  >
                    Retry
                  </Button>
              )}

              {showHomeButton && (
                  <Button
                      variant="outlined"
                      startIcon={<HomeIcon/>}
                      onClick={handleGoHome}
                  >
                    Go Home
                  </Button>
              )}

              {showSupportButton && (errorType === 'authorization' || errorType === 'forbidden') && (
                  <Button
                      variant="outlined"
                      startIcon={<SupportIcon/>}
                      onClick={handleContactSupport}
                      color="secondary"
                  >
                    Contact Support
                  </Button>
              )}
            </Stack>

            <Divider sx={{mb: 2}}/>

            {/* Recommended Actions */}
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Recommended Actions:
              </Typography>
              <Typography variant="body2" color="text.secondary" component="ul" sx={{pl: 2, mb: 0}}>
                {recommendedActions.map((action, index) => (
                    <li key={index}>{action}</li>
                ))}
              </Typography>
            </Box>

            {/* Additional Help */}
            {(errorType === 'authorization' || errorType === 'forbidden') && (
                <Alert severity="info" sx={{mt: 2}}>
                  <Typography variant="body2">
                    If you believe this is an error, please contact your administrator or support
                    team.
                    Include the error details and your account information.
                  </Typography>
                </Alert>
            )}
          </CardContent>
        </Card>
      </Box>
  )
}