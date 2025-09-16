import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PersonaSwitcher } from '../PersonaSwitcher'
import * as identityService from '../../services/identityService'

// Mock the identity service
vi.mock('../../services/identityService')

describe('PersonaSwitcher', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    })
    vi.clearAllMocks()
  })

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )

  it('should render persona switcher button', () => {
    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    expect(button).toBeInTheDocument()
  })

  it('should display current persona name', () => {
    const currentPersona = {
      id: '1',
      name: 'Work',
      type: 'work' as const,
      isActive: true,
      isDefault: false,
      settings: {}
    }

    render(<PersonaSwitcher currentPersona={currentPersona} />, { wrapper })

    expect(screen.getByText('Work')).toBeInTheDocument()
  })

  it('should show persona list when clicked', async () => {
    const mockPersonas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Personal',
        type: 'personal' as const,
        isActive: false,
        isDefault: true,
        settings: {}
      }
    ]

    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      const menuItems = screen.getAllByRole('menuitem')
      expect(menuItems).toHaveLength(2)
      expect(screen.getByRole('menuitem', { name: /Work/i })).toBeInTheDocument()
      expect(screen.getByRole('menuitem', { name: /Personal/i })).toBeInTheDocument()
    })
  })

  it('should switch persona when a different one is selected', async () => {
    const mockPersonas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Personal',
        type: 'personal' as const,
        isActive: false,
        isDefault: true,
        settings: {}
      }
    ]

    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)
    vi.mocked(identityService.switchPersona).mockResolvedValue({
      id: '2',
      name: 'Personal',
      type: 'personal',
      isActive: true,
      isDefault: true,
      settings: {}
    })

    const onSwitch = vi.fn()
    render(<PersonaSwitcher onSwitch={onSwitch} />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      const personalOption = screen.getByText('Personal')
      fireEvent.click(personalOption)
    })

    await waitFor(() => {
      expect(identityService.switchPersona).toHaveBeenCalledWith('2')
      expect(onSwitch).toHaveBeenCalledWith(expect.objectContaining({
        id: '2',
        name: 'Personal'
      }))
    })
  })

  it('should show loading state while fetching personas', async () => {
    vi.mocked(identityService.getPersonas).mockImplementation(
      () => new Promise(() => {}) // Never resolves to keep loading
    )

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })
  })

  it('should handle error when fetching personas fails', async () => {
    const errorMessage = 'Failed to fetch personas'
    vi.mocked(identityService.getPersonas).mockRejectedValue(new Error(errorMessage))

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      expect(screen.getByText(/Error loading personas/i)).toBeInTheDocument()
    })
  })

  it('should display persona type icon', () => {
    const personas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Study',
        type: 'study' as const,
        isActive: false,
        isDefault: false,
        settings: {}
      }
    ]

    vi.mocked(identityService.getPersonas).mockResolvedValue(personas)

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    waitFor(() => {
      // Check for work icon
      expect(screen.getByTestId('work-icon')).toBeInTheDocument()
      // Check for study icon
      expect(screen.getByTestId('study-icon')).toBeInTheDocument()
    })
  })

  it('should indicate the active persona in the list', async () => {
    const mockPersonas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Personal',
        type: 'personal' as const,
        isActive: false,
        isDefault: true,
        settings: {}
      }
    ]

    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      const workItem = screen.getByRole('menuitem', { name: /Work/i })
      expect(workItem).toHaveAttribute('aria-current', 'true')
    })
  })

  it('should close the menu after switching persona', async () => {
    const mockPersonas = [
      {
        id: '1',
        name: 'Work',
        type: 'work' as const,
        isActive: true,
        isDefault: false,
        settings: {}
      },
      {
        id: '2',
        name: 'Personal',
        type: 'personal' as const,
        isActive: false,
        isDefault: true,
        settings: {}
      }
    ]

    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)
    vi.mocked(identityService.switchPersona).mockResolvedValue({
      id: '2',
      name: 'Personal',
      type: 'personal',
      isActive: true,
      isDefault: true,
      settings: {}
    })

    render(<PersonaSwitcher />, { wrapper })

    const button = screen.getByLabelText('Switch persona')
    fireEvent.click(button)

    await waitFor(() => {
      expect(screen.getByText('Personal')).toBeInTheDocument()
    })

    const personalOption = screen.getByText('Personal')
    fireEvent.click(personalOption)

    await waitFor(() => {
      expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    })
  })
})