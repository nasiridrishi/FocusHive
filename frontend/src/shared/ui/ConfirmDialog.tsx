import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  Button,
  Box,
  CircularProgress,
  type Breakpoint,
} from '@mui/material'

interface ConfirmDialogProps {
  open: boolean
  title: string
  message: string
  onConfirm: () => void | Promise<void>
  onCancel: () => void
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'info' | 'success'
  loading?: boolean
  content?: React.ReactNode
  icon?: React.ReactNode
  size?: 'small' | 'medium' | 'large'
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  title,
  message,
  onConfirm,
  onCancel,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant,
  loading = false,
  content,
  icon,
  size = 'small',
}) => {
  // Map size to maxWidth
  const getMaxWidth = (): false | Breakpoint => {
    switch (size) {
      case 'small':
        return 'sm' as Breakpoint
      case 'large':
        return 'md' as Breakpoint
      case 'medium':
      default:
        return 'sm' as Breakpoint
    }
  }

  // Map variant to button color
  const getButtonColor = () => {
    switch (variant) {
      case 'danger':
        return 'error'
      case 'warning':
        return 'warning'
      case 'info':
        return 'info'
      case 'success':
        return 'success'
      default:
        return 'primary'
    }
  }

  const handleConfirm = async () => {
    if (!loading) {
      await onConfirm()
    }
  }

  const handleCancel = () => {
    if (!loading) {
      onCancel()
    }
  }

  const handleClose = (event: any, reason: string) => {
    if (reason === 'escapeKeyDown' || reason === 'backdropClick') {
      handleCancel()
    }
  }

  // Auto-focus confirm button when dialog opens
  const confirmButtonRef = React.useRef<HTMLButtonElement>(null)

  const handleEntered = () => {
    // Focus the confirm button when dialog animation completes
    confirmButtonRef.current?.focus()
  }

  return (
    <Dialog
      data-testid="confirm-dialog"
      open={open}
      onClose={handleClose}
      maxWidth={getMaxWidth()}
      fullWidth
      aria-labelledby="confirm-dialog-title"
      aria-describedby="confirm-dialog-description"
      TransitionProps={{
        onEntered: handleEntered,
      }}
    >
      <DialogTitle id="confirm-dialog-title">
        {icon && (
          <Box sx={{ display: 'inline-block', mr: 1 }}>
            {icon}
          </Box>
        )}
        {title}
      </DialogTitle>
      <DialogContent>
        {content ? (
          content
        ) : (
          <DialogContentText id="confirm-dialog-description">
            {message}
          </DialogContentText>
        )}
      </DialogContent>
      <DialogActions>
        <Button
          data-testid="cancel-button"
          onClick={handleCancel}
          disabled={loading}
        >
          {cancelText}
        </Button>
        <Button
          ref={confirmButtonRef}
          data-testid="confirm-button"
          onClick={handleConfirm}
          disabled={loading}
          variant="contained"
          color={getButtonColor()}
          startIcon={loading ? <CircularProgress size={16} /> : undefined}
        >
          {confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default ConfirmDialog