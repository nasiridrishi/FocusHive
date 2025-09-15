import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test-utils/test-utils'
import ConfirmDialog from '../ConfirmDialog'

describe('ConfirmDialog', () => {
  const defaultProps = {
    open: true,
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?',
    onConfirm: vi.fn(),
    onCancel: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render dialog when open is true', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      expect(screen.getByTestId('confirm-dialog')).toBeInTheDocument()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('should not render dialog when open is false', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} open={false} />)

      expect(screen.queryByTestId('confirm-dialog')).not.toBeInTheDocument()
    })

    it('should display title', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      expect(screen.getByText('Confirm Action')).toBeInTheDocument()
    })

    it('should display message', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      expect(screen.getByText('Are you sure you want to proceed?')).toBeInTheDocument()
    })

    it('should display confirm and cancel buttons', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      expect(screen.getByTestId('confirm-button')).toBeInTheDocument()
      expect(screen.getByTestId('cancel-button')).toBeInTheDocument()
    })

    it('should display custom button labels', () => {
      renderWithProviders(
        <ConfirmDialog
          {...defaultProps}
          confirmText="Delete"
          cancelText="Keep"
        />
      )

      expect(screen.getByText('Delete')).toBeInTheDocument()
      expect(screen.getByText('Keep')).toBeInTheDocument()
    })
  })

  describe('Interactions', () => {
    it('should call onConfirm when confirm button is clicked', async () => {
      const user = userEvent.setup()
      const onConfirm = vi.fn()

      renderWithProviders(
        <ConfirmDialog {...defaultProps} onConfirm={onConfirm} />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      await user.click(confirmButton)

      expect(onConfirm).toHaveBeenCalledTimes(1)
    })

    it('should call onCancel when cancel button is clicked', async () => {
      const user = userEvent.setup()
      const onCancel = vi.fn()

      renderWithProviders(
        <ConfirmDialog {...defaultProps} onCancel={onCancel} />
      )

      const cancelButton = screen.getByTestId('cancel-button')
      await user.click(cancelButton)

      expect(onCancel).toHaveBeenCalledTimes(1)
    })

    it('should call onCancel when escape key is pressed', async () => {
      const user = userEvent.setup()
      const onCancel = vi.fn()

      renderWithProviders(
        <ConfirmDialog {...defaultProps} onCancel={onCancel} />
      )

      await user.keyboard('{Escape}')

      expect(onCancel).toHaveBeenCalledTimes(1)
    })

    it('should call onCancel when clicking outside dialog', async () => {
      const user = userEvent.setup()
      const onCancel = vi.fn()

      renderWithProviders(
        <ConfirmDialog {...defaultProps} onCancel={onCancel} />
      )

      // Click on backdrop
      const backdrop = document.querySelector('.MuiBackdrop-root')
      if (backdrop) {
        await user.click(backdrop)
      }

      expect(onCancel).toHaveBeenCalledTimes(1)
    })
  })

  describe('Variants', () => {
    it('should display danger variant with error color', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} variant="danger" />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      expect(confirmButton).toHaveClass('MuiButton-containedError')
    })

    it('should display warning variant with warning color', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} variant="warning" />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      expect(confirmButton).toHaveClass('MuiButton-containedWarning')
    })

    it('should display info variant with info color', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} variant="info" />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      expect(confirmButton).toHaveClass('MuiButton-containedInfo')
    })

    it('should display success variant with success color', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} variant="success" />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      expect(confirmButton).toHaveClass('MuiButton-containedSuccess')
    })
  })

  describe('Loading State', () => {
    it('should show loading state on confirm button', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} loading={true} />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      expect(confirmButton).toBeDisabled()
      expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('should disable cancel button when loading', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} loading={true} />
      )

      const cancelButton = screen.getByTestId('cancel-button')
      expect(cancelButton).toBeDisabled()
    })

    it('should not call callbacks when loading', async () => {
      const onConfirm = vi.fn()
      const onCancel = vi.fn()

      renderWithProviders(
        <ConfirmDialog
          {...defaultProps}
          onConfirm={onConfirm}
          onCancel={onCancel}
          loading={true}
        />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      const cancelButton = screen.getByTestId('cancel-button')

      // Buttons should be disabled when loading
      expect(confirmButton).toBeDisabled()
      expect(cancelButton).toBeDisabled()

      // Verify callbacks weren't called (buttons are disabled, so they can't be clicked)
      expect(onConfirm).not.toHaveBeenCalled()
      expect(onCancel).not.toHaveBeenCalled()
    })
  })

  describe('Content', () => {
    it('should render custom content when provided', () => {
      renderWithProviders(
        <ConfirmDialog
          {...defaultProps}
          content={<div data-testid="custom-content">Custom content here</div>}
        />
      )

      expect(screen.getByTestId('custom-content')).toBeInTheDocument()
      expect(screen.getByText('Custom content here')).toBeInTheDocument()
    })

    it('should render icon when provided', () => {
      renderWithProviders(
        <ConfirmDialog
          {...defaultProps}
          icon={<div data-testid="custom-icon">⚠️</div>}
        />
      )

      expect(screen.getByTestId('custom-icon')).toBeInTheDocument()
    })
  })

  describe('Size', () => {
    it('should render with small size', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} size="small" />
      )

      const dialog = screen.getByRole('dialog')
      expect(dialog).toHaveClass('MuiDialog-paperWidthSm')
    })

    it('should render with medium size by default', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      const dialog = screen.getByRole('dialog')
      expect(dialog).toHaveClass('MuiDialog-paperWidthSm')
    })

    it('should render with large size', () => {
      renderWithProviders(
        <ConfirmDialog {...defaultProps} size="large" />
      )

      const dialog = screen.getByRole('dialog')
      expect(dialog).toHaveClass('MuiDialog-paperWidthMd')
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      const dialog = screen.getByRole('dialog')
      expect(dialog).toHaveAttribute('aria-labelledby')
      expect(dialog).toHaveAttribute('aria-describedby')
    })

    it('should focus confirm button by default', async () => {
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      await waitFor(() => {
        const confirmButton = screen.getByTestId('confirm-button')
        expect(document.activeElement).toBe(confirmButton)
      })
    })

    it('should trap focus within dialog', async () => {
      const user = userEvent.setup()
      renderWithProviders(<ConfirmDialog {...defaultProps} />)

      // Tab through elements
      await user.tab()
      expect(document.activeElement).toBe(screen.getByTestId('cancel-button'))

      await user.tab()
      expect(document.activeElement).toBe(screen.getByTestId('confirm-button'))

      // Should cycle back
      await user.tab()
      expect(document.activeElement).toBe(screen.getByTestId('cancel-button'))
    })
  })

  describe('Async Operations', () => {
    it('should handle async onConfirm', async () => {
      const onConfirm = vi.fn(async () => {
        await new Promise(resolve => setTimeout(resolve, 100))
      })

      renderWithProviders(
        <ConfirmDialog {...defaultProps} onConfirm={onConfirm} />
      )

      const confirmButton = screen.getByTestId('confirm-button')
      const user = userEvent.setup()
      await user.click(confirmButton)

      await waitFor(() => {
        expect(onConfirm).toHaveBeenCalled()
      })
    })
  })
})