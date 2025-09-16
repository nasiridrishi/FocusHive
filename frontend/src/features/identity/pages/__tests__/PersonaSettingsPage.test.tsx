import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PersonaSettingsPage } from '../PersonaSettingsPage'
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

describe('PersonaSettingsPage', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    })
    vi.clearAllMocks()
    // Default mock to return a current persona
    vi.mocked(identityService.getCurrentPersona).mockResolvedValue({
      id: '1',
      name: 'Work',
      type: 'work',
      isActive: true,
      isDefault: false,
      settings: {}
    })
    vi.mocked(identityService.getPersonas).mockResolvedValue([
      {
        id: '1',
        name: 'Work',
        type: 'work',
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Personal',
        type: 'personal',
        isActive: false,
        isDefault: true,
        settings: {}
      }
    ])
  })

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    </BrowserRouter>
  )

  it('should render the persona settings page', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Identity & Personas/i)).toBeInTheDocument()
    })
  })

  it('should display page title and description', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Identity & Personas/i)).toBeInTheDocument()
      expect(screen.getByText(/Switch between different personas for work, study, or personal contexts/i)).toBeInTheDocument()
    })
  })

  it('should show current persona section', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Current Persona/i)).toBeInTheDocument()
      // Use getAllByText since "Work" appears multiple times
      const workElements = screen.getAllByText('Work')
      expect(workElements.length).toBeGreaterThan(0)
    })
  })

  it('should render PersonaSwitcher component', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByLabelText('Switch persona')).toBeInTheDocument()
    })
  })

  it('should show manage personas link', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      const manageLink = screen.getByRole('link', { name: /Manage Personas/i })
      expect(manageLink).toBeInTheDocument()
      expect(manageLink).toHaveAttribute('href', '/settings/personas/manage')
    })
  })

  it('should show quick actions section', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Quick Actions/i)).toBeInTheDocument()
      // Link rendered as button - use getByText instead
      expect(screen.getByText(/Create New Persona/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Set Default Persona/i })).toBeInTheDocument()
    })
  })

  it('should show persona statistics', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Persona Statistics/i)).toBeInTheDocument()
      expect(screen.getByText(/Total Personas/i)).toBeInTheDocument()
      expect(screen.getByText('2')).toBeInTheDocument() // Based on mock data
    })
  })

  it('should handle loading state', () => {
    vi.mocked(identityService.getCurrentPersona).mockImplementation(
      () => new Promise(() => {}) // Never resolves to keep loading
    )

    render(<PersonaSettingsPage />, { wrapper })

    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('should handle error state', async () => {
    vi.mocked(identityService.getCurrentPersona).mockRejectedValue(new Error('Failed to load'))

    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Error loading persona settings/i)).toBeInTheDocument()
    })
  })

  it('should have proper page layout structure', async () => {
    render(<PersonaSettingsPage />, { wrapper })

    await waitFor(() => {
      // Check for main container
      const mainContainer = screen.getByRole('main')
      expect(mainContainer).toBeInTheDocument()

      // Check for heading hierarchy - use name to be more specific
      const heading = screen.getByRole('heading', { level: 1, name: /Identity & Personas/i })
      expect(heading).toBeInTheDocument()
    })
  })
})
