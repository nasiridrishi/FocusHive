import React from 'react'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { createTheme } from '@mui/material/styles'
import { NetworkErrorFallback } from './fallbacks/NetworkErrorFallback'
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest'

// Mock fetch for network status checks
const mockFetch = vi.fn()
global.fetch = mockFetch

const theme = createTheme()

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      {component}
    </ThemeProvider>
  )
}

const defaultProps = {
  error: new Error('Network connection failed'),
  resetErrorBoundary: vi.fn(),
}

describe('NetworkErrorFallback', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock online status
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders network error information correctly', () => {
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
    expect(screen.getByText('Unable to connect to the server. Please check your internet connection.')).toBeInTheDocument()
    expect(screen.getByText('Network connection failed')).toBeInTheDocument()
  })

  it('displays custom title and subtitle when provided', () => {
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        title="Custom Network Error"
        subtitle="Custom error description"
      />
    )
    
    expect(screen.getByText('Custom Network Error')).toBeInTheDocument()
    expect(screen.getByText('Custom error description')).toBeInTheDocument()
  })

  it('shows network status chip', () => {
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    expect(screen.getByText(/Network:/)).toBeInTheDocument()
  })

  it('displays endpoint information when provided', () => {
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        endpoint="/api/users"
        requestMethod="POST"
      />
    )
    
    expect(screen.getByText('POST /api/users')).toBeInTheDocument()
  })

  it('handles retry button click', async () => {
    const resetErrorBoundary = vi.fn()
    mockFetch.mockResolvedValueOnce(new Response())
    
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        resetErrorBoundary={resetErrorBoundary}
        maxRetries={1}
        retryDelay={100}
      />
    )
    
    const retryButton = screen.getByText('Retry Now')
    fireEvent.click(retryButton)
    
    expect(screen.getByText('Retrying...')).toBeInTheDocument()
    
    await waitFor(() => {
      expect(resetErrorBoundary).toHaveBeenCalled()
    }, { timeout: 2000 })
  })

  it('shows max retries reached when retry limit exceeded', async () => {
    mockFetch.mockRejectedValue(new Error('Fetch failed'))
    
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        maxRetries={1}
        retryDelay={50}
      />
    )
    
    const retryButton = screen.getByText('Retry Now')
    fireEvent.click(retryButton)
    
    await waitFor(() => {
      expect(screen.getByText('Max Retries Reached')).toBeInTheDocument()
    }, { timeout: 2000 })
  })

  it('handles check connection button', async () => {
    mockFetch.mockResolvedValueOnce(new Response())
    
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    const checkButton = screen.getByText('Check Connection')
    fireEvent.click(checkButton)
    
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith('/favicon.ico', expect.any(Object))
    })
  })

  it('handles offline/online events', async () => {
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    // Simulate going offline
    act(() => {
      Object.defineProperty(navigator, 'onLine', { value: false })
      window.dispatchEvent(new Event('offline'))
    })
    
    await waitFor(() => {
      expect(screen.getByText(/Network: Offline/)).toBeInTheDocument()
    })
    
    // Simulate going online
    mockFetch.mockResolvedValueOnce(new Response())
    act(() => {
      Object.defineProperty(navigator, 'onLine', { value: true })
      window.dispatchEvent(new Event('online'))
    })
    
    await waitFor(() => {
      expect(screen.getByText(/Network: Restored/)).toBeInTheDocument()
    })
  })

  it('shows troubleshooting steps', () => {
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    expect(screen.getByText('Troubleshooting Steps:')).toBeInTheDocument()
    expect(screen.getByText('Check your internet connection')).toBeInTheDocument()
    expect(screen.getByText('Try refreshing the page')).toBeInTheDocument()
  })

  it('calls onNetworkRestore when network is restored', async () => {
    const onNetworkRestore = vi.fn()
    mockFetch.mockResolvedValueOnce(new Response())
    
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        onNetworkRestore={onNetworkRestore}
      />
    )
    
    // Simulate network restoration
    act(() => {
      Object.defineProperty(navigator, 'onLine', { value: true })
      window.dispatchEvent(new Event('online'))
    })
    
    await waitFor(() => {
      expect(onNetworkRestore).toHaveBeenCalled()
    })
  })

  it('handles network status checking errors gracefully', async () => {
    mockFetch.mockRejectedValueOnce(new Error('Network check failed'))
    
    renderWithTheme(<NetworkErrorFallback {...defaultProps} />)
    
    const checkButton = screen.getByText('Check Connection')
    fireEvent.click(checkButton)
    
    await waitFor(() => {
      expect(screen.getByText(/Network: Offline/)).toBeInTheDocument()
    })
  })

  it('displays retry progress and countdown', async () => {
    const resetErrorBoundary = vi.fn()
    mockFetch.mockRejectedValueOnce(new Error('Network error'))
    
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        resetErrorBoundary={resetErrorBoundary}
        maxRetries={2}
        retryDelay={500}
      />
    )
    
    const retryButton = screen.getByText('Retry Now')
    fireEvent.click(retryButton)
    
    // Should show countdown
    await waitFor(() => {
      expect(screen.getByText(/Retrying in \d+ seconds.../)).toBeInTheDocument()
    })
  })

  it('respects showRetryButton prop', () => {
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        showRetryButton={false}
      />
    )
    
    expect(screen.queryByText('Retry Now')).not.toBeInTheDocument()
  })

  it('respects showNetworkStatus prop', () => {
    renderWithTheme(
      <NetworkErrorFallback
        {...defaultProps}
        showNetworkStatus={false}
      />
    )
    
    expect(screen.queryByText(/Network:/)).not.toBeInTheDocument()
  })
})