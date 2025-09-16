import React from 'react';
import {
  Box,
  Paper,
  LinearProgress,
  Typography,
  Stack,
  styled,
  alpha,
} from '@mui/material';
import { LoadingSpinner } from './LoadingSpinner';
import { LoadingOverlay } from './LoadingOverlay';

export interface FormWithLoadingProps {
  /** Form content */
  children: React.ReactNode;
  /** Whether the form is in loading/submitting state */
  loading?: boolean;
  /** Loading message to display */
  loadingMessage?: string;
  /** Whether to show progress bar */
  showProgress?: boolean;
  /** Progress value (0-100) */
  progress?: number;
  /** Loading variant */
  variant?: 'overlay' | 'inline' | 'disable' | 'progress-only';
  /** Whether to disable form interactions during loading */
  disableInteraction?: boolean;
  /** Custom loading indicator */
  loadingIndicator?: React.ReactNode;
  /** Form container props */
  containerProps?: React.ComponentProps<typeof Paper>;
  /** Custom styles */
  sx?: object;
  /** Callback when form submission starts */
  onSubmitStart?: () => void;
  /** Callback when form submission ends */
  onSubmitEnd?: () => void;
  /** Whether to prevent multiple submissions */
  preventMultipleSubmissions?: boolean;
}

// Styled components
const FormContainer = styled(Paper, {
  shouldForwardProp: (prop) => !['loading', 'disableInteraction'].includes(prop as string),
})<{ loading: boolean; disableInteraction: boolean }>(
  ({ loading, disableInteraction }) => ({
    position: 'relative',
    overflow: 'hidden',
    transition: 'opacity 300ms, filter 300ms',
    ...(loading && disableInteraction && {
      pointerEvents: 'none',
      opacity: 0.7,
      filter: 'grayscale(20%)',
       
      ['& *']: {
        userSelect: 'none',
      },
    }),
  })
);

const ProgressContainer = styled(Box)(({ theme }) => ({
  position: 'absolute',
  top: 0,
  left: 0,
  right: 0,
  zIndex: 1,
  backgroundColor: alpha(theme.palette.background.paper, 0.9),
}));

const InlineLoadingContainer = styled(Stack)(({ theme }) => ({
  alignItems: 'center',
  padding: theme.spacing(2),
  backgroundColor: alpha(theme.palette.primary.main, 0.04),
  borderRadius: theme.shape.borderRadius,
  border: `1px solid ${alpha(theme.palette.primary.main, 0.12)}`,
}));

const DisabledOverlay = styled(Box)(({ theme }) => ({
  position: 'absolute',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: alpha(theme.palette.background.paper, 0.8),
  backdropFilter: 'blur(1px)',
  zIndex: 1,
  pointerEvents: 'all',
  cursor: 'not-allowed',
}));

/**
 * FormWithLoading component that provides loading states for forms
 */
export const FormWithLoading: React.FC<FormWithLoadingProps> = ({
  children,
  loading = false,
  loadingMessage = 'Processing...',
  showProgress = false,
  progress = 0,
  variant = 'overlay',
  disableInteraction = true,
  loadingIndicator,
  containerProps = {},
  sx = {},
  onSubmitStart,
  onSubmitEnd,
  preventMultipleSubmissions = true,
}) => {
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const prevLoading = React.useRef(loading);

  // Handle submission state changes
  React.useEffect(() => {
    if (loading && !prevLoading.current) {
      setIsSubmitting(true);
      onSubmitStart?.();
    } else if (!loading && prevLoading.current) {
      setIsSubmitting(false);
      onSubmitEnd?.();
    }
    prevLoading.current = loading;
  }, [loading, onSubmitStart, onSubmitEnd]);

  // Prevent multiple submissions by intercepting form submission
  const handleFormSubmit = React.useCallback((event: React.FormEvent) => {
    if (preventMultipleSubmissions && (loading || isSubmitting)) {
      event.preventDefault();
      event.stopPropagation();
      return false;
    }
  }, [loading, isSubmitting, preventMultipleSubmissions]);

  const renderLoadingIndicator = (): React.ReactNode => {
    if (loadingIndicator) {
      return loadingIndicator;
    }

    switch (variant) {
      case 'inline':
        return (
          <InlineLoadingContainer spacing={2} direction="row">
            <LoadingSpinner size="small" />
            <Typography variant="body2" color="text.secondary">
              {loadingMessage}
            </Typography>
          </InlineLoadingContainer>
        );

      case 'progress-only':
        return (
          <ProgressContainer>
            <LinearProgress
              variant={showProgress && progress > 0 ? 'determinate' : 'indeterminate'}
              value={progress}
              sx={{
                height: showProgress ? 6 : 4,
                '& .MuiLinearProgress-bar': {
                  borderRadius: showProgress ? 3 : 0,
                },
              }}
            />
            {showProgress && progress > 0 && (
              <Box px={2} py={1}>
                <Typography variant="caption" color="text.secondary">
                  {loadingMessage} ({Math.round(progress)}%)
                </Typography>
              </Box>
            )}
          </ProgressContainer>
        );

      case 'disable':
        return null;

      case 'overlay':
      default:
        return (
          <LoadingOverlay
            open={loading}
            message={loadingMessage}
            showProgress={showProgress}
            progress={progress}
            usePortal={false}
            sx={{ position: 'absolute' }}
          />
        );
    }
  };

  const enhancedChildren = React.useMemo(() => {
    if (!preventMultipleSubmissions) {
      return children;
    }

    const processChildren = (child: React.ReactNode): React.ReactNode => {
      if (React.isValidElement(child)) {
        if (child.type === 'form') {
          return React.cloneElement(child as React.ReactElement<React.FormHTMLAttributes<HTMLFormElement>>, {
            onSubmit: (event: React.FormEvent) => {
              const result = handleFormSubmit(event);
              if (result !== false && child.props.onSubmit) {
                child.props.onSubmit(event);
              }
            },
          });
        }

        if (child.props?.children) {
          return React.cloneElement(child as React.ReactElement<{ children?: React.ReactNode }>, {
            children: React.Children.map(child.props.children, processChildren),
          });
        }
      }

      return child;
    };

    return React.Children.map(children, processChildren);
  }, [children, handleFormSubmit, preventMultipleSubmissions]);

  return (
    <FormContainer
      loading={loading}
      disableInteraction={disableInteraction && loading}
      sx={{
        ...sx,
      }}
      {...containerProps}
    >
      {loading && variant === 'progress-only' && renderLoadingIndicator()}

      <Box sx={{ position: 'relative' }}>
        {enhancedChildren}

        {loading && variant === 'overlay' && renderLoadingIndicator()}

        {loading && variant === 'disable' && (
          <DisabledOverlay />
        )}
      </Box>

      {loading && variant === 'inline' && (
        <Box mt={2}>
          {renderLoadingIndicator()}
        </Box>
      )}
    </FormContainer>
  );
};

export default FormWithLoading;
