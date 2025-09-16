import React from 'react';
import {
  Box,
  CircularProgress,
  CircularProgressProps,
  styled,
  Typography,
  useTheme,
} from '@mui/material';

export type LoadingSpinnerSize = 'small' | 'medium' | 'large' | 'extra-large';
export type LoadingSpinnerVariant = 'inline' | 'overlay' | 'centered';

export interface LoadingSpinnerProps extends Omit<CircularProgressProps, 'size' | 'variant'> {
  /** Size of the spinner */
  size?: LoadingSpinnerSize;
  /** Variant determines the layout and positioning */
  variant?: LoadingSpinnerVariant;
  /** Optional message to display with the spinner */
  message?: string;
  /** Whether to show a backdrop overlay */
  overlay?: boolean;
  /** Minimum time to show spinner (prevents flash) */
  minDisplayTime?: number;
  /** Custom color override */
  customColor?: string;
  /** Whether to disable the component (makes it invisible) */
  disabled?: boolean;
}

const sizeMap: Record<LoadingSpinnerSize, number> = {
  small: 16,
  medium: 24,
  large: 40,
  'extra-large': 56,
};

// Styled components
const OverlayContainer = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'overlay',
})<{ overlay: boolean }>(({theme, overlay}) => ({
  position: overlay ? 'absolute' : 'relative',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  flexDirection: 'column',
  gap: theme.spacing(2),
  ...(overlay && {
    backgroundColor: 'rgba(255, 255, 255, 0.8)',
    backdropFilter: 'blur(2px)',
    zIndex: theme.zIndex.modal - 1,
  }),
}));

const CenteredContainer = styled(Box)(({theme}) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  flexDirection: 'column',
  gap: theme.spacing(2),
  minHeight: '200px',
  width: '100%',
}));

const InlineContainer = styled(Box)(({theme}) => ({
  display: 'inline-flex',
  alignItems: 'center',
  gap: theme.spacing(1),
}));

const LoadingMessage = styled(Typography)(({theme}) => ({
  color: theme.palette.text.secondary,
  textAlign: 'center',
  userSelect: 'none',
}));

/**
 * LoadingSpinner component with multiple variants and sizes
 *
 * @example
 * ```tsx
 * // Simple inline spinner
 * <LoadingSpinner size="small" />
 *
 * // Centered with message
 * <LoadingSpinner 
 *   variant="centered" 
 *   message="Loading hives..." 
 *   size="large" 
 * />
 *
 * // Overlay spinner
 * <LoadingSpinner 
 *   variant="overlay" 
 *   overlay 
 *   message="Processing..." 
 * />
 * ```
 */
export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
                                                                size = 'medium',
                                                                variant = 'inline',
                                                                message,
                                                                overlay = false,
                                                                customColor,
                                                                disabled = false,
                                                                minDisplayTime = 0,
                                                                ...props
                                                              }) => {
  const theme = useTheme();
  const [shouldShow, setShouldShow] = React.useState(true);

  // Handle minimum display time
  React.useEffect(() => {
    if (minDisplayTime > 0) {
      const timer = setTimeout(() => {
        setShouldShow(false);
      }, minDisplayTime);

      return () => clearTimeout(timer);
    }
  }, [minDisplayTime]);

  // Don't render if disabled or if minimum time hasn't passed
  if (disabled || (!shouldShow && minDisplayTime > 0)) {
    return null;
  }

  const spinnerSize = sizeMap[size];
  const color = customColor || theme.palette.primary.main;

  const spinner = (
      <CircularProgress
          size={spinnerSize}
          sx={{
            color: color,
            ...(props.sx || {}),
          }}
          {...props}
      />
  );

  const content = (
      <>
        {spinner}
        {message && (
            <LoadingMessage variant="body2">
              {message}
            </LoadingMessage>
        )}
      </>
  );

  // Render based on variant
  switch (variant) {
    case 'overlay':
      return (
          <OverlayContainer overlay={overlay}>
            {content}
          </OverlayContainer>
      );

    case 'centered':
      return (
          <CenteredContainer>
            {content}
          </CenteredContainer>
      );

    case 'inline':
    default:
      return (
          <InlineContainer>
            {content}
          </InlineContainer>
      );
  }
};

// Convenience components for common use cases
export const InlineSpinner: React.FC<Omit<LoadingSpinnerProps, 'variant'>> = (props) => (
    <LoadingSpinner variant="inline" {...props} />
);

export const CenteredSpinner: React.FC<Omit<LoadingSpinnerProps, 'variant'>> = (props) => (
    <LoadingSpinner variant="centered" {...props} />
);

export const OverlaySpinner: React.FC<Omit<LoadingSpinnerProps, 'variant' | 'overlay'>> = (props) => (
    <LoadingSpinner variant="overlay" overlay {...props} />
);

// Higher-order component for conditional loading is available in './withLoading'

export default LoadingSpinner;