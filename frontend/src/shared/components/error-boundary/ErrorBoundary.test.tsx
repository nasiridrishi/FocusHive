import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { createLightTheme } from '@shared/theme'
import { AppErrorBoundary, ErrorFallback } from './AppErrorBoundary'
import { errorLogger } from '@shared/services/errorLogging'
import { vi, beforeEach, afterEach, beforeAll, afterAll, describe, it, expect } from 'vitest'

// Mock the error logger
vi.mock('@shared/services/errorLogging', () => ({
  errorLogger: {
    logError: vi.fn(),
    logAsyncError: vi.fn(),
  },
  logErrorBoundaryError: vi.fn(),
}))

// Create a component that throws an error
const ThrowError: React.FC<{ shouldThrow?: boolean; errorMessage?: string }> = ({
  shouldThrow = false,
  errorMessage = 'Test error',
}) => {
  if (shouldThrow) {
    throw new Error(errorMessage)
  }
  return <div data-testid="working-component">Component is working</div>
}

// Async error component for testing useAsyncError
const AsyncErrorComponent: React.FC<{ shouldThrow?: boolean }> = ({
  shouldThrow = false,
}) => {
  const handleClick = () => {
    if (shouldThrow) {
      // Simulate an async error
      setTimeout(() => {
        throw new Error('Async error')
      }, 0)
    }
  }

  return (
    <div>
      <div data-testid="async-component">Async Component</div>
      <button data-testid="trigger-async-error" onClick={handleClick}>
        Trigger Async Error
      </button>
    </div>
  )
}

const renderWithTheme = (component: React.ReactElement) => {
  const theme = createLightTheme()
  return render(<ThemeProvider theme={theme}>{component}</ThemeProvider>)
}

// Suppress console errors during tests
const originalConsoleError = console.error
beforeAll(() => {
  console.error = (...args: any[]) => {
    if (
      typeof args[0] === 'string' &&
      args[0].includes('The above error occurred in the')
    ) {
      return
    }
    originalConsoleError.call(console, ...args)
  }
})

afterAll(() => {
  console.error = originalConsoleError
})

describe('ErrorBoundary Components', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('AppErrorBoundary', () => {
    it('renders children when no error occurs', () => {
      renderWithTheme(
        <AppErrorBoundary>
          <ThrowError shouldThrow={false} />
        </AppErrorBoundary>
      )

      expect(screen.getByTestId('working-component')).toBeInTheDocument()
    })

    it('renders error fallback when component throws', async () => {
      renderWithTheme(
        <AppErrorBoundary>
          <ThrowError shouldThrow={true} errorMessage="Component crashed" />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
        expect(screen.getByText('Something went wrong')).toBeInTheDocument()
      })
    })

    it('logs error when component crashes', async () => {
      const mockLogError = vi.fn()
      const originalLogErrorBoundaryError = await import('@shared/services/errorLogging').then(m => m.logErrorBoundaryError)
      
      vi.doMock('@shared/services/errorLogging', () => ({
        logErrorBoundaryError: mockLogError,
        errorLogger: {
          logError: vi.fn(),
          logAsyncError: vi.fn(),
        },
      }))

      renderWithTheme(
        <AppErrorBoundary>
          <ThrowError shouldThrow={true} errorMessage="Component crashed" />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(mockLogError).toHaveBeenCalled()
      })
    })

    it('shows Try Again button and allows error recovery', async () => {
      const { rerender } = renderWithTheme(
        <AppErrorBoundary>
          <ThrowError shouldThrow={true} />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByText('Try Again')).toBeInTheDocument()
      })

      // Click Try Again button
      fireEvent.click(screen.getByText('Try Again'))

      // Re-render with fixed component
      rerender(
        <ThemeProvider theme={createLightTheme()}>
          <AppErrorBoundary>
            <ThrowError shouldThrow={false} />
          </AppErrorBoundary>
        </ThemeProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('working-component')).toBeInTheDocument()
      })
    })

    it('shows different titles for different error levels', async () => {
      // Test app-level error
      renderWithTheme(
        <AppErrorBoundary level="app">
          <ThrowError shouldThrow={true} />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByText('Application Error')).toBeInTheDocument()
      })
    })

    it('shows error details in development mode', async () => {
      const originalEnv = import.meta.env.DEV
      
      // Mock development environment
      Object.defineProperty(import.meta.env, 'DEV', {
        value: true,
        configurable: true,
      })

      renderWithTheme(
        <AppErrorBoundary>
          <ThrowError shouldThrow={true} errorMessage="Detailed error message" />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByText('Error Details (Development Only)')).toBeInTheDocument()
      })

      Object.defineProperty(import.meta.env, 'DEV', {
        value: originalEnv,
        configurable: true,
      })
    })
  })

  describe('ErrorFallback', () => {
    const mockError = new Error('Test fallback error')
    const mockResetErrorBoundary = vi.fn()

    beforeEach(() => {
      mockResetErrorBoundary.mockClear()
    })

    it('renders basic error information', () => {
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
        />
      )

      expect(screen.getByRole('alert')).toBeInTheDocument()
      expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    })

    it('shows custom title and subtitle', () => {
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          title="Custom Error Title"
          subtitle="Custom error description"
        />
      )

      expect(screen.getByText('Custom Error Title')).toBeInTheDocument()
      expect(screen.getByText('Custom error description')).toBeInTheDocument()
    })

    it('calls resetErrorBoundary when Try Again is clicked', () => {
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
        />
      )

      fireEvent.click(screen.getByText('Try Again'))
      expect(mockResetErrorBoundary).toHaveBeenCalled()
    })

    it('calls custom error reporter when Report Issue is clicked', () => {
      const mockReportError = vi.fn()
      
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          onReportError={mockReportError}
        />
      )

      fireEvent.click(screen.getByText('Report Issue'))
      expect(mockReportError).toHaveBeenCalledWith(mockError)
    })

    it('navigates home when Go Home is clicked', () => {
      const mockGoHome = vi.fn()
      
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          onGoHome={mockGoHome}
        />
      )

      fireEvent.click(screen.getByText('Go Home'))
      expect(mockGoHome).toHaveBeenCalled()
    })

    it('shows error boundary name when provided', () => {
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          errorBoundaryName="TestBoundary"
        />
      )

      expect(screen.getByText('Boundary: TestBoundary')).toBeInTheDocument()
    })

    it('expands error details when clicked', async () => {
      renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          showErrorDetails={true}
        />
      )

      const expandButton = screen.getByText('Error Details (Development Only)')
      fireEvent.click(expandButton)

      await waitFor(() => {
        expect(screen.getByText('Test fallback error')).toBeInTheDocument()
      })
    })

    it('shows different severity colors', () => {
      const { rerender } = renderWithTheme(
        <ErrorFallback
          error={mockError}
          resetErrorBoundary={mockResetErrorBoundary}
          severity="warning"
        />
      )

      const errorIcon = screen.getByTestId('ErrorIcon') || screen.getByRole('alert').querySelector('svg')
      expect(errorIcon).toBeInTheDocument()

      rerender(
        <ThemeProvider theme={createLightTheme()}>
          <ErrorFallback
            error={mockError}
            resetErrorBoundary={mockResetErrorBoundary}
            severity="error"
          />
        </ThemeProvider>
      )

      const newErrorIcon = screen.getByTestId('ErrorIcon') || screen.getByRole('alert').querySelector('svg')
      expect(newErrorIcon).toBeInTheDocument()
    })
  })

  describe('Error Boundary Integration', () => {
    it('handles feature-level errors with isolation', async () => {
      renderWithTheme(
        <div>
          <AppErrorBoundary level="feature" isolate={true} featureName="TestFeature">
            <ThrowError shouldThrow={true} />
          </AppErrorBoundary>
          <div data-testid="other-content">Other content should still work</div>
        </div>
      )

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
        expect(screen.getByTestId('other-content')).toBeInTheDocument()
      })
    })

    it('resets when resetKeys change', async () => {
      let resetKey = 'initial'
      
      const { rerender } = renderWithTheme(
        <AppErrorBoundary resetKeys={[resetKey]}>
          <ThrowError shouldThrow={true} />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
      })

      // Change reset key and re-render with working component
      resetKey = 'changed'
      rerender(
        <ThemeProvider theme={createLightTheme()}>
          <AppErrorBoundary resetKeys={[resetKey]}>
            <ThrowError shouldThrow={false} />
          </AppErrorBoundary>
        </ThemeProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('working-component')).toBeInTheDocument()
      })
    })
  })

  describe('Error Recovery Scenarios', () => {
    it('recovers from temporary API errors', async () => {
      let shouldFail = true
      
      const ConditionalErrorComponent = () => {
        if (shouldFail) {
          throw new Error('API temporarily unavailable')
        }
        return <div data-testid="api-working">API is working</div>
      }

      const { rerender } = renderWithTheme(
        <AppErrorBoundary>
          <ConditionalErrorComponent />
        </AppErrorBoundary>
      )

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument()
      })

      // Simulate API recovery
      shouldFail = false
      
      // Click Try Again
      fireEvent.click(screen.getByText('Try Again'))

      rerender(
        <ThemeProvider theme={createLightTheme()}>
          <AppErrorBoundary>
            <ConditionalErrorComponent />
          </AppErrorBoundary>
        </ThemeProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('api-working')).toBeInTheDocument()
      })
    })
  })
})