/**
 * Accessible Modal and Dialog Components
 * 
 * WCAG 2.1 AA compliant modal components with proper focus management,
 * keyboard navigation, screen reader support, and ARIA attributes.
 */

import React, { forwardRef } from 'react';
import {
  Dialog,
  DialogProps,
  DialogTitle,
  DialogTitleProps,
  DialogContent,
  DialogContentProps,
  DialogActions,
  DialogActionsProps,
  Modal,
  ModalProps,
  Backdrop,
  Paper,
  IconButton,
  Typography,
  Box,
  Slide,
  Fade,
  Grow,
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import { TransitionProps } from '@mui/material/transitions';
import { styled } from '@mui/material/styles';
import { useModalFocusTrap } from '../hooks/useFocusTrap';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { ScreenReaderOnly } from './ScreenReaderOnly';
import type { AccessibleProps } from '../types/accessibility';

// Enhanced Modal container with accessibility features
const StyledModalContainer = styled(Box)(({ theme }) => ({
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: theme.spacing(2),
  
  // Ensure modal is above other content
  zIndex: theme.zIndex.modal,
  
  // High contrast mode support
  '@media (prefers-contrast: high)': {
    '& .modal-content': {
      border: '3px solid currentColor',
    },
  },
}));

const StyledModalContent = styled(Paper)(({ theme }) => ({
  position: 'relative',
  maxWidth: '90vw',
  maxHeight: '90vh',
  overflow: 'auto',
  backgroundColor: theme.palette.background.paper,
  borderRadius: theme.shape.borderRadius,
  boxShadow: theme.shadows[24],
  
  '&:focus': {
    outline: 'none',
  },
  
  // Reduced motion support
  '@media (prefers-reduced-motion: reduce)': {
    transition: 'none',
  },
}));

// Transition components for different modal animations
const SlideTransition = React.forwardRef<unknown, TransitionProps & { children: React.ReactElement }>((props, ref) => (
  <Slide direction="up" ref={ref} {...props} />
));
SlideTransition.displayName = 'SlideTransition';

const FadeTransition = React.forwardRef<unknown, TransitionProps & { children: React.ReactElement }>((props, ref) => (
  <Fade ref={ref} {...props} />
));
FadeTransition.displayName = 'FadeTransition';

const GrowTransition = React.forwardRef<unknown, TransitionProps & { children: React.ReactElement }>((props, ref) => (
  <Grow ref={ref} {...props} />
));
GrowTransition.displayName = 'GrowTransition';

export interface AccessibleModalProps extends Omit<ModalProps, 'children'>, AccessibleProps {
  /**
   * Modal content
   */
  children: React.ReactNode;
  
  /**
   * Modal title (required for screen readers)
   */
  title: string;
  
  /**
   * Modal description for screen readers
   */
  description?: string;
  
  /**
   * Show close button
   */
  showCloseButton?: boolean;
  
  /**
   * Close button label
   */
  closeButtonLabel?: string;
  
  /**
   * Custom close handler
   */
  onClose?: (event: {}, reason: 'backdropClick' | 'escapeKeyDown' | 'closeButtonClick') => void;
  
  /**
   * Auto-focus element selector on open
   */
  autoFocusSelector?: string;
  
  /**
   * Return focus element selector on close
   */
  returnFocusSelector?: string;
  
  /**
   * Animation transition type
   */
  transition?: 'slide' | 'fade' | 'grow' | 'none';
  
  /**
   * Modal size
   */
  size?: 'small' | 'medium' | 'large' | 'fullscreen';
  
  /**
   * Prevent close on backdrop click
   */
  disableBackdropClose?: boolean;
  
  /**
   * Prevent close on escape key
   */
  disableEscapeKeyDown?: boolean;
}

/**
 * Accessible Modal Component
 * 
 * Features:
 * - Focus trap and restoration
 * - Keyboard navigation (Escape to close, Tab cycling)
 * - Screen reader announcements
 * - ARIA labels and descriptions
 * - High contrast mode support
 * - Reduced motion support
 */
export const AccessibleModal = forwardRef<HTMLDivElement, AccessibleModalProps>(({
  children,
  title,
  description,
  showCloseButton = true,
  closeButtonLabel = 'Close modal',
  onClose,
  autoFocusSelector,
  returnFocusSelector,
  transition = 'fade',
  size = 'medium',
  disableBackdropClose = false,
  disableEscapeKeyDown = false,
  open,
  ...props
}, ref) => {
  const modalRef = React.useRef<HTMLDivElement>(null);
  const { announcePolite } = useAnnouncement();
  
  // Set up focus trap
  useModalFocusTrap(open, () => onClose?.({}, 'escapeKeyDown'));
  
  // Generate unique IDs for ARIA relationships
  const titleId = React.useId();
  const descriptionId = React.useId();

  // Handle modal open announcements
  React.useEffect(() => {
    if (open) {
      // Announce modal opening
      announcePolite(`${title} dialog opened`);
      
      // Auto-focus specific element if provided
      if (autoFocusSelector) {
        const element = document.querySelector(autoFocusSelector) as HTMLElement;
        element?.focus();
      }
    }
  }, [open, title, announcePolite, autoFocusSelector]);

  // Handle modal close
  const handleClose = (event: any, reason: 'backdropClick' | 'escapeKeyDown') => {
    if (reason === 'backdropClick' && disableBackdropClose) return;
    if (reason === 'escapeKeyDown' && disableEscapeKeyDown) return;
    
    announcePolite(`${title} dialog closed`);
    
    // Return focus to specific element if provided
    if (returnFocusSelector) {
      setTimeout(() => {
        const element = document.querySelector(returnFocusSelector) as HTMLElement;
        element?.focus();
      }, 100);
    }
    
    onClose?.(event, reason);
  };

  const handleCloseButtonClick = () => {
    handleClose({}, 'closeButtonClick' as any);
  };

  // Size configurations
  const sizeProps = {
    small: { maxWidth: '400px' },
    medium: { maxWidth: '600px' },
    large: { maxWidth: '900px' },
    fullscreen: { maxWidth: '100vw', maxHeight: '100vh', margin: 0 },
  };

  // Transition components
  const TransitionComponent = {
    slide: SlideTransition,
    fade: FadeTransition,
    grow: GrowTransition,
    none: React.Fragment,
  }[transition];

  const modalContent = (
    <StyledModalContainer>
      <StyledModalContent
        ref={modalRef}
        className="modal-content"
        sx={sizeProps[size]}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        tabIndex={-1}
      >
        {/* Modal Header */}
        <Box
          sx={{
            p: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid',
            borderColor: 'divider',
          }}
        >
          <Typography
            id={titleId}
            variant="h2"
            component="h1"
            sx={{ fontSize: '1.25rem', fontWeight: 'bold', m: 0 }}
          >
            {title}
          </Typography>
          
          {showCloseButton && (
            <IconButton
              onClick={handleCloseButtonClick}
              aria-label={closeButtonLabel}
              sx={{
                ml: 2,
                '&:focus': {
                  outline: '2px solid',
                  outlineColor: 'primary.main',
                  outlineOffset: '2px',
                },
              }}
            >
              <CloseIcon />
            </IconButton>
          )}
        </Box>

        {/* Hidden description for screen readers */}
        {description && (
          <ScreenReaderOnly id={descriptionId}>
            {description}
          </ScreenReaderOnly>
        )}

        {/* Modal Content */}
        <Box sx={{ p: 2 }}>
          {children}
        </Box>
      </StyledModalContent>
    </StyledModalContainer>
  );

  return (
    <Modal
      {...props}
      ref={ref}
      open={open}
      onClose={handleClose}
      closeAfterTransition
      BackdropComponent={Backdrop}
      BackdropProps={{
        timeout: 500,
        ...props.BackdropProps,
      }}
    >
      {transition === 'none' ? (
        modalContent
      ) : (
        <TransitionComponent in={open} timeout={300}>
          {modalContent as React.ReactElement}
        </TransitionComponent>
      )}
    </Modal>
  );
});

AccessibleModal.displayName = 'AccessibleModal';

/**
 * Accessible Dialog Component (extends MUI Dialog)
 */
export interface AccessibleDialogProps extends Omit<DialogProps, 'aria-labelledby' | 'aria-describedby'>, AccessibleProps {
  /**
   * Dialog title (required)
   */
  title: string;
  
  /**
   * Dialog description for screen readers
   */
  description?: string;
  
  /**
   * Show close button in title bar
   */
  showCloseButton?: boolean;
  
  /**
   * Close button label
   */
  closeButtonLabel?: string;
  
  /**
   * Custom close handler with reason
   */
  onClose?: (event: any, reason: 'backdropClick' | 'escapeKeyDown' | 'closeButtonClick') => void;
}

export const AccessibleDialog = forwardRef<HTMLDivElement, AccessibleDialogProps>(({
  children,
  title,
  description,
  showCloseButton = false,
  closeButtonLabel = 'Close dialog',
  onClose,
  open,
  ...props
}, ref) => {
  const { announcePolite } = useAnnouncement();
  
  const titleId = React.useId();
  const descriptionId = React.useId();

  // Handle dialog open/close announcements
  React.useEffect(() => {
    if (open) {
      announcePolite(`${title} dialog opened`);
    }
  }, [open, title, announcePolite]);

  const handleClose = (event: any, reason: 'backdropClick' | 'escapeKeyDown') => {
    announcePolite(`${title} dialog closed`);
    onClose?.(event, reason);
  };

  const handleCloseButtonClick = () => {
    handleClose({}, 'closeButtonClick' as any);
  };

  return (
    <Dialog
      {...props}
      ref={ref}
      open={open}
      onClose={handleClose}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      PaperProps={{
        ...props.PaperProps,
        sx: {
          '&:focus': {
            outline: 'none',
          },
          ...props.PaperProps?.sx,
        },
      }}
    >
      {children}
      
      {/* Hidden description for screen readers */}
      {description && (
        <ScreenReaderOnly id={descriptionId}>
          {description}
        </ScreenReaderOnly>
      )}
    </Dialog>
  );
});

AccessibleDialog.displayName = 'AccessibleDialog';

/**
 * Accessible Dialog Title with optional close button
 */
export interface AccessibleDialogTitleProps extends DialogTitleProps {
  /**
   * Show close button
   */
  showCloseButton?: boolean;
  
  /**
   * Close button click handler
   */
  onClose?: () => void;
  
  /**
   * Close button label
   */
  closeButtonLabel?: string;
}

export const AccessibleDialogTitle: React.FC<AccessibleDialogTitleProps> = ({
  children,
  showCloseButton = false,
  onClose,
  closeButtonLabel = 'Close dialog',
  ...props
}) => {
  return (
    <DialogTitle
      {...props}
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        ...props.sx,
      }}
    >
      <Typography variant="h2" component="h1" sx={{ fontSize: '1.25rem', fontWeight: 'bold' }}>
        {children}
      </Typography>
      
      {showCloseButton && onClose && (
        <IconButton
          onClick={onClose}
          aria-label={closeButtonLabel}
          sx={{
            ml: 2,
            '&:focus': {
              outline: '2px solid',
              outlineColor: 'primary.main',
              outlineOffset: '2px',
            },
          }}
        >
          <CloseIcon />
        </IconButton>
      )}
    </DialogTitle>
  );
};

/**
 * Accessible Dialog Content with proper scrolling and focus management
 */
export const AccessibleDialogContent = forwardRef<HTMLDivElement, DialogContentProps>(({
  children,
  ...props
}, ref) => {
  return (
    <DialogContent
      {...props}
      ref={ref}
      sx={{
        '&:focus': {
          outline: 'none',
        },
        ...props.sx,
      }}
    >
      {children}
    </DialogContent>
  );
});

AccessibleDialogContent.displayName = 'AccessibleDialogContent';

/**
 * Accessible Dialog Actions with proper button grouping
 */
export interface AccessibleDialogActionsProps extends DialogActionsProps {
  /**
   * Primary action button element
   */
  primaryAction?: React.ReactElement;
  
  /**
   * Secondary action button element
   */
  secondaryAction?: React.ReactElement;
  
  /**
   * Cancel/close button element
   */
  cancelAction?: React.ReactElement;
}

export const AccessibleDialogActions = forwardRef<HTMLDivElement, AccessibleDialogActionsProps>(({
  children,
  primaryAction,
  secondaryAction,
  cancelAction,
  ...props
}, ref) => {
  // If structured actions are provided, use them
  if (primaryAction || secondaryAction || cancelAction) {
    return (
      <DialogActions
        {...props}
        ref={ref}
        sx={{
          gap: 1,
          ...props.sx,
        }}
      >
        {cancelAction}
        {secondaryAction}
        {primaryAction}
      </DialogActions>
    );
  }

  // Otherwise render children as-is
  return (
    <DialogActions
      {...props}
      ref={ref}
      sx={{
        gap: 1,
        ...props.sx,
      }}
    >
      {children}
    </DialogActions>
  );
});

AccessibleDialogActions.displayName = 'AccessibleDialogActions';

/**
 * Confirmation Dialog with accessibility features
 */
export interface AccessibleConfirmDialogProps {
  /**
   * Dialog open state
   */
  open: boolean;
  
  /**
   * Dialog title
   */
  title: string;
  
  /**
   * Dialog message/content
   */
  message: React.ReactNode;
  
  /**
   * Confirm button text
   */
  confirmText?: string;
  
  /**
   * Cancel button text
   */
  cancelText?: string;
  
  /**
   * Confirm button color/severity
   */
  severity?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  
  /**
   * Confirm handler
   */
  onConfirm: () => void;
  
  /**
   * Cancel handler
   */
  onCancel: () => void;
  
  /**
   * Auto-focus confirm button (dangerous actions should focus cancel)
   */
  autoFocusConfirm?: boolean;
}

export const AccessibleConfirmDialog: React.FC<AccessibleConfirmDialogProps> = ({
  open,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  severity = 'primary',
  onConfirm,
  onCancel,
  autoFocusConfirm = false,
}) => {
  const confirmRef = React.useRef<HTMLButtonElement>(null);
  const cancelRef = React.useRef<HTMLButtonElement>(null);

  // Focus management
  React.useEffect(() => {
    if (open) {
      setTimeout(() => {
        if (autoFocusConfirm) {
          confirmRef.current?.focus();
        } else {
          cancelRef.current?.focus();
        }
      }, 100);
    }
  }, [open, autoFocusConfirm]);

  return (
    <AccessibleDialog
      open={open}
      onClose={(_, reason) => {
        if (reason !== 'backdropClick') {
          onCancel();
        }
      }}
      title={title}
      description="Please confirm your action"
      maxWidth="sm"
    >
      <AccessibleDialogContent>
        <Typography>{message}</Typography>
      </AccessibleDialogContent>
      
      <AccessibleDialogActions>
        <Box
          component="button"
          ref={cancelRef}
          onClick={onCancel}
          sx={{
            px: 2,
            py: 1,
            border: '1px solid',
            borderColor: 'grey.300',
            borderRadius: 1,
            bgcolor: 'background.paper',
            cursor: 'pointer',
            '&:focus': {
              outline: '2px solid',
              outlineColor: 'primary.main',
              outlineOffset: '2px',
            },
          }}
        >
          {cancelText}
        </Box>
        
        <Box
          component="button"
          ref={confirmRef}
          onClick={onConfirm}
          sx={{
            px: 2,
            py: 1,
            border: 'none',
            borderRadius: 1,
            bgcolor: `${severity}.main`,
            color: `${severity}.contrastText`,
            cursor: 'pointer',
            fontWeight: 'medium',
            '&:focus': {
              outline: '2px solid',
              outlineColor: 'primary.main',
              outlineOffset: '2px',
            },
            '&:hover': {
              bgcolor: `${severity}.dark`,
            },
          }}
        >
          {confirmText}
        </Box>
      </AccessibleDialogActions>
    </AccessibleDialog>
  );
};

export default AccessibleModal;