import React from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
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
  BugReport as BugReportIcon,
  Error as ErrorIcon,
  ExpandMore as ExpandMoreIcon,
  Home as HomeIcon,
  Refresh as RefreshIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material'
import {FallbackProps} from 'react-error-boundary'

export interface ErrorFallbackProps extends FallbackProps {
  title?: string
  subtitle?: string
  showErrorDetails?: boolean
  showResetButton?: boolean
  showHomeButton?: boolean
  showReportButton?: boolean
  onGoHome?: () => void
  onReportError?: (error: Error) => void
  severity?: 'error' | 'warning' | 'info'
  errorBoundaryName?: string
}

/**
 * Enhanced Error Fallback Component with Material UI
 * Provides comprehensive error display with recovery options
 */
export const ErrorFallback: React.FC<ErrorFallbackProps> = ({
                                                              error,
                                                              resetErrorBoundary,
                                                              title = 'Something went wrong',
                                                              subtitle = 'An unexpected error occurred while loading this section.',
                                                              showErrorDetails = import.meta.env.DEV,
                                                              showResetButton = true,
                                                              showHomeButton = true,
                                                              showReportButton = true,
                                                              onGoHome,
                                                              onReportError,
                                                              severity = 'error',
                                                              errorBoundaryName,
                                                            }) => {
  const theme = useTheme()
  const isDevelopment = import.meta.env.DEV

  const handleReportError = React.useCallback(() => {
    if (onReportError) {
      onReportError(error)
    } else {
      // Default error reporting (could send to analytics)
      // console.log('Error reported:', error)
    }
  }, [error, onReportError])

  const handleGoHome = React.useCallback(() => {
    if (onGoHome) {
      onGoHome()
    } else {
      // Default: navigate to home
      window.location.href = '/'
    }
  }, [onGoHome])

  const errorTimestamp = new Date().toLocaleString()

  return (
      <Box
          role="alert"
          sx={{
            minHeight: '200px',
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
              <ErrorIcon
                  color={severity}
                  sx={{
                    fontSize: 32,
                    mr: 2,
                    color: severity === 'error' ? 'error.main' : 'warning.main',
                  }}
              />
              <Box sx={{flex: 1}}>
                <Typography variant="h6" color="text.primary" gutterBottom>
                  {title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {subtitle}
                </Typography>
              </Box>
            </Box>

            {/* Metadata */}
            {(errorBoundaryName || isDevelopment) && (
                <Box sx={{mb: 2}}>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    {errorBoundaryName && (
                        <Chip
                            size="small"
                            label={`Boundary: ${errorBoundaryName}`}
                            color="primary"
                            variant="outlined"
                        />
                    )}
                    {isDevelopment && (
                        <Chip
                            size="small"
                            label="Development Mode"
                            color="info"
                            variant="outlined"
                        />
                    )}
                    <Chip
                        size="small"
                        icon={<ScheduleIcon/>}
                        label={errorTimestamp}
                        variant="outlined"
                    />
                  </Stack>
                </Box>
            )}

            {/* Action Buttons */}
            <Box sx={{mb: 3}}>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                {showResetButton && (
                    <Button
                        variant="contained"
                        startIcon={<RefreshIcon/>}
                        onClick={resetErrorBoundary}
                        color="primary"
                    >
                      Try Again
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
                {showReportButton && (
                    <Button
                        variant="outlined"
                        startIcon={<BugReportIcon/>}
                        onClick={handleReportError}
                        color="secondary"
                    >
                      Report Issue
                    </Button>
                )}
              </Stack>
            </Box>

            <Divider sx={{mb: 2}}/>

            {/* Error Details (Development Only or when explicitly enabled) */}
            {showErrorDetails && (
                <Accordion>
                  <AccordionSummary
                      expandIcon={<ExpandMoreIcon/>}
                      aria-controls="error-details-content"
                      id="error-details-header"
                  >
                    <Typography variant="subtitle2">
                      Error Details {isDevelopment && '(Development Only)'}
                    </Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Alert severity="error" sx={{mb: 2}}>
                      <Typography variant="body2" component="div">
                        <strong>Error Message:</strong>
                        <br/>
                        {error.message}
                      </Typography>
                    </Alert>

                    {error.stack && (
                        <Box
                            sx={{
                              backgroundColor: 'grey.100',
                              border: 1,
                              borderColor: 'grey.300',
                              borderRadius: 1,
                              p: 2,
                              mt: 2,
                            }}
                        >
                          <Typography variant="subtitle2" gutterBottom>
                            Stack Trace:
                          </Typography>
                          <Typography
                              variant="caption"
                              component="pre"
                              sx={{
                                fontSize: '0.75rem',
                                fontFamily: 'monospace',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-word',
                                maxHeight: 200,
                                overflow: 'auto',
                                color: 'text.secondary',
                              }}
                          >
                            {error.stack}
                          </Typography>
                        </Box>
                    )}
                  </AccordionDetails>
                </Accordion>
            )}

            {/* User-friendly message */}
            {!showErrorDetails && (
                <Alert severity="info" sx={{mt: 2}}>
                  <Typography variant="body2">
                    If this problem persists, please try refreshing the page or contact support.
                  </Typography>
                </Alert>
            )}
          </CardContent>
        </Card>
      </Box>
  )
}