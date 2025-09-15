import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '../../../test-utils/test-utils'
import LoadingSpinner from '../LoadingSpinner'

describe('LoadingSpinner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render loading spinner', () => {
      renderWithProviders(<LoadingSpinner />)

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
      expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('should render with default size', () => {
      renderWithProviders(<LoadingSpinner />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveStyle({ width: '40px', height: '40px' })
    })

    it('should render with custom size', () => {
      renderWithProviders(<LoadingSpinner size={60} />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveStyle({ width: '60px', height: '60px' })
    })

    it('should render with custom color', () => {
      renderWithProviders(<LoadingSpinner color="secondary" />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveClass('MuiCircularProgress-colorSecondary')
    })

    it('should render with message', () => {
      renderWithProviders(<LoadingSpinner message="Loading data..." />)

      expect(screen.getByText('Loading data...')).toBeInTheDocument()
    })

    it('should render with backdrop', () => {
      renderWithProviders(<LoadingSpinner withBackdrop />)

      expect(screen.getByTestId('loading-backdrop')).toBeInTheDocument()
    })

    it('should render fullscreen when specified', () => {
      renderWithProviders(<LoadingSpinner fullscreen />)

      const container = screen.getByTestId('loading-spinner-container')
      expect(container).toHaveStyle({
        position: 'fixed',
        top: '0',
        left: '0',
        right: '0',
        bottom: '0',
      })
    })

    it('should center spinner by default', () => {
      renderWithProviders(<LoadingSpinner />)

      const container = screen.getByTestId('loading-spinner-container')
      expect(container).toHaveStyle({
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
      })
    })
  })

  describe('Variants', () => {
    it('should render circular progress by default', () => {
      renderWithProviders(<LoadingSpinner />)

      expect(screen.getByRole('progressbar')).toHaveClass('MuiCircularProgress-root')
    })

    it('should render linear progress when specified', () => {
      renderWithProviders(<LoadingSpinner variant="linear" />)

      expect(screen.getByRole('progressbar')).toHaveClass('MuiLinearProgress-root')
    })

    it('should render skeleton when specified', () => {
      renderWithProviders(<LoadingSpinner variant="skeleton" />)

      expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument()
    })

    it('should render pulse animation when specified', () => {
      renderWithProviders(<LoadingSpinner variant="pulse" />)

      const pulse = screen.getByTestId('loading-pulse')
      expect(pulse).toBeInTheDocument()
      expect(pulse).toHaveClass('pulse-animation')
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      renderWithProviders(<LoadingSpinner />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveAttribute('aria-busy', 'true')
    })

    it('should have accessible label when message provided', () => {
      renderWithProviders(<LoadingSpinner message="Loading content" />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveAttribute('aria-label', 'Loading content')
    })

    it('should have default accessible label', () => {
      renderWithProviders(<LoadingSpinner />)

      const spinner = screen.getByRole('progressbar')
      expect(spinner).toHaveAttribute('aria-label', 'Loading')
    })

    it('should announce loading state to screen readers', () => {
      renderWithProviders(<LoadingSpinner message="Please wait" />)

      const message = screen.getByText('Please wait')
      expect(message).toHaveAttribute('aria-live', 'polite')
    })
  })

  describe('Conditional Rendering', () => {
    it('should render when isLoading is true', () => {
      renderWithProviders(<LoadingSpinner isLoading={true} />)

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
    })

    it('should not render when isLoading is false', () => {
      renderWithProviders(<LoadingSpinner isLoading={false} />)

      expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument()
    })

    it('should render children when not loading', () => {
      renderWithProviders(
        <LoadingSpinner isLoading={false}>
          <div data-testid="child-content">Content</div>
        </LoadingSpinner>
      )

      expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument()
      expect(screen.getByTestId('child-content')).toBeInTheDocument()
    })

    it('should hide children when loading', () => {
      renderWithProviders(
        <LoadingSpinner isLoading={true}>
          <div data-testid="child-content">Content</div>
        </LoadingSpinner>
      )

      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
      expect(screen.queryByTestId('child-content')).not.toBeInTheDocument()
    })
  })

  describe('Delay', () => {
    it('should respect delay before showing spinner', async () => {
      vi.useFakeTimers()

      renderWithProviders(<LoadingSpinner delay={500} />)

      // Initially should not be visible
      expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument()

      // After delay, should be visible
      vi.advanceTimersByTime(500)
      await vi.waitFor(() => {
        expect(screen.getByTestId('loading-spinner')).toBeInTheDocument()
      })

      vi.useRealTimers()
    })
  })
})