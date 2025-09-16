// Spotify Connect Button Component
// Handles Spotify authentication and connection UI

import React from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Typography
} from '@mui/material'
import {
  CheckCircle as ConnectedIcon,
  Error as ErrorIcon,
  Login as LoginIcon,
  Logout as LogoutIcon,
  MusicNote as SpotifyIcon
} from '@mui/icons-material'
import {useSpotify} from '../../context/useSpotifyContext'

interface SpotifyConnectButtonProps {
  variant?: 'button' | 'card'
  size?: 'small' | 'medium' | 'large'
  showStatus?: boolean
  showDetails?: boolean
  onConnect?: () => void
  onDisconnect?: () => void
}

export const SpotifyConnectButton: React.FC<SpotifyConnectButtonProps> = ({
                                                                            variant = 'button',
                                                                            size = 'medium',
                                                                            showStatus = true,
                                                                            showDetails = false,
                                                                            onConnect,
                                                                            onDisconnect
                                                                          }) => {
  const {
    state,
    login,
    logout,
    initializePlayer,
    transferPlaybackHere
  } = useSpotify()

  const handleConnect = async () => {
    if (state.auth.isAuthenticated) {
      if (!state.player.isConnected) {
        await initializePlayer()
      } else {
        await transferPlaybackHere()
      }
    } else {
      login()
    }
    onConnect?.()
  }

  const handleDisconnect = (): void => {
    logout()
    onDisconnect?.()
  }

  const getStatusColor = (): 'success' | 'warning' | 'error' | 'default' => {
    if (state.error) return 'error'
    if (state.player.isConnected) return 'success'
    if (state.auth.isAuthenticated) return 'warning'
    return 'default'
  }

  const getStatusText = (): string => {
    if (state.isLoading) return 'Connecting...'
    if (state.error) return 'Connection Error'
    if (state.player.isConnected) return 'Connected'
    if (state.auth.isAuthenticated) return 'Authenticated'
    return 'Not Connected'
  }

  const getButtonText = (): string => {
    if (state.isLoading) return 'Connecting...'
    if (state.auth.isAuthenticated) {
      if (state.player.isConnected) return 'Transfer Playback'
      return 'Connect Player'
    }
    return 'Connect Spotify'
  }

  const getButtonIcon = (): React.ReactElement => {
    if (state.isLoading) return <CircularProgress size={20}/>
    if (state.player.isConnected) return <ConnectedIcon/>
    if (state.auth.isAuthenticated) return <SpotifyIcon/>
    return <LoginIcon/>
  }

  if (variant === 'card') {
    return (
        <Card
            variant="outlined"
            sx={{
              maxWidth: 400,
              transition: 'all 0.2s ease-in-out',
              '&:hover': {
                transform: 'translateY(-2px)',
                boxShadow: 2
              }
            }}
        >
          <CardContent>
            <Box display="flex" alignItems="center" gap={2} mb={2}>
              <SpotifyIcon color="primary" fontSize="large"/>
              <Box>
                <Typography variant="h6" component="h2">
                  Spotify Integration
                </Typography>
                {showStatus && (
                    <Chip
                        label={getStatusText()}
                        color={getStatusColor()}
                        size="small"
                        variant="outlined"
                    />
                )}
              </Box>
            </Box>

            {state.error && (
                <Alert
                    severity="error"
                    sx={{mb: 2}}
                    icon={<ErrorIcon/>}
                >
                  {state.error}
                </Alert>
            )}

            {showDetails && (
                <Box mb={2}>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    {state.auth.isAuthenticated
                        ? `Connected as ${state.auth.user?.display_name || 'Spotify User'}`
                        : 'Connect your Spotify account for enhanced music features'
                    }
                  </Typography>

                  {state.auth.isAuthenticated && (
                      <Box mt={1}>
                        <Typography variant="caption" display="block">
                          Account Type: {state.auth.isPremium ? 'Premium' : 'Free'}
                        </Typography>
                        {state.connection.deviceName && (
                            <Typography variant="caption" display="block">
                              Device: {state.connection.deviceName}
                            </Typography>
                        )}
                      </Box>
                  )}

                  {!state.auth.isPremium && state.auth.isAuthenticated && (
                      <Alert severity="info" sx={{mt: 1}}>
                        <Typography variant="caption">
                          Premium account required for full playback control
                        </Typography>
                      </Alert>
                  )}
                </Box>
            )}

            <Box display="flex" gap={1}>
              <Button
                  variant="contained"
                  color="primary"
                  startIcon={getButtonIcon()}
                  onClick={handleConnect}
                  disabled={state.isLoading}
                  fullWidth
                  size={size}
              >
                {getButtonText()}
              </Button>

              {state.auth.isAuthenticated && (
                  <Button
                      variant="outlined"
                      color="secondary"
                      startIcon={<LogoutIcon/>}
                      onClick={handleDisconnect}
                      disabled={state.isLoading}
                  >
                    Disconnect
                  </Button>
              )}
            </Box>
          </CardContent>
        </Card>
    )
  }

  // Button variant
  return (
      <Box display="flex" alignItems="center" gap={1}>
        <Button
            variant={state.auth.isAuthenticated ? 'outlined' : 'contained'}
            color="primary"
            startIcon={getButtonIcon()}
            onClick={handleConnect}
            disabled={state.isLoading}
            size={size}
            sx={{
              backgroundColor: state.auth.isAuthenticated ? undefined : '#1DB954',
              '&:hover': {
                backgroundColor: state.auth.isAuthenticated ? undefined : '#1ed760',
              }
            }}
        >
          {getButtonText()}
        </Button>

        {state.auth.isAuthenticated && (
            <Button
                variant="outlined"
                color="secondary"
                startIcon={<LogoutIcon/>}
                onClick={handleDisconnect}
                disabled={state.isLoading}
                size={size}
            >
              Disconnect
            </Button>
        )}

        {showStatus && (
            <Chip
                label={getStatusText()}
                color={getStatusColor()}
                size="small"
                variant="outlined"
            />
        )}
      </Box>
  )
}

export default SpotifyConnectButton