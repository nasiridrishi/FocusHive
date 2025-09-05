import React, { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Stack,
  Alert,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
} from '@mui/material'
import {
  ExpandMore as ExpandMoreIcon,
  BugReport as BugReportIcon,
  Code as CodeIcon,
  Warning as WarningIcon,
} from '@mui/icons-material'
import { AppErrorBoundary, FeatureLevelErrorBoundary } from './AppErrorBoundary'
import { useAsyncError } from '@shared/hooks/useAsyncError'

interface CrashComponentProps {
  errorType: string
  errorMessage: string
  shouldCrash: boolean
}

/**
 * Component that can be made to crash on demand for testing error boundaries
 */
const CrashComponent: React.FC<CrashComponentProps> = ({
  errorType,
  errorMessage,
  shouldCrash,
}) => {
  const asyncError = useAsyncError()

  React.useEffect(() => {
    if (!shouldCrash) return

    switch (errorType) {
      case 'render':
        throw new Error(errorMessage || 'Render error')
      
      case 'async':
        setTimeout(() => {
          asyncError.captureError(
            new Error(errorMessage || 'Async error'),
            { severity: 'high', shouldThrow: true }
          )
        }, 100)
        break
      
      case 'promise':
        asyncError.wrapPromise(
          Promise.reject(new Error(errorMessage || 'Promise rejection')),
          { severity: 'high' }
        )
        break
      
      case 'api':
        asyncError.safeApiCall(
          async () => {
            throw new Error(errorMessage || 'API error')
          },
          { context: { endpoint: '/demo/crash' } }
        )
        break
    }
  }, [shouldCrash, errorType, errorMessage, asyncError])

  if (shouldCrash && errorType === 'render') {
    throw new Error(errorMessage || 'Render error')
  }

  return (
    <Box
      sx={{
        p: 2,
        border: '1px dashed',
        borderColor: 'success.main',
        borderRadius: 1,
        bgcolor: 'success.50',
      }}
    >
      <Typography variant="body2" color="success.dark">
        ✅ Component is working normally
      </Typography>
    </Box>
  )
}

/**
 * Demo component for showcasing error boundary functionality
 */
export const ErrorBoundaryDemo: React.FC = () => {
  const [errorType, setErrorType] = useState('render')
  const [errorMessage, setErrorMessage] = useState('Demo crash error')
  const [shouldCrash, setShouldCrash] = useState(false)
  const [selectedBoundary, setSelectedBoundary] = useState('feature')

  const handleCrash = () => {
    setShouldCrash(true)
    setTimeout(() => setShouldCrash(false), 1000) // Reset after error
  }

  const handleReset = () => {
    setShouldCrash(false)
  }

  const getBoundaryComponent = () => {
    const props = {
      children: (
        <CrashComponent
          errorType={errorType}
          errorMessage={errorMessage}
          shouldCrash={shouldCrash}
        />
      ),
    }

    switch (selectedBoundary) {
      case 'app':
        return <AppErrorBoundary level="app" {...props} />
      case 'route':
        return <AppErrorBoundary level="route" {...props} />
      case 'feature':
        return (
          <FeatureLevelErrorBoundary featureName="Demo">
            {props.children}
          </FeatureLevelErrorBoundary>
        )
      case 'component':
        return <AppErrorBoundary level="component" {...props} />
      default:
        return props.children
    }
  }

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        Error Boundary Demo
      </Typography>
      
      <Alert severity="info" sx={{ mb: 3 }}>
        <Typography variant="body2">
          This demo showcases how error boundaries handle different types of component crashes
          and provide graceful fallback UI with recovery options.
        </Typography>
      </Alert>

      {/* Controls */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            <CodeIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
            Error Simulation Controls
          </Typography>

          <Stack spacing={2}>
            <Stack direction="row" spacing={2} alignItems="center">
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Boundary Type</InputLabel>
                <Select
                  value={selectedBoundary}
                  label="Boundary Type"
                  onChange={(e) => setSelectedBoundary(e.target.value)}
                >
                  <MenuItem value="app">App Level</MenuItem>
                  <MenuItem value="route">Route Level</MenuItem>
                  <MenuItem value="feature">Feature Level</MenuItem>
                  <MenuItem value="component">Component Level</MenuItem>
                </Select>
              </FormControl>

              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Error Type</InputLabel>
                <Select
                  value={errorType}
                  label="Error Type"
                  onChange={(e) => setErrorType(e.target.value)}
                >
                  <MenuItem value="render">Render Error</MenuItem>
                  <MenuItem value="async">Async Error</MenuItem>
                  <MenuItem value="promise">Promise Rejection</MenuItem>
                  <MenuItem value="api">API Error</MenuItem>
                </Select>
              </FormControl>

              <TextField
                size="small"
                label="Error Message"
                value={errorMessage}
                onChange={(e) => setErrorMessage(e.target.value)}
                sx={{ minWidth: 200 }}
              />
            </Stack>

            <Stack direction="row" spacing={2}>
              <Button
                variant="contained"
                color="error"
                startIcon={<BugReportIcon />}
                onClick={handleCrash}
                disabled={shouldCrash}
              >
                Trigger Error
              </Button>
              
              <Button
                variant="outlined"
                onClick={handleReset}
              >
                Reset Component
              </Button>

              <Chip
                label={`${selectedBoundary} boundary`}
                color="primary"
                variant="outlined"
              />
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {/* Error Type Descriptions */}
      <Accordion sx={{ mb: 3 }}>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">
            <WarningIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
            Error Types Explained
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Stack spacing={2}>
            <Alert severity="error">
              <Typography variant="subtitle2">Render Error</Typography>
              <Typography variant="body2">
                Synchronous error thrown during component render phase. 
                This will be caught by React Error Boundaries.
              </Typography>
            </Alert>
            
            <Alert severity="warning">
              <Typography variant="subtitle2">Async Error</Typography>
              <Typography variant="body2">
                Asynchronous error (setTimeout, event handlers). These are NOT caught 
                by React Error Boundaries and need special handling.
              </Typography>
            </Alert>
            
            <Alert severity="warning">
              <Typography variant="subtitle2">Promise Rejection</Typography>
              <Typography variant="body2">
                Unhandled promise rejection. Also not caught by Error Boundaries 
                but can be forwarded using useAsyncError hook.
              </Typography>
            </Alert>
            
            <Alert severity="info">
              <Typography variant="subtitle2">API Error</Typography>
              <Typography variant="body2">
                Network/API call failure. Demonstrates safe API call wrapper 
                that logs errors without crashing the UI.
              </Typography>
            </Alert>
          </Stack>
        </AccordionDetails>
      </Accordion>

      {/* Demo Component with Error Boundary */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Demo Component with Error Boundary
          </Typography>
          
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            The component below is wrapped with a <code>{selectedBoundary}-level</code> error boundary.
            When an error occurs, it will show a fallback UI with recovery options.
          </Typography>

          <Box sx={{ minHeight: 150, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
            {getBoundaryComponent()}
          </Box>
        </CardContent>
      </Card>

      {/* Additional Features */}
      <Box sx={{ mt: 3 }}>
        <Typography variant="h6" gutterBottom>
          Error Boundary Features
        </Typography>
        
        <Stack spacing={1}>
          <Chip label="✅ Automatic Error Logging" size="small" />
          <Chip label="✅ Development vs Production UI" size="small" />
          <Chip label="✅ Error Recovery (Try Again)" size="small" />
          <Chip label="✅ Navigation Fallbacks (Go Home)" size="small" />
          <Chip label="✅ Error Reporting" size="small" />
          <Chip label="✅ Hierarchical Error Handling" size="small" />
          <Chip label="✅ Async Error Support" size="small" />
          <Chip label="✅ Context Preservation" size="small" />
        </Stack>
      </Box>
    </Box>
  )
}

export default ErrorBoundaryDemo