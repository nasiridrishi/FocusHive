import React from 'react'
import {fireEvent, render, screen, waitFor, RenderResult} from '@testing-library/react'
import {createTheme, ThemeProvider} from '@mui/material/styles'
import {PermissionErrorFallback} from './fallbacks/PermissionErrorFallback'
import {beforeEach, describe, expect, it, vi} from 'vitest'

const theme = createTheme()

const renderWithTheme = (component: React.ReactElement): RenderResult => {
  return render(
      <ThemeProvider theme={theme}>
        {component}
      </ThemeProvider>
  )
}

const defaultProps = {
  error: new Error('Access denied'),
  resetErrorBoundary: vi.fn(),
}

describe('PermissionErrorFallback', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Authentication errors', () => {
    it('renders authentication error correctly', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authentication"
          />
      )

      expect(screen.getByText('Authentication Required')).toBeInTheDocument()
      expect(screen.getByText('You need to sign in to access this feature.')).toBeInTheDocument()
      expect(screen.getByText('Sign In')).toBeInTheDocument()
    })

    it('handles login button click', async () => {
      const onLogin = vi.fn()

      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authentication"
              onLogin={onLogin}
          />
      )

      const loginButton = screen.getByText('Sign In')
      fireEvent.click(loginButton)

      expect(screen.getByText('Signing In...')).toBeInTheDocument()
      await waitFor(() => {
        expect(onLogin).toHaveBeenCalled()
      })
    })

    it('redirects to login page when no onLogin handler provided', () => {
      const originalLocation = window.location
      Object.defineProperty(window, 'location', {
        value: {...originalLocation, href: ''},
        writable: true,
      })

      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authentication"
          />
      )

      const loginButton = screen.getByText('Sign In')
      fireEvent.click(loginButton)

      expect(window.location.href).toBe('/login')

      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
      })
    })
  })

  describe('Authorization errors', () => {
    it('renders authorization error correctly', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
              requiredPermission="admin:read"
              userRole="user"
          />
      )

      expect(screen.getByText('Access Denied')).toBeInTheDocument()
      expect(screen.getByText('You do not have permission to access this resource.')).toBeInTheDocument()
      expect(screen.getByText('Required: admin:read')).toBeInTheDocument()
      expect(screen.getByText('Current Role: user')).toBeInTheDocument()
    })

    it('shows contact support button for authorization errors', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
          />
      )

      expect(screen.getByText('Contact Support')).toBeInTheDocument()
    })

    it('handles contact support button click', () => {
      const onContactSupport = vi.fn()

      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
              onContactSupport={onContactSupport}
          />
      )

      const supportButton = screen.getByText('Contact Support')
      fireEvent.click(supportButton)

      expect(onContactSupport).toHaveBeenCalled()
    })
  })

  describe('Forbidden errors', () => {
    it('renders forbidden error correctly', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="forbidden"
          />
      )

      expect(screen.getByText('Forbidden')).toBeInTheDocument()
      expect(screen.getByText('This action is restricted for your account level.')).toBeInTheDocument()
    })
  })

  describe('Expired session errors', () => {
    it('renders expired session error correctly', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="expired"
          />
      )

      expect(screen.getByText('Session Expired')).toBeInTheDocument()
      expect(screen.getByText('Your session has expired. Please sign in again.')).toBeInTheDocument()
      expect(screen.getByText('Sign In')).toBeInTheDocument()
    })
  })

  describe('Common functionality', () => {
    it('handles retry button click', () => {
      const resetErrorBoundary = vi.fn()

      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              resetErrorBoundary={resetErrorBoundary}
          />
      )

      const retryButton = screen.getByText('Retry')
      fireEvent.click(retryButton)

      expect(resetErrorBoundary).toHaveBeenCalled()
    })

    it('handles go home button click', () => {
      const originalLocation = window.location
      Object.defineProperty(window, 'location', {
        value: {...originalLocation, href: ''},
        writable: true,
      })

      renderWithTheme(<PermissionErrorFallback {...defaultProps} />)

      const homeButton = screen.getByText('Go Home')
      fireEvent.click(homeButton)

      expect(window.location.href).toBe('/')

      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
      })
    })

    it('displays error type chip', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
          />
      )

      expect(screen.getByText('Type: Authorization')).toBeInTheDocument()
    })

    it('displays error message', () => {
      renderWithTheme(<PermissionErrorFallback {...defaultProps} />)

      expect(screen.getByText('Access denied')).toBeInTheDocument()
    })

    it('shows recommended actions', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authentication"
          />
      )

      expect(screen.getByText('Recommended Actions:')).toBeInTheDocument()
      expect(screen.getByText('Sign in with your account credentials')).toBeInTheDocument()
    })

    it('respects showLoginButton prop', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authentication"
              showLoginButton={false}
          />
      )

      expect(screen.queryByText('Sign In')).not.toBeInTheDocument()
    })

    it('respects showRefreshButton prop', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              showRefreshButton={false}
          />
      )

      expect(screen.queryByText('Retry')).not.toBeInTheDocument()
    })

    it('respects showHomeButton prop', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              showHomeButton={false}
          />
      )

      expect(screen.queryByText('Go Home')).not.toBeInTheDocument()
    })

    it('respects showSupportButton prop', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
              showSupportButton={false}
          />
      )

      expect(screen.queryByText('Contact Support')).not.toBeInTheDocument()
    })
  })

  describe('Custom content', () => {
    it('displays custom title and subtitle', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              title="Custom Permission Error"
              subtitle="Custom error description"
          />
      )

      expect(screen.getByText('Custom Permission Error')).toBeInTheDocument()
      expect(screen.getByText('Custom error description')).toBeInTheDocument()
    })

    it('shows required permission information', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
              requiredPermission="admin:write"
          />
      )

      expect(screen.getByText((content, node) => {
        const hasText = (node: Element | null) => node?.textContent === 'This action requires the admin:write permission.'
        const nodeHasText = hasText(node as Element)
        const childrenDontHaveText = node ? Array.from(node.children).every(child => !hasText(child)) : true
        return nodeHasText && childrenDontHaveText
      })).toBeInTheDocument()
    })

    it('displays additional help for authorization/forbidden errors', () => {
      renderWithTheme(
          <PermissionErrorFallback
              {...defaultProps}
              errorType="authorization"
          />
      )

      expect(screen.getByText(/If you believe this is an error/)).toBeInTheDocument()
    })
  })
})