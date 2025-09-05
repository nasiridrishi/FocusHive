/**
 * Accessible Button Components
 * 
 * WCAG 2.1 AA compliant button variants with proper ARIA attributes,
 * keyboard navigation, focus management, and screen reader support.
 */

import React, { forwardRef } from 'react';
import { Button, ButtonProps, IconButton, IconButtonProps, Fab, FabProps } from '@mui/material';
import { styled } from '@mui/material/styles';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';
import type { AriaRole, AccessibleProps } from '../types/accessibility';

// Enhanced Button with accessibility features
const StyledAccessibleButton = styled(Button, {
  shouldForwardProp: (prop) => 
    prop !== 'hasConfirmation' && 
    prop !== 'loadingText' && 
    prop !== 'successText'
})<{
  hasConfirmation?: boolean;
  loadingText?: string;
  successText?: string;
}>(({ theme }) => ({
  // Ensure minimum touch target size
  minHeight: '44px',
  minWidth: '44px',
  
  // Enhanced focus ring
  '&:focus': {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: '2px',
    backgroundColor: theme.palette.action.focus,
  },
  
  // High contrast mode support
  '@media (prefers-contrast: high)': {
    border: '2px solid currentColor',
    '&:focus': {
      outline: '3px solid currentColor',
      outlineOffset: '3px',
    },
  },
  
  // Loading state accessibility
  '&[aria-busy="true"]': {
    pointerEvents: 'none',
    position: 'relative',
    
    '&::after': {
      content: '""',
      position: 'absolute',
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)',
      width: '16px',
      height: '16px',
      border: '2px solid transparent',
      borderTop: `2px solid ${theme.palette.primary.contrastText}`,
      borderRadius: '50%',
      animation: 'spin 1s linear infinite',
    },
  },
  
  '@keyframes spin': {
    '0%': { transform: 'translate(-50%, -50%) rotate(0deg)' },
    '100%': { transform: 'translate(-50%, -50%) rotate(360deg)' },
  },
}));

export interface AccessibleButtonProps extends Omit<ButtonProps, 'role'>, AccessibleProps {
  /**
   * ARIA role for the button
   * @default 'button'
   */
  role?: AriaRole;
  
  /**
   * Loading state for async operations
   */
  loading?: boolean;
  
  /**
   * Text to announce when button is in loading state
   */
  loadingText?: string;
  
  /**
   * Success state after completion
   */
  success?: boolean;
  
  /**
   * Text to announce on success
   */
  successText?: string;
  
  /**
   * Requires confirmation before action
   */
  requiresConfirmation?: boolean;
  
  /**
   * Confirmation message
   */
  confirmationText?: string;
  
  /**
   * Auto-focus on mount
   */
  autoFocus?: boolean;
  
  /**
   * Keyboard shortcut description for screen readers
   */
  keyboardShortcut?: string;
  
  /**
   * Additional context for screen readers
   */
  description?: string;
}

/**
 * Accessible Button Component
 * 
 * Extends MUI Button with WCAG 2.1 AA compliance features:
 * - Proper ARIA attributes and roles
 * - Loading and success state announcements
 * - Keyboard navigation support
 * - High contrast mode compatibility
 * - Minimum touch target sizing
 * - Focus management and visual indicators
 */
export const AccessibleButton = forwardRef<HTMLButtonElement, AccessibleButtonProps>(({
  children,
  onClick,
  disabled = false,
  loading = false,
  success = false,
  loadingText = 'Loading...',
  successText = 'Action completed successfully',
  requiresConfirmation = false,
  confirmationText = 'Are you sure?',
  autoFocus = false,
  keyboardShortcut,
  description,
  role = 'button',
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
  ...props
}, ref) => {
  const { announceStatus, announcePolite } = useAnnouncement();
  const [isConfirming, setIsConfirming] = React.useState(false);

  // Generate unique IDs for ARIA relationships
  const descriptionId = React.useId();
  const shortcutId = React.useId();

  const handleClick = async (event: React.MouseEvent<HTMLButtonElement>) => {
    if (loading || disabled) return;

    setHasInteracted(true);

    if (requiresConfirmation && !isConfirming) {
      setIsConfirming(true);
      announcePolite(confirmationText + ' Press Enter to confirm or Escape to cancel.');
      return;
    }

    if (loading) {
      announceStatus(loadingText);
    }

    try {
      await onClick?.(event);
      
      if (success) {
        announceStatus(successText);
      }
    } catch (error) {
      announceStatus('Action failed. Please try again.');
    } finally {
      setIsConfirming(false);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'Escape' && isConfirming) {
      setIsConfirming(false);
      announcePolite('Action cancelled.');
      event.preventDefault();
      return;
    }

    if (event.key === 'Enter' && isConfirming) {
      setIsConfirming(false);
      handleClick(event as unknown);
      event.preventDefault();
      return;
    }

    // Handle keyboard shortcuts
    props.onKeyDown?.(event);
  };

  // Construct ARIA describedby
  const describedByIds = [
    ariaDescribedBy,
    description ? descriptionId : null,
    keyboardShortcut ? shortcutId : null,
  ].filter(Boolean).join(' ') || undefined;

  const buttonLabel = ariaLabel || (typeof children === 'string' ? children : undefined);
  const isDisabled = disabled || loading;
  const showConfirmationState = isConfirming && requiresConfirmation;

  return (
    <>
      <StyledAccessibleButton
        {...props}
        ref={ref}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
        disabled={isDisabled}
        autoFocus={autoFocus}
        role={role}
        aria-label={buttonLabel}
        aria-describedby={describedByIds}
        aria-busy={loading}
        aria-pressed={showConfirmationState ? true : undefined}
        data-state={
          loading ? 'loading' : 
          success ? 'success' : 
          showConfirmationState ? 'confirming' : 
          'default'
        }
      >
        {loading ? loadingText : 
         showConfirmationState ? `Confirm: ${children}` : 
         children}
      </StyledAccessibleButton>

      {/* Hidden description for screen readers */}
      {description && (
        <span id={descriptionId} style={{ 
          position: 'absolute', 
          width: '1px', 
          height: '1px', 
          padding: 0, 
          margin: '-1px', 
          overflow: 'hidden', 
          clip: 'rect(0, 0, 0, 0)', 
          whiteSpace: 'nowrap', 
          border: 0 
        }}>
          {description}
        </span>
      )}

      {/* Hidden keyboard shortcut info */}
      {keyboardShortcut && (
        <span id={shortcutId} style={{ 
          position: 'absolute', 
          width: '1px', 
          height: '1px', 
          padding: 0, 
          margin: '-1px', 
          overflow: 'hidden', 
          clip: 'rect(0, 0, 0, 0)', 
          whiteSpace: 'nowrap', 
          border: 0 
        }}>
          Keyboard shortcut: {keyboardShortcut}
        </span>
      )}
    </>
  );
});

AccessibleButton.displayName = 'AccessibleButton';

// Enhanced IconButton with accessibility features
export interface AccessibleIconButtonProps extends Omit<IconButtonProps, 'role'>, AccessibleProps {
  /**
   * Required: Accessible label for icon button
   */
  'aria-label': string;
  
  /**
   * ARIA role for the icon button
   */
  role?: AriaRole;
  
  /**
   * Loading state
   */
  loading?: boolean;
  
  /**
   * Description of what the icon represents
   */
  iconDescription?: string;
}

/**
 * Accessible Icon Button Component
 * 
 * Icon buttons require accessible labels and proper ARIA attributes
 */
export const AccessibleIconButton = forwardRef<HTMLButtonElement, AccessibleIconButtonProps>(({
  children,
  'aria-label': ariaLabel,
  role = 'button',
  loading = false,
  iconDescription,
  ...props
}, ref) => {
  const descriptionId = React.useId();

  return (
    <>
      <IconButton
        {...props}
        ref={ref}
        role={role}
        aria-label={ariaLabel}
        aria-describedby={iconDescription ? descriptionId : undefined}
        aria-busy={loading}
        disabled={props.disabled || loading}
        sx={{
          minHeight: '44px',
          minWidth: '44px',
          '&:focus': {
            outline: '2px solid',
            outlineColor: 'primary.main',
            outlineOffset: '2px',
          },
          '@media (prefers-contrast: high)': {
            border: '2px solid currentColor',
            '&:focus': {
              outline: '3px solid currentColor',
              outlineOffset: '3px',
            },
          },
          ...props.sx,
        }}
      >
        {children}
      </IconButton>

      {iconDescription && (
        <span id={descriptionId} style={{ 
          position: 'absolute', 
          width: '1px', 
          height: '1px', 
          padding: 0, 
          margin: '-1px', 
          overflow: 'hidden', 
          clip: 'rect(0, 0, 0, 0)', 
          whiteSpace: 'nowrap', 
          border: 0 
        }}>
          {iconDescription}
        </span>
      )}
    </>
  );
});

AccessibleIconButton.displayName = 'AccessibleIconButton';

// Enhanced FAB with accessibility features
export interface AccessibleFabProps extends Omit<FabProps, 'role'>, AccessibleProps {
  /**
   * Required: Accessible label for FAB
   */
  'aria-label': string;
  
  /**
   * ARIA role for the FAB
   */
  role?: AriaRole;
  
  /**
   * Expanded state for expandable FABs
   */
  expanded?: boolean;
  
  /**
   * Controls other elements (for expandable FABs)
   */
  'aria-controls'?: string;
}

/**
 * Accessible Floating Action Button
 */
export const AccessibleFab = forwardRef<HTMLButtonElement, AccessibleFabProps>(({
  'aria-label': ariaLabel,
  role = 'button',
  expanded,
  ...props
}, ref) => {
  return (
    <Fab
      {...props}
      ref={ref}
      role={role}
      aria-label={ariaLabel}
      aria-expanded={expanded}
      sx={{
        '&:focus': {
          outline: '2px solid',
          outlineColor: 'primary.main',
          outlineOffset: '2px',
        },
        '@media (prefers-contrast: high)': {
          border: '2px solid currentColor',
          '&:focus': {
            outline: '3px solid currentColor',
            outlineOffset: '3px',
          },
        },
        ...props.sx,
      }}
    />
  );
});

AccessibleFab.displayName = 'AccessibleFab';

/**
 * Button Group with enhanced keyboard navigation
 */
export interface AccessibleButtonGroupProps {
  children: React.ReactElement<AccessibleButtonProps>[];
  orientation?: 'horizontal' | 'vertical';
  role?: 'group' | 'radiogroup' | 'toolbar';
  'aria-label'?: string;
  'aria-labelledby'?: string;
}

export const AccessibleButtonGroup: React.FC<AccessibleButtonGroupProps> = ({
  children,
  orientation = 'horizontal',
  role = 'group',
  'aria-label': ariaLabel,
  'aria-labelledby': ariaLabelledBy,
}) => {
  const { keyboardNavigationProps } = useKeyboardNavigation({
    orientation,
    wrap: true,
    activateOnFocus: false,
  });

  return (
    <div
      role={role}
      aria-label={ariaLabel}
      aria-labelledby={ariaLabelledBy}
      aria-orientation={orientation}
      {...keyboardNavigationProps}
      style={{
        display: 'flex',
        flexDirection: orientation === 'vertical' ? 'column' : 'row',
        gap: '8px',
      }}
    >
      {React.Children.map(children, (child, index) => 
        React.cloneElement(child, {
          tabIndex: index === 0 ? 0 : -1,
        })
      )}
    </div>
  );
};

export default AccessibleButton;