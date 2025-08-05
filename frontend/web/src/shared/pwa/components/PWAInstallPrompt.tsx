import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  Box,
  Typography,
  IconButton,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  GetApp as InstallIcon,
  Close as CloseIcon,
  PhoneAndroid as MobileIcon,
  Computer as DesktopIcon,
} from '@mui/icons-material';
import { usePWAInstall } from '../hooks/usePWAInstall';

export interface PWAInstallPromptProps {
  /**
   * Whether to show the install prompt dialog
   */
  open?: boolean;
  /**
   * Callback when the dialog should be closed
   */
  onClose?: () => void;
  /**
   * Custom title for the install prompt
   */
  title?: string;
  /**
   * Custom description for the install prompt
   */
  description?: string;
}

/**
 * PWA Install Prompt Component
 * 
 * Displays a Material UI dialog prompting users to install the PWA.
 * Integrates with the usePWAInstall hook to handle installation logic.
 * 
 * Features:
 * - Responsive design adapting to mobile/desktop
 * - Material Design following patterns
 * - Progressive enhancement (gracefully handles unsupported browsers)
 * - Accessible with proper ARIA labels
 */
export const PWAInstallPrompt: React.FC<PWAInstallPromptProps> = ({
  open: externalOpen,
  onClose: externalOnClose,
  title = 'Install FocusHive',
  description = 'Install FocusHive on your device for a better experience with offline access and native-like performance.',
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  
  const {
    isInstallable,
    isInstalling,
    isStandalone,
    promptInstall,
    cancelInstall,
  } = usePWAInstall();

  // Use external open state if provided, otherwise use internal state
  const isOpen = externalOpen !== undefined ? externalOpen : isInstallable;
  
  const handleClose = () => {
    if (externalOnClose) {
      externalOnClose();
    } else {
      cancelInstall();
    }
  };

  const handleInstall = async () => {
    try {
      await promptInstall();
      handleClose();
    } catch (error) {
      console.error('Installation failed:', error);
    }
  };

  // Don't show if already installed or not installable
  if (isStandalone || (!isInstallable && externalOpen === undefined)) {
    return null;
  }

  return (
    <Dialog
      open={isOpen}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      fullScreen={isMobile}
      aria-labelledby="pwa-install-dialog-title"
      aria-describedby="pwa-install-dialog-description"
      PaperProps={{
        sx: {
          borderRadius: isMobile ? 0 : 2,
          m: isMobile ? 0 : 2,
        },
      }}
    >
      <DialogTitle
        id="pwa-install-dialog-title"
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          pb: 1,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {isMobile ? <MobileIcon color="primary" /> : <DesktopIcon color="primary" />}
          <Typography variant="h6" component="span">
            {title}
          </Typography>
        </Box>
        <IconButton
          aria-label="close"
          onClick={handleClose}
          size="small"
          sx={{ color: 'text.secondary' }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <DialogContent sx={{ pb: 2 }}>
        <DialogContentText
          id="pwa-install-dialog-description"
          sx={{ mb: 2 }}
        >
          {description}
        </DialogContentText>

        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
            p: 2,
            bgcolor: 'background.default',
            borderRadius: 1,
          }}
        >
          <Typography variant="subtitle2" color="text.secondary">
            Benefits of installing:
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, ml: 1 }}>
            <Typography variant="body2" color="text.secondary">
              • Faster loading and better performance
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Works offline when internet is unavailable
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Native app-like experience
            </Typography>
            <Typography variant="body2" color="text.secondary">
              • Quick access from your home screen or desktop
            </Typography>
          </Box>
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 3, gap: 1 }}>
        <Button
          onClick={handleClose}
          color="inherit"
          sx={{ minWidth: 100 }}
        >
          Maybe Later
        </Button>
        <Button
          onClick={handleInstall}
          variant="contained"
          startIcon={<InstallIcon />}
          disabled={isInstalling}
          sx={{ minWidth: 120 }}
        >
          {isInstalling ? 'Installing...' : 'Install App'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};