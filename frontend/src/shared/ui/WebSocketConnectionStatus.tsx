import React from 'react';
import {
  Chip,
  Tooltip,
  Button,
  Box,
  Typography,
  CircularProgress
} from '@mui/material';
import {
  Wifi as ConnectedIcon,
  WifiOff as DisconnectedIcon,
  Sync as ReconnectingIcon,
  Error as ErrorIcon
} from '@mui/icons-material';
import type { WebSocketConnectionInfo } from '../../hooks/useWebSocket';

interface WebSocketConnectionStatusProps {
  connectionInfo: WebSocketConnectionInfo;
  onRetry?: () => void;
  showDetails?: boolean;
  variant?: 'chip' | 'detailed';
}

export const WebSocketConnectionStatus: React.FC<WebSocketConnectionStatusProps> = ({
  connectionInfo,
  onRetry,
  showDetails = false,
  variant = 'chip'
}) => {
  const { connectionState, reconnectionInfo } = connectionInfo;

  const getStatusColor = () => {
    switch (connectionState) {
      case 'CONNECTED':
        return 'success';
      case 'RECONNECTING':
        return 'warning';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusIcon = () => {
    switch (connectionState) {
      case 'CONNECTED':
        return <ConnectedIcon />;
      case 'RECONNECTING':
        return <ReconnectingIcon className="animate-spin" />;
      case 'FAILED':
        return <ErrorIcon />;
      default:
        return <DisconnectedIcon />;
    }
  };

  const getStatusText = () => {
    switch (connectionState) {
      case 'CONNECTED':
        return 'Connected';
      case 'RECONNECTING':
        return `Reconnecting... (${reconnectionInfo.attempts}/${reconnectionInfo.maxAttempts})`;
      case 'FAILED':
        return 'Connection Failed';
      default:
        return 'Disconnected';
    }
  };

  const getTooltipText = () => {
    if (connectionState === 'CONNECTED') {
      return 'Real-time connection is active';
    }
    if (connectionState === 'RECONNECTING') {
      return `Attempting to reconnect (${reconnectionInfo.attempts}/${reconnectionInfo.maxAttempts})`;
    }
    if (connectionState === 'FAILED') {
      return 'Connection failed after maximum retry attempts. Click to retry.';
    }
    return 'Real-time connection is not active';
  };

  if (variant === 'detailed') {
    return (
      <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
          {getStatusIcon()}
          <Typography variant="subtitle2" component="span">
            WebSocket Status: {getStatusText()}
          </Typography>
        </Box>
        
        {showDetails && (
          <Box sx={{ ml: 4, mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Connection State: {connectionState}
            </Typography>
            {reconnectionInfo.isReconnecting && (
              <Typography variant="body2" color="text.secondary">
                Reconnection Attempts: {reconnectionInfo.attempts}/{reconnectionInfo.maxAttempts}
              </Typography>
            )}
          </Box>
        )}

        {connectionState === 'FAILED' && onRetry && (
          <Button 
            variant="outlined" 
            size="small" 
            onClick={onRetry}
            startIcon={<ReconnectingIcon />}
          >
            Retry Connection
          </Button>
        )}

        {connectionState === 'RECONNECTING' && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 4 }}>
            <CircularProgress size={16} />
            <Typography variant="body2" color="text.secondary">
              Reconnecting...
            </Typography>
          </Box>
        )}
      </Box>
    );
  }

  // Chip variant (default)
  return (
    <Tooltip title={getTooltipText()} arrow>
      <Chip
        icon={getStatusIcon()}
        label={getStatusText()}
        color={getStatusColor()}
        size="small"
        clickable={connectionState === 'FAILED' && !!onRetry}
        onClick={connectionState === 'FAILED' ? onRetry : undefined}
        sx={{
          '& .MuiChip-icon': {
            animation: connectionState === 'RECONNECTING' ? 'spin 1s linear infinite' : 'none',
          },
          '@keyframes spin': {
            '0%': { transform: 'rotate(0deg)' },
            '100%': { transform: 'rotate(360deg)' },
          }
        }}
      />
    </Tooltip>
  );
};

export default WebSocketConnectionStatus;