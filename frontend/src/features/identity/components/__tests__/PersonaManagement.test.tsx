import React from 'react'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PersonaManagement } from '../PersonaManagement'
import * as identityService from '../../services/identityService'

// Mock the identity service
vi.mock('../../services/identityService')

describe('PersonaManagement', () => {
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
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )

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
    },
    {
      id: '3',
      name: 'Gaming',
      type: 'gaming' as const,
      isActive: false,
      isDefault: false,
      settings: {}
    }
  ]

  it('should render persona management component', async () => {
    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Manage Personas/i)).toBeInTheDocument()
    })
  })

  it('should display list of personas', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText('Work')).toBeInTheDocument()
      expect(screen.getByText('Personal')).toBeInTheDocument()
      expect(screen.getByText('Gaming')).toBeInTheDocument()
    })
  })

  it('should show add persona button', async () => {
    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Add Persona/i })).toBeInTheDocument()
    })
  })

  it('should open create persona dialog when add button is clicked', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Add Persona/i })).toBeInTheDocument()
    })

    const addButton = screen.getByRole('button', { name: /Add Persona/i })
    fireEvent.click(addButton)

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText(/Create New Persona/i)).toBeInTheDocument()
    })
  })

  it('should create a new persona', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)
    const newPersona = {
      id: '4',
      name: 'Study',
      type: 'study' as const,
      isActive: false,
      isDefault: false,
      settings: {}
    }
    vi.mocked(identityService.createPersona).mockResolvedValue(newPersona)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Add Persona/i })).toBeInTheDocument()
    })

    const addButton = screen.getByRole('button', { name: /Add Persona/i })
    fireEvent.click(addButton)

    await waitFor(() => {
      const dialog = screen.getByRole('dialog')
      expect(dialog).toBeInTheDocument()
    })

    const nameInput = screen.getByLabelText(/Persona Name/i)
    const typeSelect = screen.getByLabelText(/Persona Type/i)
    
    fireEvent.change(nameInput, { target: { value: 'Study' } })
    fireEvent.mouseDown(typeSelect)
    
    await waitFor(() => {
      const studyOption = screen.getByRole('option', { name: /study/i })
      fireEvent.click(studyOption)
    })

    const createButton = screen.getByRole('button', { name: /Create/i })
    fireEvent.click(createButton)

    await waitFor(() => {
      expect(identityService.createPersona).toHaveBeenCalledWith({
        name: 'Study',
        type: 'study',
        isDefault: false,
        settings: {}
      })
    })
  })

  it('should allow editing a persona', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)
    vi.mocked(identityService.updatePersona).mockResolvedValue({
      ...mockPersonas[0],
      name: 'Office Work'
    })

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText('Work')).toBeInTheDocument()
    })

    const editButtons = screen.getAllByLabelText(/Edit persona/i)
    fireEvent.click(editButtons[0])

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText(/Edit Persona/i)).toBeInTheDocument()
    })

    const nameInput = screen.getByLabelText(/Persona Name/i) as HTMLInputElement
    expect(nameInput.value).toBe('Work')
    
    fireEvent.change(nameInput, { target: { value: 'Office Work' } })
    
    const saveButton = screen.getByRole('button', { name: /Save/i })
    fireEvent.click(saveButton)

    await waitFor(() => {
      expect(identityService.updatePersona).toHaveBeenCalledWith('1', {
        name: 'Office Work'
      })
    })
  })

  it('should allow deleting a persona', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)
    vi.mocked(identityService.deletePersona).mockResolvedValue()

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText('Gaming')).toBeInTheDocument()
    })

    // Get all delete buttons (including disabled ones)
    const personaCards = screen.getAllByTestId('persona-card')
    const gamingCard = personaCards[2] // Gaming is the third persona

    const deleteButton = within(gamingCard).getByRole('button', { name: /Delete persona/i })
    expect(deleteButton).not.toBeDisabled() // Ensure it's enabled
    fireEvent.click(deleteButton)

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText(/Are you sure you want to delete/i)).toBeInTheDocument()
    })

    const confirmButton = screen.getByRole('button', { name: /Confirm/i })
    fireEvent.click(confirmButton)

    await waitFor(() => {
      expect(identityService.deletePersona).toHaveBeenCalledWith('3')
    })
  })

  it('should not allow deleting default persona', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText('Personal')).toBeInTheDocument()
    })

    const personaCards = screen.getAllByTestId('persona-card')
    const personalCard = personaCards[1] // Personal is the second persona and is default
    
    const deleteButton = within(personalCard).getByLabelText(/Delete persona/i)
    expect(deleteButton).toBeDisabled()
  })

  it('should not allow deleting active persona', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText('Work')).toBeInTheDocument()
    })

    const personaCards = screen.getAllByTestId('persona-card')
    const workCard = personaCards[0] // Work is the first persona and is active
    
    const deleteButton = within(workCard).getByLabelText(/Delete persona/i)
    expect(deleteButton).toBeDisabled()
  })

  it('should show loading state while fetching personas', async () => {
    vi.mocked(identityService.getPersonas).mockImplementation(
      () => new Promise(() => {}) // Never resolves to keep loading
    )

    render(<PersonaManagement />, { wrapper })

    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('should handle error when fetching personas fails', async () => {
    const errorMessage = 'Failed to fetch personas'
    vi.mocked(identityService.getPersonas).mockRejectedValue(new Error(errorMessage))

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      expect(screen.getByText(/Error loading personas/i)).toBeInTheDocument()
    })
  })

  it('should indicate active and default personas', async () => {
    vi.mocked(identityService.getPersonas).mockResolvedValue(mockPersonas)

    render(<PersonaManagement />, { wrapper })

    await waitFor(() => {
      const personaCards = screen.getAllByTestId('persona-card')
      
      // Work card should show active indicator
      const workCard = personaCards[0]
      expect(within(workCard).getByText(/Active/i)).toBeInTheDocument()
      
      // Personal card should show default indicator
      const personalCard = personaCards[1]
      expect(within(personalCard).getByText(/Default/i)).toBeInTheDocument()
    })
  })
})
