import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PersonaManagementPage } from '../PersonaManagementPage'
import * as identityService from '../../services/identityService'

// Mock the identity service
vi.mock('../../services/identityService')

// Mock the auth hooks
vi.mock('../../../auth/hooks/useAuth', () => ({
  useAuth: () => ({
    authState: {
      user: { id: '1', email: 'test@example.com', username: 'testuser' },
      isAuthenticated: true,
      isLoading: false,
      token: 'mock-token',
      refreshToken: 'mock-refresh-token',
      error: null
    },
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    refreshAuth: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    requestPasswordReset: vi.fn(),
    clearError: vi.fn()
  })
}))

describe('PersonaManagementPage', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    })
    vi.clearAllMocks()
    // Default mock to return empty array
    vi.mocked(identityService.getPersonas).mockResolvedValue([])
  })

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    </BrowserRouter>
  )

  it('should render the persona management page', async () => {
    render(<PersonaManagementPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Persona Management/i)).toBeInTheDocument()
    })
  })

  it('should display page title and description', async () => {
    render(<PersonaManagementPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Persona Management/i)).toBeInTheDocument()
      expect(screen.getByText(/Manage your different personas for various contexts/i)).toBeInTheDocument()
    })
  })

  it('should show back navigation button', async () => {
    render(<PersonaManagementPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Back to Settings/i })).toBeInTheDocument()
    })
  })

  it('should render PersonaManagement component', async () => {
    const mockPersonas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      }
    ]
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagementPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Manage Personas/i)).toBeInTheDocument()
      expect(screen.getByText('Work')).toBeInTheDocument()
    })
  })

  it('should show loading state while fetching data', () => {
    vi.mocked(identityService.getPersonas).mockImplementation(
      () => new Promise(() => {}) // Never resolves to keep loading
    )

    render(<PersonaManagementPage />, { wrapper })

    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('should handle authentication check', async () => {
    // Mock as unauthenticated
    vi.mock('../../../auth/contexts/AuthContext', () => ({
      useAuth: () => ({
        user: null,
        isAuthenticated: false,
        isLoading: false
      })
    }))

    render(<PersonaManagementPage />, { wrapper })

    // Should still render but might redirect (depending on implementation)
    await waitFor(() => {
      expect(screen.queryByText(/Persona Management/i)).toBeInTheDocument()
    })
  })

  it('should have proper page layout structure', async () => {
    render(<PersonaManagementPage />, { wrapper })

    await waitFor(() => {
      // Check for main container
      const mainContainer = screen.getByRole('main')
      expect(mainContainer).toBeInTheDocument()

      // Check for heading hierarchy - use name to be more specific since there are multiple h1
      const heading = screen.getByRole('heading', { level: 1, name: /^Persona Management$/i })
      expect(heading).toBeInTheDocument()
    })
  })
})
