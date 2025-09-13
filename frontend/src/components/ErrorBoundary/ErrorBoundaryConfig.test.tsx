import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { createTheme } from '@mui/material/styles'
import {
  ErrorBoundaryConfig,
  APIErrorBoundary,
  AuthErrorBoundary,
  PermissionErrorBoundary,
  FeatureErrorBoundary,
  ComponentErrorBoundary,
  withErrorBoundary,
} from './ErrorBoundaryConfig'
import { vi, beforeEach, describe, it, expect } from 'vitest'

const theme = createTheme()

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <ThemeProvider theme={theme}>
      {component}
    </ThemeProvider>
  )
}

// Test component that throws errors
const ThrowError: React.FC<{ shouldThrow?: boolean; errorMessage?: string }> = ({
  shouldThrow = false,
  errorMessage = 'Test error',
}) => {
  if (shouldThrow) {
    throw new Error(errorMessage)
  }
  return <div data-testid="working-component">Component is working</div>
}

describe('ErrorBoundaryConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Suppress console.error for error boundary tests
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders children when no error occurs', () => {
    renderWithTheme(
      <ErrorBoundaryConfig>
        <ThrowError />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByTestId('working-component')).toBeInTheDocument()
  })

  it('shows default error fallback when error occurs', () => {
    renderWithTheme(
      <ErrorBoundaryConfig>
        <ThrowError shouldThrow={true} />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('shows network error fallback for network error type', () => {
    renderWithTheme(
      <ErrorBoundaryConfig errorType="network" endpoint="/api/test">
        <ThrowError shouldThrow={true} errorMessage="Network error" />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
    expect(screen.getByText('GET /api/test')).toBeInTheDocument()
  })

  it('shows permission error fallback for permission error type', () => {
    renderWithTheme(
      <ErrorBoundaryConfig 
        errorType="permission" 
        requiredPermission="admin:read"
        userRole="user"
      >
        <ThrowError shouldThrow={true} errorMessage="Access denied" />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
    expect(screen.getByText('Required: admin:read')).toBeInTheDocument()
    expect(screen.getByText('Current Role: user')).toBeInTheDocument()
  })

  it('shows authentication error fallback for authentication error type', () => {
    renderWithTheme(
      <ErrorBoundaryConfig errorType="authentication">
        <ThrowError shouldThrow={true} errorMessage="Authentication required" />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Authentication Required')).toBeInTheDocument()
    expect(screen.getByText('Sign In')).toBeInTheDocument()
  })

  it('shows authorization error fallback for authorization error type', () => {
    renderWithTheme(
      <ErrorBoundaryConfig 
        errorType="authorization"
        requiredPermission="admin:write"
      >
        <ThrowError shouldThrow={true} errorMessage="Authorization failed" />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
    expect(screen.getByText('Required: admin:write')).toBeInTheDocument()
  })

  it('uses custom fallback when provided', () => {
    const CustomFallback = () => <div>Custom error fallback</div>
    
    renderWithTheme(
      <ErrorBoundaryConfig customFallback={CustomFallback}>
        <ThrowError shouldThrow={true} />
      </ErrorBoundaryConfig>
    )
    
    expect(screen.getByText('Custom error fallback')).toBeInTheDocument()
  })

  it('passes through error boundary props', () => {
    const onError = vi.fn()
    
    renderWithTheme(
      <ErrorBoundaryConfig 
        level="feature" 
        name="TestBoundary"
        onError={onError}
      >
        <ThrowError shouldThrow={true} />
      </ErrorBoundaryConfig>
    )
    
    expect(onError).toHaveBeenCalledWith(
      expect.any(Error),
      expect.objectContaining({ componentStack: expect.any(String) })
    )
  })
})

describe('APIErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders API-specific error boundary', () => {
    renderWithTheme(
      <APIErrorBoundary endpoint="/api/users" method="POST">
        <ThrowError shouldThrow={true} errorMessage="API error" />
      </APIErrorBoundary>
    )
    
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
    expect(screen.getByText('POST /api/users')).toBeInTheDocument()
  })

  it('renders children when no error occurs', () => {
    renderWithTheme(
      <APIErrorBoundary>
        <ThrowError />
      </APIErrorBoundary>
    )
    
    expect(screen.getByTestId('working-component')).toBeInTheDocument()
  })
})

describe('AuthErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders authentication error boundary', () => {
    renderWithTheme(
      <AuthErrorBoundary>
        <ThrowError shouldThrow={true} errorMessage="Auth error" />
      </AuthErrorBoundary>
    )
    
    expect(screen.getByText('Authentication Required')).toBeInTheDocument()
    expect(screen.getByText('Sign In')).toBeInTheDocument()
  })

  it('calls onLogin when provided', () => {
    const onLogin = vi.fn()
    
    renderWithTheme(
      <AuthErrorBoundary onLogin={onLogin}>
        <ThrowError shouldThrow={true} />
      </AuthErrorBoundary>
    )
    
    const loginButton = screen.getByText('Sign In')
    fireEvent.click(loginButton)
    
    expect(onLogin).toHaveBeenCalled()
  })
})

describe('PermissionErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders permission error boundary', () => {
    renderWithTheme(
      <PermissionErrorBoundary 
        requiredPermission="admin:delete"
        userRole="editor"
      >
        <ThrowError shouldThrow={true} errorMessage="Permission denied" />
      </PermissionErrorBoundary>
    )
    
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
    expect(screen.getByText('Required: admin:delete')).toBeInTheDocument()
    expect(screen.getByText('Current Role: editor')).toBeInTheDocument()
  })

  it('calls onContactSupport when provided', () => {
    const onContactSupport = vi.fn()
    
    renderWithTheme(
      <PermissionErrorBoundary 
        requiredPermission="admin:read"
        onContactSupport={onContactSupport}
      >
        <ThrowError shouldThrow={true} />
      </PermissionErrorBoundary>
    )
    
    const supportButton = screen.getByText('Contact Support')
    fireEvent.click(supportButton)
    
    expect(onContactSupport).toHaveBeenCalled()
  })
})

describe('FeatureErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders feature error boundary with default fallback', () => {
    renderWithTheme(
      <FeatureErrorBoundary featureName="UserDashboard">
        <ThrowError shouldThrow={true} errorMessage="Feature error" />
      </FeatureErrorBoundary>
    )
    
    expect(screen.getByText('Feature Unavailable')).toBeInTheDocument()
  })

  it('renders feature error boundary with network fallback', () => {
    renderWithTheme(
      <FeatureErrorBoundary featureName="UserDashboard" fallbackType="network">
        <ThrowError shouldThrow={true} errorMessage="Network feature error" />
      </FeatureErrorBoundary>
    )
    
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
  })

  it('renders feature error boundary with permission fallback', () => {
    renderWithTheme(
      <FeatureErrorBoundary featureName="AdminPanel" fallbackType="permission">
        <ThrowError shouldThrow={true} errorMessage="Permission feature error" />
      </FeatureErrorBoundary>
    )
    
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
  })
})

describe('ComponentErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('renders component error boundary', () => {
    renderWithTheme(
      <ComponentErrorBoundary componentName="UserCard">
        <ThrowError shouldThrow={true} errorMessage="Component error" />
      </ComponentErrorBoundary>
    )
    
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('renders children when no error occurs', () => {
    renderWithTheme(
      <ComponentErrorBoundary componentName="UserCard">
        <ThrowError />
      </ComponentErrorBoundary>
    )
    
    expect(screen.getByTestId('working-component')).toBeInTheDocument()
  })
})

describe('withErrorBoundary HOC', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  it('wraps component with error boundary', () => {
    const TestComponent = withErrorBoundary(ThrowError)
    
    renderWithTheme(<TestComponent />)
    
    expect(screen.getByTestId('working-component')).toBeInTheDocument()
  })

  it('shows error fallback when wrapped component throws', () => {
    const TestComponent = withErrorBoundary(ThrowError)
    
    renderWithTheme(<TestComponent shouldThrow={true} />)
    
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('uses custom error boundary props', () => {
    const TestComponent = withErrorBoundary(ThrowError, {
      errorType: 'network',
      endpoint: '/api/test'
    })
    
    renderWithTheme(<TestComponent shouldThrow={true} />)
    
    expect(screen.getByText('Network Connection Error')).toBeInTheDocument()
    expect(screen.getByText('GET /api/test')).toBeInTheDocument()
  })

  it('preserves component display name', () => {
    const TestComponent = () => <div>Test</div>
    TestComponent.displayName = 'TestComponent'
    
    const WrappedComponent = withErrorBoundary(TestComponent)
    
    expect(WrappedComponent.displayName).toBe('withErrorBoundary(TestComponent)')
  })

  it('handles components without display name', () => {
    const TestComponent = () => <div>Test</div>
    
    const WrappedComponent = withErrorBoundary(TestComponent)
    
    expect(WrappedComponent.displayName).toBe('withErrorBoundary(TestComponent)')
  })
})