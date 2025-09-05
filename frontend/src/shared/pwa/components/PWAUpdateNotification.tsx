import React, { useState } from 'react';
import {
  Snackbar,
  Alert,
  AlertTitle,
  Button,
  Box,
  Slide,
  IconButton,
  Typography,
  LinearProgress,
} from '@mui/material';
import {
  Update as UpdateIcon,
  Close as CloseIcon,
  CloudDownload as DownloadIcon,
  CheckCircle as ReadyIcon,
} from '@mui/icons-material';
import { usePWA } from './PWAProvider';
import { TransitionProps } from '@mui/material/transitions';

export interface PWAUpdateNotificationProps {
  /**
   * Position of the snackbar
   */
  anchorOrigin?: {
    vertical: 'top' | 'bottom';
    horizontal: 'left' | 'center' | 'right';
  };
  /**
   * Auto hide duration in milliseconds (0 means no auto hide)
   */
  autoHideDuration?: number;
  /**
   * Custom messages for different states
   */
  messages?: {
    updateAvailable?: string;
    offlineReady?: string;
    updating?: string;
  };
}

// Slide transition component
const SlideTransition = React.forwardRef<unknown, TransitionProps & { children: React.ReactElement }>((props, ref) => {
  return <Slide {...props} direction="up" ref={ref} />;
});

/**
 * PWA Update Notification Component
 * 
 * Displays Material UI snackbar notifications for PWA updates and offline readiness.
 * Uses the PWA context to access service worker state and avoid duplicate registrations.
 * 
 * Features:
 * - Shows update available notifications
 * - Shows offline ready notifications  
 * - Handles service worker updates with loading states
 * - Material Design with consistent theming
 * - Accessible with proper ARIA labels
 * - Configurable positioning and timing
 */
export const PWAUpdateNotification: React.FC<PWAUpdateNotificationProps> = ({
  anchorOrigin = { vertical: 'bottom', horizontal: 'left' },
  autoHideDuration = 0, // Don't auto-hide by default
  messages = {},
}) => {
  const [isUpdating, setIsUpdating] = useState(false);
  const [dismissed, setDismissed] = useState({
    needsRefresh: false,
    offlineReady: false,
  });

  // Use PWA context instead of creating a duplicate service worker registration
  const { serviceWorker } = usePWA();
  const {
    needsRefresh,
    offlineReady,
    updateServiceWorker,
  } = serviceWorker;

  const defaultMessages = {
    updateAvailable: 'A new version of FocusHive is available!',
    offlineReady: 'FocusHive is ready to work offline.',
    updating: 'Updating FocusHive...',
  };

  const finalMessages = { ...defaultMessages, ...messages };

  const handleUpdate = async () => {
    setIsUpdating(true);
    try {
      await updateServiceWorker(true); // Reload page after update
    } catch (error) {
      setIsUpdating(false);
    }
  };

  const handleDismiss = (type: 'needsRefresh' | 'offlineReady') => {
    setDismissed(prev => ({ ...prev, [type]: true }));
  };

  // Show update available notification
  const showUpdateNotification = needsRefresh && !dismissed.needsRefresh;
  // Show offline ready notification
  const showOfflineNotification = offlineReady && !dismissed.offlineReady && !needsRefresh;

  return (
    <>
      {/* Update Available Notification */}
      <Snackbar
        open={showUpdateNotification}
        anchorOrigin={anchorOrigin}
        autoHideDuration={autoHideDuration || undefined}
        onClose={() => handleDismiss('needsRefresh')}
        TransitionComponent={SlideTransition}
        sx={{ mb: showOfflineNotification ? 8 : 0 }}
      >
        <Alert
          severity="info"
          variant="filled"
          icon={<UpdateIcon />}
          sx={{
            width: '100%',
            alignItems: 'flex-start',
          }}
          action={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Button
                color="inherit"
                size="small"
                onClick={handleUpdate}
                disabled={isUpdating}
                startIcon={isUpdating ? <DownloadIcon /> : <UpdateIcon />}
                sx={{ whiteSpace: 'nowrap' }}
              >
                {isUpdating ? 'Updating...' : 'Update Now'}
              </Button>
              <IconButton
                size="small"
                aria-label="close"
                color="inherit"
                onClick={() => handleDismiss('needsRefresh')}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          }
        >
          <AlertTitle sx={{ mb: 0.5 }}>Update Available</AlertTitle>
          <Typography variant="body2">
            {finalMessages.updateAvailable}
          </Typography>
          {isUpdating && (
            <Box sx={{ mt: 1, width: '100%' }}>
              <LinearProgress />
              <Typography variant="caption" color="inherit" sx={{ mt: 0.5 }}>
                {finalMessages.updating}
              </Typography>
            </Box>
          )}
        </Alert>
      </Snackbar>

      {/* Offline Ready Notification */}
      <Snackbar
        open={showOfflineNotification}
        anchorOrigin={anchorOrigin}
        autoHideDuration={autoHideDuration || 6000} // Auto-hide offline ready after 6 seconds
        onClose={() => handleDismiss('offlineReady')}
        TransitionComponent={SlideTransition}
      >
        <Alert
          severity="success"
          variant="filled"
          icon={<ReadyIcon />}
          sx={{ width: '100%' }}
          action={
            <IconButton
              size="small"
              aria-label="close"
              color="inherit"
              onClick={() => handleDismiss('offlineReady')}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          }
        >
          <AlertTitle sx={{ mb: 0.5 }}>App Ready</AlertTitle>
          <Typography variant="body2">
            {finalMessages.offlineReady}
          </Typography>
        </Alert>
      </Snackbar>
    </>
  );
};