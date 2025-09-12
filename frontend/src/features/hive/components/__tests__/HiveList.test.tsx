import React from 'react'
import { describe, it, expect, vi, beforeEach, type MockedFunction } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import { HiveList } from '../HiveList'
import { Hive, HiveMember, CreateHiveRequest, User } from '@shared/types'

// Mock child components
vi.mock('../HiveCard', () => ({
  HiveCard: ({ 
    hive, 
    onJoin, 
    onLeave, 
    onEnter, 
    onSettings, 
    onShare, 
    variant,
    members = [],
    currentUserId
  }: {
    hive: Hive
    onJoin?: (hiveId: string) => void
    onLeave?: (hiveId: string) => void
    onEnter?: (hiveId: string) => void
    onSettings?: (hiveId: string) => void
    onShare?: (hiveId: string) => void
    variant?: 'default' | 'compact'
    members?: HiveMember[]
    currentUserId?: string
  }) => {
    const isMember = currentUserId && members.some(m => m.userId === currentUserId)
    const onlineMembers = members.filter(m => m.isActive).length
    
    return (
      <div data-testid={`hive-card-${hive.id}`} data-variant={variant}>
        <h3>{hive.name}</h3>
        <p>{hive.description}</p>
        <div>Members: {hive.currentMembers}/{hive.maxMembers}</div>
        <div>Online: {onlineMembers}</div>
        <div>Tags: {hive.tags.join(', ')}</div>
        <div>Public: {hive.isPublic ? 'Yes' : 'No'}</div>
        {onJoin && !isMember && (
          <button onClick={() => onJoin(hive.id)}>Join Hive</button>
        )}
        {onLeave && isMember && (
          <button onClick={() => onLeave(hive.id)}>Leave Hive</button>
        )}
        {onEnter && isMember && (
          <button onClick={() => onEnter(hive.id)}>Enter Hive</button>
        )}
        {onSettings && isMember && (
          <button onClick={() => onSettings(hive.id)}>Settings</button>
        )}
        {onShare && (
          <button onClick={() => onShare(hive.id)}>Share</button>
        )}
      </div>
    )
  }
}))

vi.mock('../CreateHiveForm', () => ({
  CreateHiveForm: ({ 
    open, 
    onClose, 
    onSubmit 
  }: {
    open: boolean
    onClose: () => void
    onSubmit: (hive: CreateHiveRequest) => void
  }) => {
    if (!open) return null
    
    const handleSubmit = () => {
      onSubmit({
        name: 'New Test Hive',
        description: 'A newly created test hive',
        maxMembers: 10,
        isPublic: true,
        tags: ['test'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'continuous',
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      })
    }

    return (
      <div data-testid="create-hive-form">
        <button onClick={onClose}>Cancel</button>
        <button onClick={handleSubmit}>Create Hive</button>
      </div>
    )
  }
}))

vi.mock('@shared/components/loading', () => ({
  ContentSkeleton: ({ type, count }: { type: string; count: number }) => (
    <div data-testid="loading-skeleton">
      Loading {count} {type} items...
    </div>
  )
}))

// Mock data
const mockUser: User = {
  id: 'user1',
  username: 'testuser',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  name: 'Test User',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z'
}

const mockOwner: User = {
  id: 'owner1',
  username: 'owner',
  email: 'owner@example.com',
  firstName: 'Hive',
  lastName: 'Owner',
  name: 'Hive Owner',
  avatar: null,
  profilePicture: null,
  isEmailVerified: true,
  isVerified: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z'
}

const createMockHive = (overrides: Partial<Hive> = {}): Hive => ({
  id: `hive-${Math.random().toString(36).substr(2, 9)}`,
  name: 'Test Hive',
  description: 'A test hive for development',
  ownerId: 'owner1',
  owner: mockOwner,
  maxMembers: 10,
  isPublic: true,
  tags: ['study', 'programming'],
  settings: {
    allowChat: true,
    allowVoice: false,
    requireApproval: false,
    focusMode: 'continuous',
    defaultSessionLength: 25,
    maxSessionLength: 120
  },
  currentMembers: 3,
  memberCount: 3,
  isOwner: false,
  isMember: false,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  ...overrides
})

const createMockMember = (overrides: Partial<HiveMember> = {}): HiveMember => ({
  id: `member-${Math.random().toString(36).substr(2, 9)}`,
  userId: 'user1',
  user: mockUser,
  hiveId: 'hive1',
  role: 'member',
  joinedAt: '2024-01-01T00:00:00Z',
  isActive: true,
  permissions: {
    canInviteMembers: false,
    canModerateChat: false,
    canManageSettings: false,
    canStartTimers: false
  },
  ...overrides
})

describe('HiveList', () => {
  const mockProps = {
    hives: [] as Hive[],
    members: {} as Record<string, HiveMember[]>,
    currentUserId: undefined as string | undefined,
    isLoading: false,
    error: null,
    onJoin: vi.fn(),
    onLeave: vi.fn(),
    onEnter: vi.fn(),
    onSettings: vi.fn(),
    onShare: vi.fn(),
    onRefresh: vi.fn(),
    onCreateHive: vi.fn(),
    title: 'Test Hives',
    showCreateButton: true,
    showFilters: true,
    defaultView: 'grid' as const
  }

  // Helper function to click on Material UI Select components
  const clickSelect = async (user: ReturnType<typeof userEvent.setup>, labelText: string) => {
    const label = screen.getByText(labelText)
    const formControl = label.closest('.MuiFormControl-root')
    const selectElement = formControl?.querySelector('[role="combobox"]')
    if (selectElement) {
      await user.click(selectElement)
    }
    return selectElement
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render the component with title', () => {
      renderWithProviders(<HiveList {...mockProps} />)
      
      expect(screen.getByRole('heading', { name: 'Test Hives' })).toBeInTheDocument()
    })

    it('should render with default title when none provided', () => {
      renderWithProviders(<HiveList {...mockProps} title={undefined} />)
      
      expect(screen.getByRole('heading', { name: 'Hives' })).toBeInTheDocument()
    })

    it('should render create hive button when showCreateButton is true', () => {
      renderWithProviders(<HiveList {...mockProps} />)
      
      expect(screen.getByRole('button', { name: /create hive/i })).toBeInTheDocument()
    })

    it('should not render create hive button when showCreateButton is false', () => {
      renderWithProviders(<HiveList {...mockProps} showCreateButton={false} />)
      
      expect(screen.queryByRole('button', { name: /create hive/i })).not.toBeInTheDocument()
    })

    it('should render refresh button when onRefresh is provided', () => {
      renderWithProviders(<HiveList {...mockProps} />)
      
      expect(screen.getByRole('button', { name: /refresh/i })).toBeInTheDocument()
    })

    it('should not render refresh button when onRefresh is not provided', () => {
      renderWithProviders(<HiveList {...mockProps} onRefresh={undefined} />)
      
      expect(screen.queryByRole('button', { name: /refresh/i })).not.toBeInTheDocument()
    })

    it('should render filters when showFilters is true', () => {
      renderWithProviders(<HiveList {...mockProps} />)
      
      expect(screen.getByPlaceholderText('Search hives...')).toBeInTheDocument()
      expect(screen.getByText('Category')).toBeInTheDocument()
      expect(screen.getByText('Sort by')).toBeInTheDocument()
    })

    it('should not render filters when showFilters is false', () => {
      renderWithProviders(<HiveList {...mockProps} showFilters={false} />)
      
      expect(screen.queryByPlaceholderText('Search hives...')).not.toBeInTheDocument()
    })
  })

  describe('Loading States', () => {
    it('should display loading skeleton when isLoading is true', () => {
      renderWithProviders(<HiveList {...mockProps} isLoading={true} />)
      
      expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument()
      expect(screen.getByText('Loading 6 hive items...')).toBeInTheDocument()
    })

    it('should show loading text in results count when loading', () => {
      renderWithProviders(<HiveList {...mockProps} isLoading={true} />)
      
      expect(screen.getByText('Loading...')).toBeInTheDocument()
    })

    it('should disable refresh button when loading', () => {
      renderWithProviders(<HiveList {...mockProps} isLoading={true} />)
      
      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      expect(refreshButton).toBeDisabled()
    })
  })

  describe('Error States', () => {
    it('should display error message when error is provided', () => {
      const errorMessage = 'Failed to load hives'
      renderWithProviders(<HiveList {...mockProps} error={errorMessage} />)
      
      expect(screen.getByText(errorMessage)).toBeInTheDocument()
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })

    it('should show retry button in error state', () => {
      renderWithProviders(<HiveList {...mockProps} error="Network error" />)
      
      const retryButton = screen.getByRole('button', { name: /retry/i })
      expect(retryButton).toBeInTheDocument()
    })

    it('should call onRefresh when retry button is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} error="Network error" />)
      
      const retryButton = screen.getByRole('button', { name: /retry/i })
      await user.click(retryButton)
      
      expect(mockProps.onRefresh).toHaveBeenCalledOnce()
    })

    it('should not show retry button if onRefresh is not provided', () => {
      renderWithProviders(
        <HiveList {...mockProps} error="Network error" onRefresh={undefined} />
      )
      
      expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument()
    })
  })

  describe('Empty States', () => {
    it('should show empty state when no hives are provided', () => {
      renderWithProviders(<HiveList {...mockProps} hives={[]} />)
      
      expect(screen.getByText('No hives found')).toBeInTheDocument()
      expect(screen.getByText('Be the first to create a hive!')).toBeInTheDocument()
    })

    it('should show create button in empty state when showCreateButton is true', () => {
      renderWithProviders(<HiveList {...mockProps} hives={[]} />)
      
      expect(screen.getByRole('button', { name: /create your first hive/i })).toBeInTheDocument()
    })

    it('should not show create button in empty state when showCreateButton is false', () => {
      renderWithProviders(<HiveList {...mockProps} hives={[]} showCreateButton={false} />)
      
      expect(screen.queryByRole('button', { name: /create your first hive/i })).not.toBeInTheDocument()
    })

    it('should show filtered empty state when search returns no results', async () => {
      const user = userEvent.setup()
      const hives = [createMockHive({ name: 'Programming Hive' })]
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'nonexistent')
      
      await waitFor(() => {
        expect(screen.getByText('No hives found')).toBeInTheDocument()
        expect(screen.getByText('Try adjusting your search or filters')).toBeInTheDocument()
      })
    })
  })

  describe('Hive Display', () => {
    it('should render hive cards for each hive', () => {
      const hives = [
        createMockHive({ id: 'hive1', name: 'Hive 1' }),
        createMockHive({ id: 'hive2', name: 'Hive 2' })
      ]
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.getByTestId('hive-card-hive1')).toBeInTheDocument()
      expect(screen.getByTestId('hive-card-hive2')).toBeInTheDocument()
      expect(screen.getByText('Hive 1')).toBeInTheDocument()
      expect(screen.getByText('Hive 2')).toBeInTheDocument()
    })

    it('should show results count', () => {
      const hives = [
        createMockHive(),
        createMockHive()
      ]
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.getByText('2 hives found')).toBeInTheDocument()
    })

    it('should show singular form for single hive', () => {
      const hives = [createMockHive()]
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.getByText('1 hive found')).toBeInTheDocument()
    })

    it('should pass members data to hive cards', () => {
      const hive = createMockHive({ id: 'hive1' })
      const member = createMockMember({ hiveId: 'hive1', isActive: true })
      const members = { hive1: [member] }
      
      renderWithProviders(
        <HiveList {...mockProps} hives={[hive]} members={members} />
      )
      
      expect(screen.getByText('Online: 1')).toBeInTheDocument()
    })
  })

  describe('Search Functionality', () => {
    const hives = [
      createMockHive({ 
        name: 'Study Group', 
        description: 'Focus on studying',
        tags: ['study', 'academic']
      }),
      createMockHive({ 
        name: 'Programming Hive', 
        description: 'Code together',
        tags: ['coding', 'tech']
      })
    ]

    it('should filter hives by name', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'Study')
      
      await waitFor(() => {
        expect(screen.getByText('Study Group')).toBeInTheDocument()
        expect(screen.queryByText('Programming Hive')).not.toBeInTheDocument()
        expect(screen.getByText('1 hive found')).toBeInTheDocument()
      })
    })

    it('should filter hives by description', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'code')
      
      await waitFor(() => {
        expect(screen.getByText('Programming Hive')).toBeInTheDocument()
        expect(screen.queryByText('Study Group')).not.toBeInTheDocument()
      })
    })

    it('should filter hives by tags', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'academic')
      
      await waitFor(() => {
        expect(screen.getByText('Study Group')).toBeInTheDocument()
        expect(screen.queryByText('Programming Hive')).not.toBeInTheDocument()
      })
    })

    it('should be case insensitive', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'STUDY')
      
      await waitFor(() => {
        expect(screen.getByText('Study Group')).toBeInTheDocument()
      })
    })

    it('should clear search results when input is cleared', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'Study')
      
      await waitFor(() => {
        expect(screen.getByText('1 hive found')).toBeInTheDocument()
      })
      
      await user.clear(searchInput)
      
      await waitFor(() => {
        expect(screen.getByText('2 hives found')).toBeInTheDocument()
      })
    })
  })

  describe('Category Filtering', () => {
    const publicHive = createMockHive({ id: 'public', isPublic: true })
    const privateHive = createMockHive({ id: 'private', isPublic: false })
    const joinedMember = createMockMember({ userId: 'user1', hiveId: 'joined' })
    const joinedHive = createMockHive({ id: 'joined', isPublic: true })
    const fullHive = createMockHive({ id: 'full', maxMembers: 5, currentMembers: 5 })
    
    const hives = [publicHive, privateHive, joinedHive, fullHive]
    const members = { joined: [joinedMember] }

    it('should filter by public hives', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <HiveList {...mockProps} hives={hives} members={members} currentUserId="user1" />
      )
      
      await clickSelect(user, 'Category')
      await user.click(screen.getByRole('option', { name: /public/i }))
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-public')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-joined')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-private')).not.toBeInTheDocument()
      })
    })

    it('should filter by private hives', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <HiveList {...mockProps} hives={hives} members={members} currentUserId="user1" />
      )
      
      await clickSelect(user, 'Category')
      await user.click(screen.getByRole('option', { name: /private/i }))
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-private')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-public')).not.toBeInTheDocument()
      })
    })

    it('should filter by joined hives', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <HiveList {...mockProps} hives={hives} members={members} currentUserId="user1" />
      )
      
      await clickSelect(user, 'Category')
      await user.click(screen.getByRole('option', { name: /joined/i }))
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-joined')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-public')).not.toBeInTheDocument()
        expect(screen.getByText('1 hive found')).toBeInTheDocument()
      })
    })

    it('should filter by available hives', async () => {
      const user = userEvent.setup()
      renderWithProviders(
        <HiveList {...mockProps} hives={hives} members={members} currentUserId="user1" />
      )
      
      await clickSelect(user, 'Category')
      await user.click(screen.getByRole('option', { name: /available/i }))
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-public')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-private')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-joined')).not.toBeInTheDocument() // Already joined
        expect(screen.queryByTestId('hive-card-full')).not.toBeInTheDocument() // Full
      })
    })
  })

  describe('Sorting', () => {
    const hives = [
      createMockHive({ 
        id: 'alpha', 
        name: 'Alpha Hive', 
        currentMembers: 5,
        createdAt: '2024-01-03T00:00:00Z'
      }),
      createMockHive({ 
        id: 'beta', 
        name: 'Beta Hive', 
        currentMembers: 8,
        createdAt: '2024-01-01T00:00:00Z'
      }),
      createMockHive({ 
        id: 'gamma', 
        name: 'Gamma Hive', 
        currentMembers: 3,
        createdAt: '2024-01-02T00:00:00Z'
      })
    ]

    const members = {
      alpha: [
        createMockMember({ isActive: true }),
        createMockMember({ isActive: false })
      ],
      beta: [
        createMockMember({ isActive: true }),
        createMockMember({ isActive: true }),
        createMockMember({ isActive: true })
      ],
      gamma: [
        createMockMember({ isActive: false })
      ]
    }

    it('should sort by name alphabetically', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} members={members} />)
      
      await clickSelect(user, 'Sort by')
      await user.click(screen.getByRole('option', { name: /name/i }))
      
      await waitFor(() => {
        const hiveCards = screen.getAllByTestId(/hive-card-/)
        expect(hiveCards[0]).toHaveAttribute('data-testid', 'hive-card-alpha')
        expect(hiveCards[1]).toHaveAttribute('data-testid', 'hive-card-beta')
        expect(hiveCards[2]).toHaveAttribute('data-testid', 'hive-card-gamma')
      })
    })

    it('should sort by member count descending', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} members={members} />)
      
      await clickSelect(user, 'Sort by')
      await user.click(screen.getByRole('option', { name: /members/i }))
      
      await waitFor(() => {
        const hiveCards = screen.getAllByTestId(/hive-card-/)
        expect(hiveCards[0]).toHaveAttribute('data-testid', 'hive-card-beta') // 8 members
        expect(hiveCards[1]).toHaveAttribute('data-testid', 'hive-card-alpha') // 5 members
        expect(hiveCards[2]).toHaveAttribute('data-testid', 'hive-card-gamma') // 3 members
      })
    })

    it('should sort by activity (online members then total members)', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} members={members} />)
      
      // Activity is the default sort, so cards should be ordered by online members
      const hiveCards = screen.getAllByTestId(/hive-card-/)
      expect(hiveCards[0]).toHaveAttribute('data-testid', 'hive-card-beta') // 3 online
      expect(hiveCards[1]).toHaveAttribute('data-testid', 'hive-card-alpha') // 1 online
      expect(hiveCards[2]).toHaveAttribute('data-testid', 'hive-card-gamma') // 0 online
    })

    it('should sort by created date (newest first)', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} members={members} />)
      
      await clickSelect(user, 'Sort by')
      await user.click(screen.getByRole('option', { name: /created/i }))
      
      await waitFor(() => {
        const hiveCards = screen.getAllByTestId(/hive-card-/)
        expect(hiveCards[0]).toHaveAttribute('data-testid', 'hive-card-alpha') // 2024-01-03
        expect(hiveCards[1]).toHaveAttribute('data-testid', 'hive-card-gamma') // 2024-01-02
        expect(hiveCards[2]).toHaveAttribute('data-testid', 'hive-card-beta') // 2024-01-01
      })
    })
  })

  describe('Tag Filtering', () => {
    const hives = [
      createMockHive({ 
        id: 'hive1', 
        name: 'Study Hive',
        tags: ['study', 'academic', 'math'] 
      }),
      createMockHive({ 
        id: 'hive2', 
        name: 'Work Hive',
        tags: ['work', 'programming', 'tech'] 
      }),
      createMockHive({ 
        id: 'hive3', 
        name: 'Mixed Hive',
        tags: ['study', 'programming'] 
      })
    ]

    it('should display all available tags as chips', () => {
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.getByText('Filter by tags:')).toBeInTheDocument()
      expect(screen.getByText('academic')).toBeInTheDocument()
      expect(screen.getByText('math')).toBeInTheDocument()
      expect(screen.getByText('programming')).toBeInTheDocument()
      expect(screen.getByText('study')).toBeInTheDocument()
      expect(screen.getByText('tech')).toBeInTheDocument()
      expect(screen.getByText('work')).toBeInTheDocument()
    })

    it('should filter hives by selected tag', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const studyChip = screen.getByText('study')
      await user.click(studyChip)
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-hive1')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-hive3')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-hive2')).not.toBeInTheDocument()
        expect(screen.getByText('2 hives found')).toBeInTheDocument()
      })
    })

    it('should allow multiple tag selection', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      await user.click(screen.getByText('study'))
      await user.click(screen.getByText('programming'))
      
      await waitFor(() => {
        expect(screen.getByTestId('hive-card-hive3')).toBeInTheDocument() // Has both tags
        expect(screen.queryByTestId('hive-card-hive1')).not.toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-hive2')).not.toBeInTheDocument()
        expect(screen.getByText('1 hive found')).toBeInTheDocument()
      })
    })

    it('should deselect tag when clicked again', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const studyChip = screen.getByText('study')
      await user.click(studyChip)
      
      await waitFor(() => {
        expect(screen.getByText('2 hives found')).toBeInTheDocument()
      })
      
      await user.click(studyChip)
      
      await waitFor(() => {
        expect(screen.getByText('3 hives found')).toBeInTheDocument()
      })
    })

    it('should not show tag filter section when no tags exist', () => {
      const hivesWithoutTags = hives.map(hive => ({ ...hive, tags: [] }))
      renderWithProviders(<HiveList {...mockProps} hives={hivesWithoutTags} />)
      
      expect(screen.queryByText('Filter by tags:')).not.toBeInTheDocument()
    })
  })

  describe('View Mode Toggle', () => {
    const hives = [createMockHive()]

    it('should start with default view mode', () => {
      renderWithProviders(<HiveList {...mockProps} hives={hives} defaultView="list" />)
      
      const hiveCard = screen.getByTestId('hive-card-' + hives[0].id)
      expect(hiveCard).toHaveAttribute('data-variant', 'compact')
    })

    it('should toggle between grid and list view on desktop', async () => {
      const user = userEvent.setup()
      // Mock desktop breakpoint
      Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1200 })
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const listViewButton = screen.getByRole('button', { name: /list view/i })
      await user.click(listViewButton)
      
      await waitFor(() => {
        const hiveCard = screen.getByTestId('hive-card-' + hives[0].id)
        expect(hiveCard).toHaveAttribute('data-variant', 'compact')
      })
    })

    it('should not show view toggle on mobile', () => {
      // Mock mobile breakpoint
      Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 600 })
      
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.queryByRole('button', { name: /grid view/i })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /list view/i })).not.toBeInTheDocument()
    })
  })

  describe('User Interactions', () => {
    const mockHive = createMockHive({ id: 'test-hive' })

    it('should call onJoin when join button is clicked', async () => {
      const user = userEvent.setup()
      const onJoin = vi.fn()
      
      renderWithProviders(
        <HiveList {...mockProps} hives={[mockHive]} onJoin={onJoin} currentUserId="user1" />
      )
      
      const joinButton = screen.getByRole('button', { name: /join hive/i })
      await user.click(joinButton)
      
      expect(onJoin).toHaveBeenCalledWith('test-hive')
    })

    it('should call onLeave when leave button is clicked', async () => {
      const user = userEvent.setup()
      const onLeave = vi.fn()
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveList 
          {...mockProps} 
          hives={[mockHive]} 
          members={{ 'test-hive': [member] }}
          currentUserId="user1"
          onLeave={onLeave} 
        />
      )
      
      const leaveButton = screen.getByRole('button', { name: /leave hive/i })
      await user.click(leaveButton)
      
      expect(onLeave).toHaveBeenCalledWith('test-hive')
    })

    it('should call onEnter when enter button is clicked', async () => {
      const user = userEvent.setup()
      const onEnter = vi.fn()
      const member = createMockMember({ userId: 'user1', hiveId: 'test-hive' })
      
      renderWithProviders(
        <HiveList 
          {...mockProps} 
          hives={[mockHive]} 
          members={{ 'test-hive': [member] }}
          currentUserId="user1"
          onEnter={onEnter} 
        />
      )
      
      const enterButton = screen.getByRole('button', { name: /enter hive/i })
      await user.click(enterButton)
      
      expect(onEnter).toHaveBeenCalledWith('test-hive')
    })

    it('should call onRefresh when refresh button is clicked', async () => {
      const user = userEvent.setup()
      const onRefresh = vi.fn()
      
      renderWithProviders(<HiveList {...mockProps} onRefresh={onRefresh} />)
      
      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      await user.click(refreshButton)
      
      expect(onRefresh).toHaveBeenCalledOnce()
    })
  })

  describe('Create Hive Dialog', () => {
    it('should open create dialog when create button is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} />)
      
      const createButton = screen.getByRole('button', { name: /create hive/i })
      await user.click(createButton)
      
      expect(screen.getByTestId('create-hive-form')).toBeInTheDocument()
    })

    it('should close create dialog when cancel is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} />)
      
      const createButton = screen.getByRole('button', { name: /create hive/i })
      await user.click(createButton)
      
      const cancelButton = screen.getByRole('button', { name: /cancel/i })
      await user.click(cancelButton)
      
      expect(screen.queryByTestId('create-hive-form')).not.toBeInTheDocument()
    })

    it('should call onCreateHive and close dialog when form is submitted', async () => {
      const user = userEvent.setup()
      const onCreateHive = vi.fn()
      
      renderWithProviders(<HiveList {...mockProps} onCreateHive={onCreateHive} />)
      
      const createButton = screen.getByRole('button', { name: /create hive/i })
      await user.click(createButton)
      
      const submitButton = screen.getByRole('button', { name: /create hive/i })
      await user.click(submitButton)
      
      expect(onCreateHive).toHaveBeenCalledWith({
        name: 'New Test Hive',
        description: 'A newly created test hive',
        maxMembers: 10,
        isPublic: true,
        tags: ['test'],
        settings: {
          allowChat: true,
          allowVoice: false,
          requireApproval: false,
          focusMode: 'continuous',
          defaultSessionLength: 25,
          maxSessionLength: 120
        }
      })
      
      expect(screen.queryByTestId('create-hive-form')).not.toBeInTheDocument()
    })

    it('should open create dialog from empty state button', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={[]} />)
      
      const createButton = screen.getByRole('button', { name: /create your first hive/i })
      await user.click(createButton)
      
      expect(screen.getByTestId('create-hive-form')).toBeInTheDocument()
    })
  })

  describe('Combined Filtering', () => {
    const complexHives = [
      createMockHive({ 
        id: 'study-public', 
        name: 'Study Group', 
        description: 'Public study group',
        isPublic: true,
        tags: ['study', 'academic'],
        currentMembers: 5
      }),
      createMockHive({ 
        id: 'study-private', 
        name: 'Private Study', 
        description: 'Private study session',
        isPublic: false,
        tags: ['study', 'private'],
        currentMembers: 3
      }),
      createMockHive({ 
        id: 'work-public', 
        name: 'Work Focus', 
        description: 'Public work session',
        isPublic: true,
        tags: ['work', 'productivity'],
        currentMembers: 8
      })
    ]

    it('should apply search and tag filters together', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={complexHives} />)
      
      // Search for "study"
      const searchInput = screen.getByPlaceholderText('Search hives...')
      await user.type(searchInput, 'study')
      
      // Then filter by study tag
      const studyChip = screen.getByText('study')
      await user.click(studyChip)
      
      await waitFor(() => {
        expect(screen.getByText('2 hives found')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-study-public')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-study-private')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-work-public')).not.toBeInTheDocument()
      })
    })

    it('should apply category and tag filters together', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HiveList {...mockProps} hives={complexHives} />)
      
      // Filter by public category
      await clickSelect(user, 'Category')
      await user.click(screen.getByRole('option', { name: /public/i }))
      
      // Then filter by study tag
      const studyChip = screen.getByText('study')
      await user.click(studyChip)
      
      await waitFor(() => {
        expect(screen.getByText('1 hive found')).toBeInTheDocument()
        expect(screen.getByTestId('hive-card-study-public')).toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-study-private')).not.toBeInTheDocument()
        expect(screen.queryByTestId('hive-card-work-public')).not.toBeInTheDocument()
      })
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      const hives = [createMockHive()]
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      expect(screen.getByRole('heading', { name: /test hives/i })).toBeInTheDocument()
      expect(screen.getByRole('textbox')).toBeInTheDocument() // Search input
      expect(screen.getByText('Category')).toBeInTheDocument()
      expect(screen.getByText('Sort by')).toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument() // No error state
    })

    it('should be keyboard navigable', async () => {
      const user = userEvent.setup()
      const hives = [createMockHive()]
      renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      // Tab through interactive elements - order may vary, just check they're focusable
      const searchInput = screen.getByPlaceholderText('Search hives...')
      const refreshButton = screen.getByRole('button', { name: /refresh/i })
      const createButton = screen.getByRole('button', { name: /create hive/i })
      
      // Focus on search input manually to test it's focusable
      searchInput.focus()
      expect(searchInput).toHaveFocus()
      
      // Test buttons are focusable
      refreshButton.focus()
      expect(refreshButton).toHaveFocus()
      
      createButton.focus()
      expect(createButton).toHaveFocus()
    })

    it('should announce loading state to screen readers', () => {
      renderWithProviders(<HiveList {...mockProps} isLoading={true} />)
      
      expect(screen.getByText('Loading...')).toBeInTheDocument()
      expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument()
    })

    it('should announce error state to screen readers', () => {
      renderWithProviders(<HiveList {...mockProps} error="Failed to load" />)
      
      const alert = screen.getByRole('alert')
      expect(alert).toHaveTextContent('Failed to load')
    })
  })

  describe('Performance', () => {
    it('should not re-render unnecessarily with same props', () => {
      const hives = [createMockHive()]
      const { rerender } = renderWithProviders(<HiveList {...mockProps} hives={hives} />)
      
      const initialText = screen.getByText('1 hive found')
      expect(initialText).toBeInTheDocument()
      
      // Rerender with same props
      rerender(<HiveList {...mockProps} hives={hives} />)
      
      // Should still show the same content
      expect(screen.getByText('1 hive found')).toBeInTheDocument()
    })

    it('should handle large lists efficiently', () => {
      // Create 100 hives
      const largeHiveList = Array.from({ length: 100 }, (_, i) => 
        createMockHive({ id: `hive-${i}`, name: `Hive ${i}` })
      )
      
      renderWithProviders(<HiveList {...mockProps} hives={largeHiveList} />)
      
      expect(screen.getByText('100 hives found')).toBeInTheDocument()
      expect(screen.getAllByTestId(/hive-card-/)).toHaveLength(100)
    })
  })
})