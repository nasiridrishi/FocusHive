import React from 'react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import HivePresencePanel from '../HivePresencePanel'
import type { UserPresence, HivePresenceInfo, PresenceStatus } from '@shared/types/presence'
import type { User } from '@shared/types/auth'

// Mock presence context
const mockPresenceContext = {
  currentPresence: null,
  hivePresence: {},
  updatePresence: vi.fn(),
  joinHivePresence: vi.fn(),
  leaveHivePresence: vi.fn(),
  startFocusSession: vi.fn(),
  endFocusSession: vi.fn(),
  takeBreak: vi.fn(),
  resumeFromBreak: vi.fn(),
}

// Mock users
const mockUsers: User[] = [
  {
    id: 'user1',
    email: 'john@example.com',
    username: 'john_doe',
    firstName: 'John',
    lastName: 'Doe',
    name: 'John Doe',
    avatar: '/avatars/john.jpg',
    profilePicture: '/avatars/john.jpg',
    isEmailVerified: true,
    isVerified: true,
    createdAt: '2025-01-01T10:00:00Z',
    updatedAt: '2025-01-01T10:00:00Z',
  },
  {
    id: 'user2',
    email: 'jane@example.com',
    username: 'jane_smith',
    firstName: 'Jane',
    lastName: 'Smith',
    name: 'Jane Smith',
    avatar: '/avatars/jane.jpg',
    profilePicture: '/avatars/jane.jpg',
    isEmailVerified: true,
    isVerified: true,
    createdAt: '2025-01-01T10:00:00Z',
    updatedAt: '2025-01-01T10:00:00Z',
  },
  {
    id: 'user3',
    email: 'bob@example.com',
    username: 'bob_wilson',
    firstName: 'Bob',
    lastName: 'Wilson',
    name: 'Bob Wilson',
    isEmailVerified: true,
    isVerified: true,
    createdAt: '2025-01-01T10:00:00Z',
    updatedAt: '2025-01-01T10:00:00Z',
  },
]

// Mock user presence data
const createMockPresence = (
  user: User,
  status: PresenceStatus = 'online',
  activity?: string
): UserPresence => ({
  userId: user.id,
  user,
  status,
  currentActivity: activity,
  sessionStartTime: '2025-01-01T10:00:00Z',
  lastSeen: new Date().toISOString(),
  hiveId: 'hive1',
  deviceInfo: {
    type: 'web',
    browser: 'Chrome',
    os: 'Windows',
  },
})

const mockHivePresence: HivePresenceInfo = {
  hiveId: 'hive1',
  activeUsers: [
    createMockPresence(mockUsers[0], 'focusing', 'Working on CS project'),
    createMockPresence(mockUsers[1], 'online', 'Available for study'),
    createMockPresence(mockUsers[2], 'break', 'Taking a coffee break'),
  ],
  totalOnline: 3,
  totalFocusing: 1,
  totalOnBreak: 1,
}

// Mock WebSocket for real-time updates
const mockWebSocket = {
  send: vi.fn(),
  close: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: WebSocket.OPEN,
}

// Mock presence hook
vi.mock('@shared/contexts/PresenceContext', () => ({
  usePresence: () => mockPresenceContext,
  PresenceProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

describe('HivePresencePanel', () => {
  const defaultProps = {
    hiveId: 'hive1',
    onUserClick: vi.fn(),
    onPresenceUpdate: vi.fn(),
    compact: false,
    showActivityText: true,
    maxVisibleUsers: 10,
    enableCollapse: true,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    // Reset mock presence context
    mockPresenceContext.hivePresence = {
      hive1: mockHivePresence,
    }
    mockPresenceContext.currentPresence = createMockPresence(mockUsers[0], 'online')

    // Mock WebSocket connection
    Object.defineProperty(global, 'WebSocket', {
      value: vi.fn(() => mockWebSocket),
      configurable: true,
      writable: true,
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('Rendering Tests', () => {
    it('should render presence panel container', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('hive-presence-panel')).toBeInTheDocument()
    })

    it('should display panel title with online count', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('presence-panel-title')).toHaveTextContent('Online (3)')
    })

    it('should render user list container', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('presence-user-list')).toBeInTheDocument()
    })

    it('should display empty state when no users online', () => {
      mockPresenceContext.hivePresence = {
        hive1: { ...mockHivePresence, activeUsers: [], totalOnline: 0 },
      }

      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('presence-empty-state')).toBeInTheDocument()
      expect(screen.getByText('No one is online right now')).toBeInTheDocument()
      expect(screen.getByText('Be the first to join this hive!')).toBeInTheDocument()
    })

    it('should show loading state', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} isLoading />)

      expect(screen.getByTestId('presence-loading')).toBeInTheDocument()
      expect(screen.getAllByTestId('presence-user-skeleton')).toHaveLength(3)
    })

    it('should render compact layout when specified', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} compact />)

      const panel = screen.getByTestId('hive-presence-panel')
      expect(panel).toHaveClass('presence-panel--compact')
    })
  })

  describe('User List Display Tests', () => {
    it('should display all online users', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('presence-user-john_doe')).toBeInTheDocument()
      expect(screen.getByTestId('presence-user-jane_smith')).toBeInTheDocument()
      expect(screen.getByTestId('presence-user-bob_wilson')).toBeInTheDocument()
    })

    it('should show user avatars with proper alt text', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const johnAvatar = screen.getByRole('img', { name: /john doe/i })
      expect(johnAvatar).toHaveAttribute('src', '/avatars/john.jpg')

      const janeAvatar = screen.getByRole('img', { name: /jane smith/i })
      expect(janeAvatar).toHaveAttribute('src', '/avatars/jane.jpg')
    })

    it('should display fallback avatar when no image provided', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Bob has no avatar
      const bobUser = screen.getByTestId('presence-user-bob_wilson')
      const bobAvatar = within(bobUser).getByTestId('user-avatar-fallback')
      expect(bobAvatar).toHaveTextContent('BW') // First letters of name
    })

    it('should show user names correctly', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByText('John Doe')).toBeInTheDocument()
      expect(screen.getByText('Jane Smith')).toBeInTheDocument()
      expect(screen.getByText('Bob Wilson')).toBeInTheDocument()
    })

    it('should display activity text when enabled', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} showActivityText />)

      expect(screen.getByText('Working on CS project')).toBeInTheDocument()
      expect(screen.getByText('Available for study')).toBeInTheDocument()
      expect(screen.getByText('Taking a coffee break')).toBeInTheDocument()
    })

    it('should hide activity text when disabled', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} showActivityText={false} />)

      expect(screen.queryByText('Working on CS project')).not.toBeInTheDocument()
      expect(screen.queryByText('Available for study')).not.toBeInTheDocument()
      expect(screen.queryByText('Taking a coffee break')).not.toBeInTheDocument()
    })

    it('should limit visible users when maxVisibleUsers is set', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} maxVisibleUsers={2} />)

      const userList = screen.getByTestId('presence-user-list')
      const visibleUsers = within(userList).getAllByTestId(/^presence-user-/)
      expect(visibleUsers).toHaveLength(2)

      // Should show "and X more" indicator
      expect(screen.getByTestId('more-users-indicator')).toHaveTextContent('and 1 more')
    })
  })

  describe('Status Indicators Tests', () => {
    it('should show correct status indicators', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Focusing status
      const johnUser = screen.getByTestId('presence-user-john_doe')
      expect(within(johnUser).getByTestId('status-focusing')).toBeInTheDocument()

      // Online status
      const janeUser = screen.getByTestId('presence-user-jane_smith')
      expect(within(janeUser).getByTestId('status-online')).toBeInTheDocument()

      // Break status
      const bobUser = screen.getByTestId('presence-user-bob_wilson')
      expect(within(bobUser).getByTestId('status-break')).toBeInTheDocument()
    })

    it('should display status badges with correct colors', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const focusingBadge = screen.getByTestId('status-focusing')
      expect(focusingBadge).toHaveClass('status-badge--focusing')

      const onlineBadge = screen.getByTestId('status-online')
      expect(onlineBadge).toHaveClass('status-badge--online')

      const breakBadge = screen.getByTestId('status-break')
      expect(breakBadge).toHaveClass('status-badge--break')
    })

    it('should show session duration for focusing users', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const johnUser = screen.getByTestId('presence-user-john_doe')
      expect(within(johnUser).getByTestId('session-duration')).toBeInTheDocument()
    })

    it('should display device indicators', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getAllByTestId('device-web')).toHaveLength(3)
    })
  })

  describe('Real-time Updates Tests', () => {
    it('should update when user joins hive', async () => {
      const { rerender } = renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Initially 3 users
      expect(screen.getAllByTestId(/^presence-user-/)).toHaveLength(3)

      // Simulate new user joining
      const newUser = createMockPresence({
        id: 'user4',
        name: 'Alice Cooper',
        username: 'alice_cooper',
        firstName: 'Alice',
        lastName: 'Cooper',
        email: 'alice@example.com',
        isEmailVerified: true,
        createdAt: '2025-01-01T10:00:00Z',
        updatedAt: '2025-01-01T10:00:00Z',
      })

      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: [...mockHivePresence.activeUsers, newUser],
          totalOnline: 4,
        },
      }

      rerender(<HivePresencePanel {...defaultProps} />)

      await waitFor(() => {
        expect(screen.getAllByTestId(/^presence-user-/)).toHaveLength(4)
        expect(screen.getByText('Alice Cooper')).toBeInTheDocument()
        expect(screen.getByTestId('presence-panel-title')).toHaveTextContent('Online (4)')
      })
    })

    it('should update when user leaves hive', async () => {
      const { rerender } = renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Initially 3 users
      expect(screen.getAllByTestId(/^presence-user-/)).toHaveLength(3)

      // Simulate user leaving
      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: mockHivePresence.activeUsers.slice(0, 2),
          totalOnline: 2,
        },
      }

      rerender(<HivePresencePanel {...defaultProps} />)

      await waitFor(() => {
        expect(screen.getAllByTestId(/^presence-user-/)).toHaveLength(2)
        expect(screen.queryByTestId('presence-user-bob_wilson')).not.toBeInTheDocument()
        expect(screen.getByTestId('presence-panel-title')).toHaveTextContent('Online (2)')
      })
    })

    it('should update when user status changes', async () => {
      const { rerender } = renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // John is initially focusing
      expect(screen.getByTestId('status-focusing')).toBeInTheDocument()

      // Simulate status change to online
      const updatedUser = createMockPresence(mockUsers[0], 'online', 'Taking a break')
      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: [
            updatedUser,
            ...mockHivePresence.activeUsers.slice(1),
          ],
        },
      }

      rerender(<HivePresencePanel {...defaultProps} />)

      await waitFor(() => {
        const johnUser = screen.getByTestId('presence-user-john_doe')
        expect(within(johnUser).getByTestId('status-online')).toBeInTheDocument()
        expect(within(johnUser).queryByTestId('status-focusing')).not.toBeInTheDocument()
      })
    })

    it('should call onPresenceUpdate when changes occur', async () => {
      const onPresenceUpdate = vi.fn()
      const { rerender } = renderWithProviders(
        <HivePresencePanel {...defaultProps} onPresenceUpdate={onPresenceUpdate} />
      )

      // Simulate presence update
      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: mockHivePresence.activeUsers.slice(0, 2),
          totalOnline: 2,
        },
      }

      rerender(<HivePresencePanel {...defaultProps} onPresenceUpdate={onPresenceUpdate} />)

      await waitFor(() => {
        expect(onPresenceUpdate).toHaveBeenCalledWith({
          hiveId: 'hive1',
          activeUsers: expect.any(Array),
          totalOnline: 2,
        })
      })
    })
  })

  describe('User Interactions Tests', () => {
    it('should call onUserClick when user is clicked', async () => {
      const user = userEvent.setup()
      const onUserClick = vi.fn()

      renderWithProviders(<HivePresencePanel {...defaultProps} onUserClick={onUserClick} />)

      const johnUser = screen.getByTestId('presence-user-john_doe')
      await user.click(johnUser)

      expect(onUserClick).toHaveBeenCalledWith(mockUsers[0])
    })

    it('should show user profile preview on hover', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const johnUser = screen.getByTestId('presence-user-john_doe')
      await user.hover(johnUser)

      await waitFor(() => {
        expect(screen.getByTestId('user-profile-preview')).toBeInTheDocument()
        expect(screen.getByText('john@example.com')).toBeInTheDocument()
        expect(screen.getByText('Joined: Jan 1, 2025')).toBeInTheDocument()
      })
    })

    it('should hide profile preview on mouse leave', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const johnUser = screen.getByTestId('presence-user-john_doe')
      await user.hover(johnUser)

      await waitFor(() => {
        expect(screen.getByTestId('user-profile-preview')).toBeInTheDocument()
      })

      await user.unhover(johnUser)

      await waitFor(() => {
        expect(screen.queryByTestId('user-profile-preview')).not.toBeInTheDocument()
      })
    })

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup()
      const onUserClick = vi.fn()

      renderWithProviders(<HivePresencePanel {...defaultProps} onUserClick={onUserClick} />)

      const firstUser = screen.getByTestId('presence-user-john_doe')
      firstUser.focus()

      await user.keyboard('{Enter}')
      expect(onUserClick).toHaveBeenCalledWith(mockUsers[0])

      await user.keyboard('{ArrowDown}')
      const secondUser = screen.getByTestId('presence-user-jane_smith')
      expect(secondUser).toHaveFocus()
    })
  })

  describe('Collapsible Functionality Tests', () => {
    it('should render collapse button when enabled', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse />)

      expect(screen.getByTestId('collapse-toggle')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /collapse presence panel/i })).toBeInTheDocument()
    })

    it('should not render collapse button when disabled', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse={false} />)

      expect(screen.queryByTestId('collapse-toggle')).not.toBeInTheDocument()
    })

    it('should collapse and expand panel when button is clicked', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse />)

      const collapseButton = screen.getByTestId('collapse-toggle')
      const userList = screen.getByTestId('presence-user-list')

      // Initially expanded
      expect(userList).toBeVisible()

      // Collapse
      await user.click(collapseButton)
      await waitFor(() => {
        expect(userList).not.toBeVisible()
        expect(screen.getByRole('button', { name: /expand presence panel/i })).toBeInTheDocument()
      })

      // Expand
      await user.click(collapseButton)
      await waitFor(() => {
        expect(userList).toBeVisible()
        expect(screen.getByRole('button', { name: /collapse presence panel/i })).toBeInTheDocument()
      })
    })

    it('should persist collapse state', async () => {
      const user = userEvent.setup()
      const { rerender } = renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse />)

      const collapseButton = screen.getByTestId('collapse-toggle')
      await user.click(collapseButton)

      // Component re-renders but maintains collapsed state
      rerender(<HivePresencePanel {...defaultProps} enableCollapse />)

      expect(screen.getByTestId('presence-user-list')).not.toBeVisible()
    })

    it('should show only count when collapsed', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse />)

      const collapseButton = screen.getByTestId('collapse-toggle')
      await user.click(collapseButton)

      await waitFor(() => {
        expect(screen.getByTestId('presence-panel-title')).toHaveTextContent('Online (3)')
        expect(screen.queryByTestId('presence-user-list')).not.toBeVisible()
      })
    })
  })

  describe('Accessibility Tests', () => {
    it('should have proper ARIA attributes', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const panel = screen.getByTestId('hive-presence-panel')
      expect(panel).toHaveAttribute('role', 'region')
      expect(panel).toHaveAttribute('aria-label', 'Hive presence panel')

      const userList = screen.getByTestId('presence-user-list')
      expect(userList).toHaveAttribute('role', 'list')
    })

    it('should have accessible user list items', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const userItems = screen.getAllByTestId(/^presence-user-/)
      userItems.forEach((item) => {
        expect(item).toHaveAttribute('role', 'listitem')
        expect(item).toHaveAttribute('tabindex', '0')
      })
    })

    it('should announce status changes to screen readers', async () => {
      const { rerender } = renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Simulate status change
      const updatedUser = createMockPresence(mockUsers[0], 'break', 'Taking a break')
      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: [
            updatedUser,
            ...mockHivePresence.activeUsers.slice(1),
          ],
        },
      }

      rerender(<HivePresencePanel {...defaultProps} />)

      await waitFor(() => {
        expect(screen.getByTestId('status-announcer')).toHaveTextContent(
          'John Doe status changed to break'
        )
      })
    })

    it('should have proper heading hierarchy', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const title = screen.getByTestId('presence-panel-title')
      expect(title).toHaveAttribute('role', 'heading')
      expect(title).toHaveAttribute('aria-level', '3')
    })

    it('should support keyboard navigation between users', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const firstUser = screen.getByTestId('presence-user-john_doe')
      firstUser.focus()

      await user.keyboard('{ArrowDown}')
      expect(screen.getByTestId('presence-user-jane_smith')).toHaveFocus()

      await user.keyboard('{ArrowDown}')
      expect(screen.getByTestId('presence-user-bob_wilson')).toHaveFocus()

      await user.keyboard('{ArrowUp}')
      expect(screen.getByTestId('presence-user-jane_smith')).toHaveFocus()
    })

    it('should have accessible collapse button', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} enableCollapse />)

      const collapseButton = screen.getByTestId('collapse-toggle')
      expect(collapseButton).toHaveAttribute('aria-expanded', 'true')
      expect(collapseButton).toHaveAttribute('aria-controls', 'presence-user-list')
    })

    it('should have proper color contrast for status indicators', () => {
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      const statusBadges = screen.getAllByTestId(/^status-/)
      statusBadges.forEach((badge) => {
        expect(badge).toHaveAttribute('data-high-contrast', 'true')
      })
    })
  })

  describe('Error Handling Tests', () => {
    it('should handle missing hive presence data gracefully', () => {
      mockPresenceContext.hivePresence = {}

      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      expect(screen.getByTestId('presence-empty-state')).toBeInTheDocument()
    })

    it('should handle network errors', async () => {
      const mockError = new Error('Network error')
      vi.spyOn(console, 'error').mockImplementation(() => {})

      renderWithProviders(<HivePresencePanel {...defaultProps} onError={vi.fn()} />)

      // Simulate WebSocket error
      const errorEvent = new Event('error')
      mockWebSocket.addEventListener.mock.calls.forEach(([event, handler]) => {
        if (event === 'error') {
          handler(errorEvent)
        }
      })

      await waitFor(() => {
        expect(screen.getByTestId('presence-error-state')).toBeInTheDocument()
        expect(screen.getByText('Failed to load presence data')).toBeInTheDocument()
      })
    })

    it('should retry connection on failure', async () => {
      const user = userEvent.setup()
      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Simulate error state
      const errorEvent = new Event('error')
      mockWebSocket.addEventListener.mock.calls.forEach(([event, handler]) => {
        if (event === 'error') {
          handler(errorEvent)
        }
      })

      await waitFor(() => {
        expect(screen.getByTestId('presence-error-state')).toBeInTheDocument()
      })

      const retryButton = screen.getByRole('button', { name: /retry/i })
      await user.click(retryButton)

      expect(mockPresenceContext.joinHivePresence).toHaveBeenCalledWith('hive1')
    })
  })

  describe('Performance Tests', () => {
    it('should virtualize large user lists', () => {
      const manyUsers = Array.from({ length: 100 }, (_, i) =>
        createMockPresence({
          id: `user${i}`,
          name: `User ${i}`,
          username: `user${i}`,
          firstName: 'User',
          lastName: `${i}`,
          email: `user${i}@example.com`,
          isEmailVerified: true,
          createdAt: '2025-01-01T10:00:00Z',
          updatedAt: '2025-01-01T10:00:00Z',
        })
      )

      mockPresenceContext.hivePresence = {
        hive1: {
          ...mockHivePresence,
          activeUsers: manyUsers,
          totalOnline: 100,
        },
      }

      renderWithProviders(<HivePresencePanel {...defaultProps} />)

      // Only visible items should be rendered
      const renderedUsers = screen.getAllByTestId(/^presence-user-/)
      expect(renderedUsers.length).toBeLessThan(100)
      expect(screen.getByTestId('virtual-list-container')).toBeInTheDocument()
    })

    it('should debounce real-time updates', async () => {
      const onPresenceUpdate = vi.fn()
      const { rerender } = renderWithProviders(
        <HivePresencePanel {...defaultProps} onPresenceUpdate={onPresenceUpdate} />
      )

      // Rapid updates
      for (let i = 0; i < 5; i++) {
        mockPresenceContext.hivePresence = {
          hive1: {
            ...mockHivePresence,
            totalOnline: i + 1,
          },
        }
        rerender(<HivePresencePanel {...defaultProps} onPresenceUpdate={onPresenceUpdate} />)
      }

      // Should debounce and only call once
      await waitFor(() => {
        expect(onPresenceUpdate).toHaveBeenCalledTimes(1)
      })
    })
  })
})