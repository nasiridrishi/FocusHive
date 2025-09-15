import React, { Component, ReactNode, ErrorInfo } from 'react'
import {
  Box,
  Button,
  Container,
  Typography,
  Paper,
  Collapse,
  Alert,
  type Breakpoint,
} from '@mui/material'
import { Refresh, ErrorOutline, ExpandMore } from '@mui/icons-material'

interface ErrorBoundaryProps {
  children: ReactNode
  fallback?: ReactNode
  onError?: (error: Error, errorInfo: ErrorInfo) => void
  onReset?: () => void
  showDetails?: boolean
  showStackTrace?: boolean
  logError?: (error: Error, errorInfo: ErrorInfo) => void
}

interface ErrorBoundaryState {
  hasError: boolean
  error: Error | null
  errorInfo: ErrorInfo | null
  showDetails: boolean
}

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false,
    }
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    // Update state so the next render will show the fallback UI
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Log error to console in development
    if (process.env.NODE_ENV === 'development') {
      console.error('ErrorBoundary caught an error:', error, errorInfo)
    }

    // Call onError callback if provided
    if (this.props.onError) {
      this.props.onError(error, errorInfo)
    }

    // Call logging service if provided
    if (this.props.logError) {
      this.props.logError(error, errorInfo)
    }

    // Update state with error info
    this.setState({
      errorInfo,
    })
  }

  handleReset = (): void => {
    // Call onReset callback if provided
    if (this.props.onReset) {
      this.props.onReset()
    }

    // Reset error state
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false,
    })
  }

  toggleDetails = (): void => {
    this.setState((prevState) => ({
      showDetails: !prevState.showDetails,
    }))
  }

  componentDidMount(): void {
    // Focus reset button when error boundary mounts with error
    if (this.state.hasError) {
      const resetButton = document.querySelector('[data-testid="reset-error-button"]') as HTMLElement
      if (resetButton) {
        resetButton.focus()
      }
    }
  }

  componentDidUpdate(prevProps: ErrorBoundaryProps, prevState: ErrorBoundaryState): void {
    // Focus reset button when error occurs
    if (!prevState.hasError && this.state.hasError) {
      setTimeout(() => {
        const resetButton = document.querySelector('[data-testid="reset-error-button"]') as HTMLElement
        if (resetButton) {
          resetButton.focus()
        }
      }, 100)
    }
  }

  render(): ReactNode {
    if (this.state.hasError) {
      // If custom fallback is provided, use it
      if (this.props.fallback) {
        return this.props.fallback
      }

      const { error, errorInfo } = this.state
      const showDetails = this.props.showDetails || process.env.NODE_ENV === 'development'
      const showStackTrace = this.props.showStackTrace && showDetails

      // Default fallback UI
      return (
        <Container maxWidth={'md' as Breakpoint}>
          <Box
            data-testid="error-boundary-fallback"
            role="alert"
            aria-live="assertive"
            sx={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '400px',
              py: 4,
            }}
          >
            <Paper
              elevation={3}
              sx={{
                p: 4,
                width: '100%',
                textAlign: 'center',
                borderRadius: 2,
              }}
            >
              <ErrorOutline
                sx={{
                  fontSize: 64,
                  color: 'error.main',
                  mb: 2,
                }}
              />

              <Typography variant="h4" gutterBottom>
                Something went wrong
              </Typography>

              <Typography variant="body1" color="text.secondary" paragraph>
                We're sorry for the inconvenience. The application encountered an unexpected error.
              </Typography>

              {showDetails && error && (
                <Box data-testid="error-details" sx={{ mt: 3, mb: 3 }}>
                  <Alert severity="error" sx={{ textAlign: 'left', mb: 2 }}>
                    <Typography variant="subtitle2" fontWeight="bold">
                      Error Message:
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                      {error.toString()}
                    </Typography>
                  </Alert>

                  {showStackTrace && errorInfo && (
                    <Box sx={{ mt: 2 }}>
                      <Button
                        startIcon={<ExpandMore />}
                        onClick={this.toggleDetails}
                        variant="text"
                        size="small"
                      >
                        {this.state.showDetails ? 'Hide' : 'Show'} Stack Trace
                      </Button>

                      <Collapse in={this.state.showDetails}>
                        <Paper
                          data-testid="error-stack-trace"
                          variant="outlined"
                          sx={{
                            p: 2,
                            mt: 2,
                            bgcolor: 'grey.50',
                            maxHeight: 300,
                            overflow: 'auto',
                          }}
                        >
                          <Typography
                            variant="caption"
                            component="pre"
                            sx={{
                              fontFamily: 'monospace',
                              whiteSpace: 'pre-wrap',
                              wordBreak: 'break-all',
                            }}
                          >
                            {error.stack}
                            {'\n\nComponent Stack:'}
                            {errorInfo.componentStack}
                          </Typography>
                        </Paper>
                      </Collapse>
                    </Box>
                  )}
                </Box>
              )}

              <Button
                data-testid="reset-error-button"
                variant="contained"
                color="primary"
                startIcon={<Refresh />}
                onClick={this.handleReset}
                sx={{ mt: 2 }}
              >
                Try again
              </Button>
            </Paper>
          </Box>
        </Container>
      )
    }

    // No error, render children normally
    return this.props.children
  }
}

export default ErrorBoundary