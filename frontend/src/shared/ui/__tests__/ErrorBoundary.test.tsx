import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test-utils/test-utils'
import ErrorBoundary from '../ErrorBoundary'

// Test component that throws an error
const ThrowError: React.FC<{ shouldThrow?: boolean }> = ({ shouldThrow = false }) => {
  if (shouldThrow) {
    throw new Error('Test error')
  }
  return <div data-testid="child-content">Child content</div>
}

describe('ErrorBoundary', () => {
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    // Suppress console.error for error boundary tests
    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
  })

  describe('Normal Rendering', () => {
    it('should render children when there is no error', () => {
      renderWithProviders(
        <ErrorBoundary>
          <div data-testid="child-content">Child content</div>
        </ErrorBoundary>
      )

      expect(screen.getByTestId('child-content')).toBeInTheDocument()
      expect(screen.queryByTestId('error-boundary-fallback')).not.toBeInTheDocument()
    })

    it('should render multiple children without error', () => {
      renderWithProviders(
        <ErrorBoundary>
          <div data-testid="child-1">Child 1</div>
          <div data-testid="child-2">Child 2</div>
        </ErrorBoundary>
      )

      expect(screen.getByTestId('child-1')).toBeInTheDocument()
      expect(screen.getByTestId('child-2')).toBeInTheDocument()
    })
  })

  describe('Error Handling', () => {
    it('should catch errors and display fallback UI', () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByTestId('error-boundary-fallback')).toBeInTheDocument()
      expect(screen.queryByTestId('child-content')).not.toBeInTheDocument()
    })

    it('should display error message in fallback', () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByText(/Something went wrong/i)).toBeInTheDocument()
    })

    it('should display custom fallback when provided', () => {
      const CustomFallback = () => <div data-testid="custom-fallback">Custom error UI</div>

      renderWithProviders(
        <ErrorBoundary fallback={<CustomFallback />}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByTestId('custom-fallback')).toBeInTheDocument()
      expect(screen.queryByTestId('error-boundary-fallback')).not.toBeInTheDocument()
    })

    it('should call onError callback when error occurs', () => {
      const onError = vi.fn()

      renderWithProviders(
        <ErrorBoundary onError={onError}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(onError).toHaveBeenCalledWith(
        expect.any(Error),
        expect.objectContaining({
          componentStack: expect.any(String)
        })
      )
    })
  })

  describe('Error Details', () => {
    it('should show error details when showDetails is true', () => {
      renderWithProviders(
        <ErrorBoundary showDetails={true}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByTestId('error-details')).toBeInTheDocument()
      expect(screen.getByText(/Test error/)).toBeInTheDocument()
    })

    it('should not show error details when showDetails is false', () => {
      renderWithProviders(
        <ErrorBoundary showDetails={false}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.queryByTestId('error-details')).not.toBeInTheDocument()
    })

    it('should show stack trace when enabled', () => {
      renderWithProviders(
        <ErrorBoundary showDetails={true} showStackTrace={true}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByTestId('error-stack-trace')).toBeInTheDocument()
    })
  })

  describe('Reset Functionality', () => {
    it('should provide reset button in fallback UI', () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByTestId('reset-error-button')).toBeInTheDocument()
      expect(screen.getByText(/Try again/i)).toBeInTheDocument()
    })

    it('should reset error state when reset button is clicked', async () => {
      const user = userEvent.setup()
      let shouldThrow = true

      const TestComponent = () => <ThrowError shouldThrow={shouldThrow} />

      const { rerender } = renderWithProviders(
        <ErrorBoundary>
          <TestComponent />
        </ErrorBoundary>
      )

      // Error is shown
      expect(screen.getByTestId('error-boundary-fallback')).toBeInTheDocument()

      // Fix the error condition
      shouldThrow = false

      // Click reset
      const resetButton = screen.getByTestId('reset-error-button')
      await user.click(resetButton)

      // Rerender with fixed condition
      rerender(
        <ErrorBoundary>
          <TestComponent />
        </ErrorBoundary>
      )

      // Children should be rendered again
      await waitFor(() => {
        expect(screen.queryByTestId('error-boundary-fallback')).not.toBeInTheDocument()
        expect(screen.getByTestId('child-content')).toBeInTheDocument()
      })
    })

    it('should call onReset callback when reset button is clicked', async () => {
      const user = userEvent.setup()
      const onReset = vi.fn()

      renderWithProviders(
        <ErrorBoundary onReset={onReset}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const resetButton = screen.getByTestId('reset-error-button')
      await user.click(resetButton)

      expect(onReset).toHaveBeenCalled()
    })
  })

  describe('Logging', () => {
    it('should log errors to console', () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(consoleErrorSpy).toHaveBeenCalled()
    })

    it('should send error to logging service when configured', () => {
      const logError = vi.fn()

      renderWithProviders(
        <ErrorBoundary logError={logError}>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(logError).toHaveBeenCalledWith(
        expect.any(Error),
        expect.objectContaining({
          componentStack: expect.any(String)
        })
      )
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA attributes on error state', () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const errorContainer = screen.getByTestId('error-boundary-fallback')
      expect(errorContainer).toHaveAttribute('role', 'alert')
      expect(errorContainer).toHaveAttribute('aria-live', 'assertive')
    })

    it('should focus reset button for keyboard navigation', async () => {
      renderWithProviders(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} />
        </ErrorBoundary>
      )

      const resetButton = screen.getByTestId('reset-error-button')
      await waitFor(() => {
        expect(document.activeElement).toBe(resetButton)
      })
    })
  })

  describe('Multiple Error Boundaries', () => {
    it('should isolate errors to nearest error boundary', () => {
      renderWithProviders(
        <ErrorBoundary>
          <div data-testid="outer-content">Outer content</div>
          <ErrorBoundary>
            <ThrowError shouldThrow={true} />
          </ErrorBoundary>
        </ErrorBoundary>
      )

      // Outer content should still be visible
      expect(screen.getByTestId('outer-content')).toBeInTheDocument()
      // Inner error boundary should show fallback
      expect(screen.getByTestId('error-boundary-fallback')).toBeInTheDocument()
    })
  })
})