import React from 'react';
import {
  Box,
  Button,
  ButtonProps,
  CircularProgress,
  Fab,
  FabProps,
  IconButton,
  IconButtonProps,
  styled,
  useTheme,
} from '@mui/material';
import {LoadingButton, LoadingButtonProps} from '@mui/lab';

export type ButtonWithLoadingVariant = 'text' | 'outlined' | 'contained' | 'loading-button';

export interface BaseButtonWithLoadingProps {
  /** Whether the button is in loading state */
  loading?: boolean;
  /** Loading text to show when loading */
  loadingText?: string;
  /** Size of the loading spinner */
  spinnerSize?: number;
  /** Position of the loading spinner */
  spinnerPosition?: 'start' | 'center' | 'end';
  /** Whether to maintain button width during loading */
  maintainWidth?: boolean;
  /** Custom loading indicator */
  loadingIndicator?: React.ReactNode;
  /** Minimum loading time in ms */
  minLoadingTime?: number;
  /** Success state to show briefly after loading */
  success?: boolean;
  /** Success icon to show */
  successIcon?: React.ReactNode;
  /** Success duration in ms */
  successDuration?: number;
  /** Whether the button is disabled */
  disabled?: boolean;
}

export interface ButtonWithLoadingProps
    extends Omit<ButtonProps, 'disabled' | 'variant'>,
        BaseButtonWithLoadingProps {
  variant?: ButtonWithLoadingVariant;
}

export interface IconButtonWithLoadingProps
    extends Omit<IconButtonProps, 'disabled'>,
        BaseButtonWithLoadingProps {
}

export interface FabWithLoadingProps
    extends Omit<FabProps, 'disabled'>,
        BaseButtonWithLoadingProps {
}

// Styled components
const ButtonContainer = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'maintainWidth',
})<{ maintainWidth: boolean }>(({maintainWidth}) => ({
  display: 'inline-flex',
  position: 'relative',
  ...(maintainWidth && {
    minWidth: 'fit-content',
  }),
}));

const SpinnerContainer = styled(Box)(({theme: _theme}) => ({
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
}));

const LoadingContent = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'loading',
})<{ loading: boolean }>(({loading}) => ({
  display: 'flex',
  alignItems: 'center',
  gap: '8px',
  opacity: loading ? 0 : 1,
  transition: 'opacity 0.2s ease-in-out',
}));

/**
 * Custom hook for managing button loading state with minimum display time
 */
const useButtonLoading = (loading: boolean, minLoadingTime: number = 0): boolean => {
  const [isLoading, setIsLoading] = React.useState(false);
  const [showLoading, setShowLoading] = React.useState(false);
  const loadingStartTime = React.useRef<number | null>(null);

  React.useEffect(() => {
    if (loading && !isLoading) {
      // Start loading
      setIsLoading(true);
      setShowLoading(true);
      loadingStartTime.current = Date.now();
    } else if (!loading && isLoading) {
      // Stop loading with minimum time check
      const elapsed = loadingStartTime.current ? Date.now() - loadingStartTime.current : 0;
      const remainingTime = Math.max(0, minLoadingTime - elapsed);

      setTimeout(() => {
        setIsLoading(false);
        setShowLoading(false);
        loadingStartTime.current = null;
      }, remainingTime);
    }
  }, [loading, isLoading, minLoadingTime]);

  return showLoading;
};

/**
 * ButtonWithLoading component that shows loading state
 *
 * @example
 * ```tsx
 * <ButtonWithLoading
 *   loading={isSubmitting}
 *   loadingText="Saving..."
 *   onClick={handleSave}
 * >
 *   Save Changes
 * </ButtonWithLoading>
 * ```
 */
export const ButtonWithLoading: React.FC<ButtonWithLoadingProps> = ({
                                                                      loading = false,
                                                                      loadingText,
                                                                      spinnerSize = 20,
                                                                      spinnerPosition = 'start',
                                                                      maintainWidth = true,
                                                                      loadingIndicator,
                                                                      minLoadingTime = 0,
                                                                      success = false,
                                                                      successIcon,
                                                                      successDuration = 2000,
                                                                      variant = 'button',
                                                                      children,
                                                                      onClick,
                                                                      disabled,
                                                                      sx,
                                                                      ...props
                                                                    }) => {
  const theme = useTheme();
  const [showSuccess, setShowSuccess] = React.useState(false);
  const showLoading = useButtonLoading(loading, minLoadingTime);
  const [buttonWidth, setButtonWidth] = React.useState<number | undefined>();
  const buttonRef = React.useRef<HTMLButtonElement>(null);

  // Handle success state
  React.useEffect(() => {
    if (success && !loading) {
      setShowSuccess(true);
      const timer = setTimeout(() => {
        setShowSuccess(false);
      }, successDuration);
      return () => clearTimeout(timer);
    }
  }, [success, loading, successDuration]);

  // Capture button width for maintaining size
  React.useEffect(() => {
    if (maintainWidth && buttonRef.current && !showLoading) {
      setButtonWidth(buttonRef.current.offsetWidth);
    }
  }, [maintainWidth, showLoading, children]);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
    if (!showLoading && !disabled && onClick) {
      const result = onClick(event);
      // Handle any return value if needed
      if (result !== undefined) {
        // onClick might return a value, but we don't need to do anything with it
      }
    }
  };

  const isDisabled = disabled || showLoading;

  const renderSpinner = (): React.ReactElement | null => {
    if (loadingIndicator) {
      return loadingIndicator as React.ReactElement;
    }

    return (
        <CircularProgress
            size={spinnerSize}
            sx={{
              color: theme.palette.primary.contrastText,
            }}
        />
    );
  };

  const renderContent = (): React.ReactNode => {
    if (showSuccess && successIcon) {
      return successIcon;
    }

    if (showLoading) {
      if (spinnerPosition === 'center') {
        return (
            <>
              <LoadingContent loading={true}>
                {loadingText || children}
              </LoadingContent>
              <SpinnerContainer>
                {renderSpinner()}
              </SpinnerContainer>
            </>
        );
      }

      return (
          <>
            {spinnerPosition === 'start' && renderSpinner()}
            {loadingText || (spinnerPosition === 'end' && children)}
            {spinnerPosition === 'end' && renderSpinner()}
          </>
      );
    }

    return children;
  };

  if (variant === 'loading-button') {
    return (
        <LoadingButton
            loading={showLoading}
            loadingPosition={spinnerPosition}
            loadingIndicator={loadingIndicator}
            disabled={isDisabled}
            onClick={handleClick}
            ref={buttonRef}
            sx={{
              ...(maintainWidth && buttonWidth && {minWidth: buttonWidth}),
              ...sx,
            }}
            {...(props as LoadingButtonProps)}
        >
          {showSuccess && successIcon ? successIcon : (loadingText && showLoading ? loadingText : children)}
        </LoadingButton>
    );
  }

  return (
      <ButtonContainer maintainWidth={maintainWidth}>
        <Button
            disabled={isDisabled}
            onClick={handleClick}
            ref={buttonRef}
            sx={{
              ...(maintainWidth && buttonWidth && {minWidth: buttonWidth}),
              position: 'relative',
              ...sx,
            }}
            {...props}
        >
          {renderContent()}
        </Button>
      </ButtonContainer>
  );
};

/**
 * IconButtonWithLoading component
 */
export const IconButtonWithLoading: React.FC<IconButtonWithLoadingProps> = ({
                                                                              loading = false,
                                                                              spinnerSize = 20,
                                                                              loadingIndicator,
                                                                              minLoadingTime = 0,
                                                                              success = false,
                                                                              successIcon,
                                                                              successDuration = 2000,
                                                                              children,
                                                                              onClick,
                                                                              disabled,
                                                                              sx,
                                                                              ...props
                                                                            }) => {
  const _theme = useTheme();
  const [showSuccess, setShowSuccess] = React.useState(false);
  const showLoading = useButtonLoading(loading, minLoadingTime);

  React.useEffect(() => {
    if (success && !loading) {
      setShowSuccess(true);
      const timer = setTimeout(() => {
        setShowSuccess(false);
      }, successDuration);
      return () => clearTimeout(timer);
    }
  }, [success, loading, successDuration]);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
    if (!showLoading && !disabled && onClick) {
      const result = onClick(event);
      // Handle any return value if needed
      if (result !== undefined) {
        // onClick might return a value, but we don't need to do anything with it
      }
    }
  };

  const renderContent = (): React.ReactNode => {
    if (showSuccess && successIcon) {
      return successIcon;
    }

    if (showLoading) {
      return loadingIndicator || (
          <CircularProgress
              size={spinnerSize}
              sx={{color: 'inherit'}}
          />
      );
    }

    return children;
  };

  return (
      <IconButton
          disabled={disabled || showLoading}
          onClick={handleClick}
          sx={{
            position: 'relative',
            ...sx,
          }}
          {...props}
      >
        {renderContent()}
      </IconButton>
  );
};

/**
 * FabWithLoading component
 */
export const FabWithLoading: React.FC<FabWithLoadingProps> = ({
                                                                loading = false,
                                                                spinnerSize = 24,
                                                                loadingIndicator,
                                                                minLoadingTime = 0,
                                                                success = false,
                                                                successIcon,
                                                                successDuration = 2000,
                                                                children,
                                                                onClick,
                                                                disabled,
                                                                sx,
                                                                ...props
                                                              }) => {
  const [showSuccess, setShowSuccess] = React.useState(false);
  const showLoading = useButtonLoading(loading, minLoadingTime);

  React.useEffect(() => {
    if (success && !loading) {
      setShowSuccess(true);
      const timer = setTimeout(() => {
        setShowSuccess(false);
      }, successDuration);
      return () => clearTimeout(timer);
    }
  }, [success, loading, successDuration]);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
    if (!showLoading && !disabled && onClick) {
      const result = onClick(event);
      // Handle any return value if needed
      if (result !== undefined) {
        // onClick might return a value, but we don't need to do anything with it
      }
    }
  };

  const renderContent = (): React.ReactNode => {
    if (showSuccess && successIcon) {
      return successIcon;
    }

    if (showLoading) {
      return loadingIndicator || (
          <CircularProgress
              size={spinnerSize}
              sx={{color: 'inherit'}}
          />
      );
    }

    return children;
  };

  return (
      <Fab
          disabled={disabled || showLoading}
          onClick={handleClick}
          sx={{
            position: 'relative',
            ...sx,
          }}
          {...props}
      >
        {renderContent()}
      </Fab>
  );
};

export default ButtonWithLoading;