import React, { useState, useEffect, useCallback } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Chip,
  Stack,
  LinearProgress,
  useTheme,
} from '@mui/material'
import {
  SignalWifiOff as OfflineIcon,
  Refresh as RefreshIcon,
  NetworkCheck as NetworkIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material'
import { FallbackProps } from 'react-error-boundary'

export interface NetworkErrorFallbackProps extends FallbackProps {
  title?: string
  subtitle?: string
  showRetryButton?: boolean
  showNetworkStatus?: boolean
  maxRetries?: number
  retryDelay?: number
  onNetworkRestore?: () => void
  endpoint?: string
  requestMethod?: string
}

type NetworkStatus = 'checking' | 'online' | 'offline' | 'restored'
type RetryState = 'idle' | 'retrying' | 'failed' | 'success'

/**
 * Specialized Error Fallback for Network-Related Errors
 * Includes offline detection, retry with exponential backoff, and network status monitoring
 */
export const NetworkErrorFallback: React.FC<NetworkErrorFallbackProps> = ({
  error,
  resetErrorBoundary,
  title = 'Network Connection Error',
  subtitle = 'Unable to connect to the server. Please check your internet connection.',
  showRetryButton = true,
  showNetworkStatus = true,
  maxRetries = 3,
  retryDelay = 1000,
  onNetworkRestore,
  endpoint,
  requestMethod,
}) => {
  const theme = useTheme()
  
  const [networkStatus, setNetworkStatus] = useState<NetworkStatus>('checking')
  const [retryState, setRetryState] = useState<RetryState>('idle')
  const [retryCount, setRetryCount] = useState(0)
  const [retryProgress, setRetryProgress] = useState(0)
  const [timeUntilRetry, setTimeUntilRetry] = useState(0)

  // Check network status
  const checkNetworkStatus = useCallback(async () => {
    setNetworkStatus('checking')
    
    try {
      // Try to fetch a small resource to check connectivity
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 5000)
      
      await fetch('/favicon.ico', {
        method: 'HEAD',
        cache: 'no-cache',
        signal: controller.signal,
      })
      
      clearTimeout(timeoutId)
      setNetworkStatus('online')
      
      // If we were offline and now online, trigger restore callback
      if (networkStatus === 'offline') {
        setNetworkStatus('restored')
        onNetworkRestore?.()
      }
    } catch (networkError) {
      setNetworkStatus('offline')
    }
  }, [networkStatus, onNetworkRestore])

  // Retry with exponential backoff
  const handleRetry = useCallback(async () => {
    if (retryCount >= maxRetries) {
      setRetryState('failed')
      return
    }

    setRetryState('retrying')
    setRetryCount(prev => prev + 1)
    
    // Calculate delay with exponential backoff
    const delay = retryDelay * Math.pow(2, retryCount)
    setTimeUntilRetry(delay / 1000)
    
    // Show countdown
    const countdownInterval = setInterval(() => {
      setTimeUntilRetry(prev => {
        if (prev <= 1) {
          clearInterval(countdownInterval)
          return 0
        }
        return prev - 1
      })
    }, 1000)

    // Wait for delay period with progress indication
    let progress = 0
    const progressInterval = setInterval(() => {
      progress += 100 / (delay / 100)
      setRetryProgress(Math.min(progress, 100))
    }, 100)

    setTimeout(() => {
      clearInterval(progressInterval)
      setRetryProgress(0)
      
      // Check network status first
      checkNetworkStatus().then(() => {
        if (networkStatus === 'online' || networkStatus === 'restored') {
          setRetryState('success')
          resetErrorBoundary()
        } else {
          setRetryState('idle')
          // Auto-retry if not at max retries
          if (retryCount < maxRetries - 1) {
            setTimeout(() => handleRetry(), 1000)
          } else {
            setRetryState('failed')
          }
        }
      })
    }, delay)
  }, [retryCount, maxRetries, retryDelay, resetErrorBoundary, checkNetworkStatus, networkStatus])

  // Reset retry state
  const handleManualRetry = useCallback(() => {
    setRetryCount(0)
    setRetryState('idle')
    handleRetry()
  }, [handleRetry])

  // Listen for online/offline events
  useEffect(() => {
    const handleOnline = () => {
      setNetworkStatus('restored')
      checkNetworkStatus()
    }
    
    const handleOffline = () => {
      setNetworkStatus('offline')
    }

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    
    // Initial network check
    checkNetworkStatus()

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [checkNetworkStatus])

  const getNetworkStatusColor = (): 'success' | 'error' | 'info' | 'default' => {
    switch (networkStatus) {
      case 'online':
      case 'restored':
        return 'success'
      case 'offline':
        return 'error'
      case 'checking':
        return 'info'
      default:
        return 'default'
    }
  }

  const getNetworkStatusIcon = () => {
    switch (networkStatus) {
      case 'online':
      case 'restored':
        return <CheckCircleIcon />
      case 'offline':
        return <OfflineIcon />
      case 'checking':
        return <NetworkIcon />
      default:
        return <WarningIcon />
    }
  }

  const isRetrying = retryState === 'retrying'
  const hasReachedMaxRetries = retryCount >= maxRetries && retryState === 'failed'

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
        <CardContent sx={{ p: 3 }}>
          {/* Header */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <OfflineIcon
              color="error"
              sx={{
                fontSize: 32,
                mr: 2,
              }}
            />
            <Box sx={{ flex: 1 }}>
              <Typography variant="h6" color="text.primary" gutterBottom>
                {title}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {subtitle}
              </Typography>
            </Box>
          </Box>

          {/* Network Status */}
          {showNetworkStatus && (
            <Box sx={{ mb: 2 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <Chip
                  size="small"
                  icon={getNetworkStatusIcon()}
                  label={`Network: ${networkStatus.charAt(0).toUpperCase() + networkStatus.slice(1)}`}
                  color={getNetworkStatusColor()}
                  variant="outlined"
                />
                {endpoint && (
                  <Chip
                    size="small"
                    label={`${requestMethod || 'GET'} ${endpoint}`}
                    variant="outlined"
                  />
                )}
                {retryCount > 0 && (
                  <Chip
                    size="small"
                    label={`Retry ${retryCount}/${maxRetries}`}
                    color={hasReachedMaxRetries ? 'error' : 'primary'}
                    variant="outlined"
                  />
                )}
              </Stack>
            </Box>
          )}

          {/* Retry Progress */}
          {isRetrying && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Retrying in {Math.ceil(timeUntilRetry)} seconds...
              </Typography>
              <LinearProgress 
                variant="determinate" 
                value={retryProgress} 
                sx={{ height: 8, borderRadius: 4 }}
              />
            </Box>
          )}

          {/* Error Details */}
          <Alert 
            severity={networkStatus === 'offline' ? 'error' : 'warning'} 
            sx={{ mb: 2 }}
          >
            <Typography variant="body2">
              <strong>Error:</strong> {error.message}
            </Typography>
            {networkStatus === 'offline' && (
              <Typography variant="body2" sx={{ mt: 1 }}>
                Your device appears to be offline. Please check your internet connection.
              </Typography>
            )}
          </Alert>

          {/* Action Buttons */}
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {showRetryButton && !isRetrying && (
              <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={handleManualRetry}
                disabled={hasReachedMaxRetries}
                color="primary"
              >
                {hasReachedMaxRetries ? 'Max Retries Reached' : 'Retry Now'}
              </Button>
            )}
            
            {isRetrying && (
              <Button
                variant="outlined"
                startIcon={<CircularProgress size={16} />}
                disabled
              >
                Retrying...
              </Button>
            )}

            <Button
              variant="outlined"
              startIcon={<NetworkIcon />}
              onClick={checkNetworkStatus}
              disabled={networkStatus === 'checking'}
            >
              Check Connection
            </Button>
          </Stack>

          {/* Recovery Instructions */}
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Troubleshooting Steps:
            </Typography>
            <Typography variant="body2" color="text.secondary" component="ul" sx={{ pl: 2 }}>
              <li>Check your internet connection</li>
              <li>Try refreshing the page</li>
              <li>Disable VPN or proxy if enabled</li>
              <li>Check if the server is accessible from another device</li>
              {hasReachedMaxRetries && (
                <li>Contact support if the problem persists</li>
              )}
            </Typography>
          </Box>

          {/* Success State */}
          {networkStatus === 'restored' && (
            <Alert severity="success" sx={{ mt: 2 }}>
              <Typography variant="body2">
                Network connection restored! The application should work normally now.
              </Typography>
            </Alert>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}