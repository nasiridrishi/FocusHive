/**
 * Environment Error Page Component
 * 
 * Displays user-friendly error messages when environment variable validation fails.
 * Provides guidance for developers on how to fix configuration issues.
 */

import React from 'react';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import {
  Error as ErrorIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  ContentCopy as CopyIcon,
  Refresh as RefreshIcon,
  Settings as SettingsIcon
} from '@mui/icons-material';
import type { EnvValidationError } from '../services/validation/envValidation';

interface EnvironmentErrorProps {
  errors: EnvValidationError[];
  onRetry?: () => void;
}

/**
 * Copies text to clipboard and shows feedback
 */
const copyToClipboard = async (text: string): Promise<void> => {
  try {
    await navigator.clipboard.writeText(text);
    // Could show a snackbar here in a real implementation
    console.log('Copied to clipboard');
  } catch (err) {
    console.error('Failed to copy to clipboard:', err);
  }
};

/**
 * Environment Error Page Component
 */
export const EnvironmentError: React.FC<EnvironmentErrorProps> = ({
  errors,
  onRetry
}) => {
  const theme = useTheme();
  
  const criticalErrors = errors.filter(error => error.severity === 'error');
  const warnings = errors.filter(error => error.severity === 'warning');

  // Generate example .env content based on errors
  const generateExampleEnv = (): string => {
    const envLines: string[] = [
      '# FocusHive Environment Configuration',
      '',
      '# Required: Backend API Configuration',
      'VITE_API_BASE_URL=http://localhost:8080',
      '',
      '# Required: WebSocket Configuration',
      'VITE_WEBSOCKET_URL=ws://localhost:8080',
      '',
      '# Optional: WebSocket Settings (with defaults)',
      'VITE_WEBSOCKET_RECONNECT_ATTEMPTS=10',
      'VITE_WEBSOCKET_RECONNECT_DELAY=1000',
      'VITE_WEBSOCKET_HEARTBEAT_INTERVAL=30000',
      '',
      '# Optional: Music Service Configuration',
      'VITE_MUSIC_SERVICE_URL=http://localhost:8084',
      'VITE_MUSIC_API_BASE_URL=http://localhost:8080',
      '',
      '# Optional: Spotify Integration',
      'VITE_SPOTIFY_CLIENT_ID=your_spotify_client_id_here',
      'VITE_SPOTIFY_REDIRECT_URI=http://localhost:3000/music/spotify/callback',
      '',
      '# Optional: Error Logging',
      '# VITE_ERROR_LOGGING_ENDPOINT=https://your-logging-service.com/api/errors',
      '# VITE_ERROR_LOGGING_API_KEY=your_api_key_here'
    ];

    return envLines.join('\n');
  };

  const exampleEnvContent = generateExampleEnv();

  return (
    <Container maxWidth={false} sx={{ maxWidth: 'md', py: 4 }}>
      <Stack spacing={4}>
        {/* Header */}
        <Box textAlign="center">
          <ErrorIcon 
            color="error" 
            sx={{ fontSize: 64, mb: 2 }} 
          />
          <Typography variant="h3" component="h1" gutterBottom>
            Configuration Error
          </Typography>
          <Typography variant="h6" color="text.secondary">
            FocusHive requires additional configuration to start
          </Typography>
        </Box>

        {/* Critical Errors */}
        {criticalErrors.length > 0 && (
          <Card>
            <CardContent>
              <Typography variant="h6" color="error" gutterBottom>
                <ErrorIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                Critical Issues ({criticalErrors.length})
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                These issues must be resolved before the application can start:
              </Typography>
              <List dense>
                {criticalErrors.map((error, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <ErrorIcon color="error" fontSize="small" />
                    </ListItemIcon>
                    <ListItemText
                      primary={error.variable}
                      secondary={error.message}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        )}

        {/* Warnings */}
        {warnings.length > 0 && (
          <Card>
            <CardContent>
              <Typography variant="h6" color="warning.main" gutterBottom>
                <WarningIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
                Warnings ({warnings.length})
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                These issues should be addressed for optimal performance:
              </Typography>
              <List dense>
                {warnings.map((error, index) => (
                  <ListItem key={index}>
                    <ListItemIcon>
                      <WarningIcon color="warning" fontSize="small" />
                    </ListItemIcon>
                    <ListItemText
                      primary={error.variable}
                      secondary={error.message}
                    />
                  </ListItem>
                ))}
              </List>
            </CardContent>
          </Card>
        )}

        {/* Setup Instructions */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              <SettingsIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
              Setup Instructions
            </Typography>
            
            <Stack spacing={3}>
              <Alert severity="info">
                <AlertTitle>Quick Fix</AlertTitle>
                Copy the example configuration below to a <code>.env</code> file in your project root and update the values as needed.
              </Alert>

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  1. Create or update your <code>.env</code> file:
                </Typography>
                <Paper 
                  variant="outlined" 
                  sx={{ 
                    p: 2, 
                    backgroundColor: theme.palette.mode === 'dark' ? 'grey.900' : 'grey.50',
                    position: 'relative'
                  }}
                >
                  <Button
                    size="small"
                    startIcon={<CopyIcon />}
                    onClick={() => copyToClipboard(exampleEnvContent)}
                    sx={{ 
                      position: 'absolute', 
                      top: 8, 
                      right: 8, 
                      minWidth: 'auto' 
                    }}
                  >
                    Copy
                  </Button>
                  <Typography
                    component="pre"
                    variant="body2"
                    sx={{
                      fontFamily: 'monospace',
                      fontSize: '0.875rem',
                      lineHeight: 1.6,
                      margin: 0,
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-all',
                      pr: 8 // Make room for copy button
                    }}
                  >
                    {exampleEnvContent}
                  </Typography>
                </Paper>
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  2. Update the configuration values:
                </Typography>
                <List dense>
                  <ListItem>
                    <ListItemIcon>
                      <InfoIcon color="info" />
                    </ListItemIcon>
                    <ListItemText 
                      primary="VITE_API_BASE_URL"
                      secondary="Set to your FocusHive backend server URL (e.g., http://localhost:8080)"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemIcon>
                      <InfoIcon color="info" />
                    </ListItemIcon>
                    <ListItemText 
                      primary="VITE_WEBSOCKET_URL"
                      secondary="Set to your WebSocket server URL (e.g., ws://localhost:8080)"
                    />
                  </ListItem>
                  <ListItem>
                    <ListItemIcon>
                      <InfoIcon color="info" />
                    </ListItemIcon>
                    <ListItemText 
                      primary="VITE_SPOTIFY_CLIENT_ID"
                      secondary="Get from https://developer.spotify.com/dashboard (optional for music features)"
                    />
                  </ListItem>
                </List>
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  3. Restart the development server:
                </Typography>
                <Paper 
                  variant="outlined" 
                  sx={{ 
                    p: 2, 
                    backgroundColor: theme.palette.mode === 'dark' ? 'grey.900' : 'grey.50' 
                  }}
                >
                  <Typography
                    component="pre"
                    variant="body2"
                    sx={{
                      fontFamily: 'monospace',
                      margin: 0
                    }}
                  >
                    npm run dev
                  </Typography>
                </Paper>
              </Box>
            </Stack>
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <Box textAlign="center">
          <Stack direction="row" spacing={2} justifyContent="center">
            {onRetry && (
              <Button
                variant="contained"
                startIcon={<RefreshIcon />}
                onClick={onRetry}
              >
                Retry Configuration
              </Button>
            )}
            <Button
              variant="outlined"
              onClick={() => window.location.reload()}
            >
              Reload Page
            </Button>
          </Stack>
        </Box>

        {/* Environment Details */}
        {import.meta.env.DEV && (
          <Card sx={{ backgroundColor: 'action.hover' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Environment Details (Development Only)
              </Typography>
              <Typography variant="body2" component="div">
                <strong>Mode:</strong> {import.meta.env.MODE}<br />
                <strong>Development:</strong> {import.meta.env.DEV ? 'Yes' : 'No'}<br />
                <strong>Production:</strong> {import.meta.env.PROD ? 'Yes' : 'No'}<br />
                <strong>Base URL:</strong> {import.meta.env.BASE_URL}
              </Typography>
            </CardContent>
          </Card>
        )}
      </Stack>
    </Container>
  );
};

export default EnvironmentError;