import React from 'react';
import {
  Backdrop,
  Box,
  Typography,
  Fade,
  Portal,
  styled,
  useTheme,
  LinearProgress,
} from '@mui/material';
import { LoadingSpinner, LoadingSpinnerProps } from './LoadingSpinner';

export interface LoadingOverlayProps {
  /** Whether the overlay is visible */
  open: boolean;
  /** Loading message to display */
  message?: string;
  /** Subtitle or additional context */
  subtitle?: string;
  /** Whether to show progress bar */
  showProgress?: boolean;
  /** Progress value (0-100) */
  progress?: number;
  /** Whether the overlay is closable by clicking backdrop */
  closable?: boolean;
  /** Callback when overlay is closed */
  onClose?: () => void;
  /** Custom backdrop opacity */
  backdropOpacity?: number;
  /** Whether to blur background content */
  blurBackground?: boolean;
  /** Whether to prevent interaction with background */
  disableBackdropClick?: boolean;
  /** Custom z-index */
  zIndex?: number;
  /** Spinner configuration */
  spinnerProps?: Partial<LoadingSpinnerProps>;
  /** Whether to use portal (render outside parent) */
  usePortal?: boolean;
  /** Custom container to render overlay in */
  container?: Element | (() => Element | null) | null;
  /** Transition timeout */
  timeout?: number;
  /** Custom styles */
  sx?: object;
}

// Styled components
const OverlayContainer = styled(Box, {
  shouldForwardProp: (prop) => !['blurBackground', 'backdropOpacity'].includes(prop as string),
})<{ blurBackground: boolean; backdropOpacity: number }>(
  ({ theme, blurBackground, backdropOpacity }) => ({
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'column',
    backgroundColor: `rgba(255, 255, 255, ${backdropOpacity})`,
    zIndex: theme.zIndex.modal,
    ...(blurBackground && {
      backdropFilter: 'blur(4px)',
      WebkitBackdropFilter: 'blur(4px)', // Safari support
    }),
  })
);

const ContentContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  gap: theme.spacing(3),
  padding: theme.spacing(4),
  borderRadius: theme.shape.borderRadius * 2,
  backgroundColor: 'rgba(255, 255, 255, 0.95)',
  boxShadow: theme.shadows[8],
  backdropFilter: 'blur(8px)',
  WebkitBackdropFilter: 'blur(8px)',
  border: `1px solid ${theme.palette.divider}`,
  minWidth: '280px',
  maxWidth: '400px',
  textAlign: 'center',
}));

const MessageContainer = styled(Box)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  gap: theme.spacing(1),
}));

const ProgressContainer = styled(Box)(({ theme }) => ({
  width: '100%',
  display: 'flex',
  flexDirection: 'column',
  gap: theme.spacing(1),
}));

/**
 * LoadingOverlay component for full-screen loading states
 * 
 * @example
 * ```tsx
 * // Simple overlay
 * <LoadingOverlay 
 *   open={isLoading} 
 *   message="Loading hives..." 
 * />
 * 
 * // With progress
 * <LoadingOverlay 
 *   open={isLoading}
 *   message="Uploading files"
 *   showProgress
 *   progress={uploadProgress}
 * />
 * 
 * // Closable overlay
 * <LoadingOverlay 
 *   open={isLoading}
 *   message="Processing..."
 *   closable
 *   onClose={() => setLoading(false)}
 * />
 * ```
 */
export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  open,
  message,
  subtitle,
  showProgress = false,
  progress = 0,
  closable = false,
  onClose,
  backdropOpacity = 0.8,
  blurBackground = true,
  disableBackdropClick = true,
  zIndex,
  spinnerProps = {},
  usePortal = true,
  container,
  timeout = 300,
  sx = {},
}) => {
  const theme = useTheme();

  const handleBackdropClick = (event: React.MouseEvent) => {
    if (!disableBackdropClick && closable && onClose) {
      event.stopPropagation();
      onClose();
    }
  };

  const content = (
    <Fade in={open} timeout={timeout}>
      <OverlayContainer
        blurBackground={blurBackground}
        backdropOpacity={backdropOpacity}
        onClick={handleBackdropClick}
        sx={{
          ...(zIndex && { zIndex }),
          ...sx,
        }}
      >
        <ContentContainer
          onClick={(e) => e.stopPropagation()} // Prevent closing when clicking content
        >
          {/* Loading Spinner */}
          <LoadingSpinner
            size="large"
            variant="inline"
            {...spinnerProps}
          />

          {/* Message Container */}
          {(message || subtitle) && (
            <MessageContainer>
              {message && (
                <Typography 
                  variant="h6" 
                  component="h2"
                  color="text.primary"
                  sx={{ fontWeight: 500 }}
                >
                  {message}
                </Typography>
              )}
              {subtitle && (
                <Typography 
                  variant="body2" 
                  color="text.secondary"
                  sx={{ maxWidth: '300px' }}
                >
                  {subtitle}
                </Typography>
              )}
            </MessageContainer>
          )}

          {/* Progress Indicator */}
          {showProgress && (
            <ProgressContainer>
              <LinearProgress
                variant={progress > 0 ? 'determinate' : 'indeterminate'}
                value={progress}
                sx={{
                  height: 6,
                  borderRadius: 3,
                  backgroundColor: theme.palette.grey[200],
                  '& .MuiLinearProgress-bar': {
                    borderRadius: 3,
                  },
                }}
              />
              {progress > 0 && (
                <Typography variant="caption" color="text.secondary">
                  {Math.round(progress)}%
                </Typography>
              )}
            </ProgressContainer>
          )}

          {/* Close instruction */}
          {closable && (
            <Typography 
              variant="caption" 
              color="text.secondary"
              sx={{ 
                fontSize: '0.75rem',
                opacity: 0.7,
              }}
            >
              Click outside to cancel
            </Typography>
          )}
        </ContentContainer>
      </OverlayContainer>
    </Fade>
  );

  if (!open) {
    return null;
  }

  if (usePortal) {
    return (
      <Portal container={container}>
        {content}
      </Portal>
    );
  }

  return content;
};

// Alternative backdrop-based implementation
export const BackdropLoadingOverlay: React.FC<LoadingOverlayProps> = ({
  open,
  message,
  onClose,
  closable = false,
  disableBackdropClick = true,
  zIndex,
  spinnerProps = {},
  timeout = 300,
  sx = {},
}) => {
  const handleClose = () => {
    if (closable && onClose) {
      onClose();
    }
  };

  return (
    <Backdrop
      open={open}
      onClick={!disableBackdropClick ? handleClose : undefined}
      sx={{
        color: '#fff',
        zIndex: zIndex || ((theme) => theme.zIndex.modal),
        backdropFilter: 'blur(4px)',
        ...sx,
      }}
      transitionDuration={timeout}
    >
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        gap={2}
        onClick={(e) => e.stopPropagation()}
      >
        <LoadingSpinner
          size="large"
          variant="inline"
          customColor="#fff"
          {...spinnerProps}
        />
        {message && (
          <Typography variant="h6" color="inherit">
            {message}
          </Typography>
        )}
      </Box>
    </Backdrop>
  );
};

// Hook for managing overlay state
export const useLoadingOverlay = () => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [message, setMessage] = React.useState<string>('');
  const [progress, setProgress] = React.useState<number>(0);

  const showOverlay = React.useCallback((loadingMessage?: string) => {
    setMessage(loadingMessage || '');
    setIsOpen(true);
  }, []);

  const hideOverlay = React.useCallback(() => {
    setIsOpen(false);
    setProgress(0);
  }, []);

  const updateProgress = React.useCallback((value: number) => {
    setProgress(Math.max(0, Math.min(100, value)));
  }, []);

  const updateMessage = React.useCallback((newMessage: string) => {
    setMessage(newMessage);
  }, []);

  return {
    isOpen,
    message,
    progress,
    showOverlay,
    hideOverlay,
    updateProgress,
    updateMessage,
  };
};

export default LoadingOverlay;