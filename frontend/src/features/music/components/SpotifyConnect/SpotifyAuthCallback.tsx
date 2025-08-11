// Spotify Auth Callback Component
// Handles the OAuth callback from Spotify and processes the authorization code

import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { 
  Box, 
  Typography, 
  CircularProgress, 
  Alert, 
  Button,
  Card,
  CardContent
} from '@mui/material'
import { 
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  MusicNote as SpotifyIcon
} from '@mui/icons-material'
import { useSpotify } from '../../context/SpotifyContext'

interface SpotifyAuthCallbackProps {
  redirectTo?: string
  onSuccess?: () => void
  onError?: (error: string) => void
}

export const SpotifyAuthCallback: React.FC<SpotifyAuthCallbackProps> = ({
  redirectTo = '/music',
  onSuccess,
  onError
}) => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { handleAuthCallback, state } = useSpotify()
  const [status, setStatus] = useState<'processing' | 'success' | 'error'>('processing')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    const processCallback = async () => {
      try {
        // Check for error from Spotify
        const error = searchParams.get('error')
        if (error) {
          const errorDescription = searchParams.get('error_description') || 'Unknown error'
          throw new Error(`Spotify authorization failed: ${errorDescription}`)
        }

        // Get authorization code and state
        const code = searchParams.get('code')
        const state = searchParams.get('state')

        if (!code) {
          throw new Error('No authorization code received from Spotify')
        }

        if (!state) {
          throw new Error('No state parameter received (CSRF protection)')
        }

        // Process the callback
        const success = await handleAuthCallback(code, state)
        
        if (success) {
          setStatus('success')
          onSuccess?.()
          
          // Redirect after a short delay to show success message
          setTimeout(() => {
            navigate(redirectTo, { replace: true })
          }, 2000)
        } else {
          throw new Error('Authentication failed')
        }
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Authentication failed'
        setErrorMessage(message)
        setStatus('error')
        onError?.(message)
      }
    }

    processCallback()
  }, [searchParams, handleAuthCallback, navigate, redirectTo, onSuccess, onError])

  const handleRetry = () => {
    navigate('/music', { replace: true })
  }

  const handleGoBack = () => {
    navigate(-1)
  }

  if (status === 'processing') {
    return (
      <Box 
        display="flex" 
        flexDirection="column" 
        alignItems="center" 
        justifyContent="center" 
        minHeight="60vh"
        gap={3}
      >
        <SpotifyIcon sx={{ fontSize: 60, color: 'primary.main' }} />
        <CircularProgress size={40} />
        <Typography variant="h6" align="center">
          Connecting to Spotify...
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center">
          Please wait while we complete the authentication process
        </Typography>
      </Box>
    )
  }

  if (status === 'success') {
    return (
      <Box 
        display="flex" 
        flexDirection="column" 
        alignItems="center" 
        justifyContent="center" 
        minHeight="60vh"
        gap={3}
      >
        <SuccessIcon sx={{ fontSize: 60, color: 'success.main' }} />
        <Typography variant="h5" align="center" color="success.main">
          Successfully Connected!
        </Typography>
        <Typography variant="body1" align="center">
          Your Spotify account has been linked to FocusHive
        </Typography>
        {state.auth.user && (
          <Card variant="outlined" sx={{ mt: 2, maxWidth: 400 }}>
            <CardContent>
              <Typography variant="subtitle2" gutterBottom>
                Connected Account
              </Typography>
              <Typography variant="body2">
                {state.auth.user.display_name}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {state.auth.isPremium ? 'Premium' : 'Free'} Account
              </Typography>
            </CardContent>
          </Card>
        )}
        <Typography variant="body2" color="text.secondary" align="center">
          Redirecting to music player...
        </Typography>
      </Box>
    )
  }

  // Error state
  return (
    <Box 
      display="flex" 
      flexDirection="column" 
      alignItems="center" 
      justifyContent="center" 
      minHeight="60vh"
      gap={3}
      px={2}
    >
      <ErrorIcon sx={{ fontSize: 60, color: 'error.main' }} />
      
      <Typography variant="h5" align="center" color="error.main">
        Connection Failed
      </Typography>
      
      <Alert 
        severity="error" 
        sx={{ maxWidth: 500, width: '100%' }}
      >
        <Typography variant="body2">
          {errorMessage || 'Failed to connect to Spotify. Please try again.'}
        </Typography>
      </Alert>

      {state.auth.error && state.auth.error !== errorMessage && (
        <Alert 
          severity="warning" 
          sx={{ maxWidth: 500, width: '100%' }}
        >
          <Typography variant="body2">
            Additional error: {state.auth.error}
          </Typography>
        </Alert>
      )}

      <Box display="flex" gap={2} mt={2}>
        <Button
          variant="contained"
          color="primary"
          onClick={handleRetry}
          startIcon={<SpotifyIcon />}
        >
          Try Again
        </Button>
        
        <Button
          variant="outlined"
          color="secondary"
          onClick={handleGoBack}
        >
          Go Back
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary" align="center" mt={2}>
        If the problem persists, please check your internet connection or try again later
      </Typography>
    </Box>
  )
}

export default SpotifyAuthCallback